package com.mhss.app.mybrain.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mhss.app.preferences.domain.repository.SecretStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AndroidKeystoreSecretStore(
    private val dataStore: DataStore<Preferences>,
    private val ioDispatcher: CoroutineDispatcher
) : SecretStore {
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    private val keyLock = Any()

    override fun observe(secretId: String): Flow<String> =
        dataStore.data.map { preferences ->
            val payload = preferences[payloadKey(secretId)] ?: return@map ""
            try {
                decrypt(payload)
            } catch (_: Exception) {
                remove(secretId)
                resetEncryptionKey()
                ""
            }
        }

    override suspend fun get(secretId: String): String = withContext(ioDispatcher) {
        val payload = dataStore.data.first()[payloadKey(secretId)] ?: return@withContext ""
        try {
            decrypt(payload)
        } catch (_: Exception) {
            remove(secretId)
            resetEncryptionKey()
            ""
        }
    }

    override suspend fun set(secretId: String, value: String) {
        if (value.isBlank()) {
            remove(secretId)
            return
        }
        withContext(ioDispatcher) {
            val payload = encrypt(value)
            dataStore.edit { preferences ->
                preferences[payloadKey(secretId)] = payload
            }
        }
    }

    override suspend fun remove(secretId: String) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences.remove(payloadKey(secretId))
            }
        }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return listOf(
            PAYLOAD_VERSION,
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        ).joinToString(PAYLOAD_SEPARATOR)
    }

    private fun decrypt(payload: String): String {
        val parts = payload.split(PAYLOAD_SEPARATOR, limit = 3)
        require(parts.size == 3 && parts[0] == PAYLOAD_VERSION) {
            "Unsupported encrypted secret payload"
        }
        val iv = Base64.decode(parts[1], Base64.NO_WRAP)
        val ciphertext = Base64.decode(parts[2], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey = synchronized(keyLock) {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setRandomizedEncryptionRequired(true)
                        .build()
                )
            }
            .generateKey()
    }

    private fun resetEncryptionKey() = synchronized(keyLock) {
        runCatching { keyStore.deleteEntry(KEY_ALIAS) }
    }

    private fun payloadKey(secretId: String) =
        stringPreferencesKey("$PAYLOAD_PREFIX$secretId")

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "daily_flow_secret_store_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH = 128
        const val PAYLOAD_VERSION = "1"
        const val PAYLOAD_SEPARATOR = ":"
        const val PAYLOAD_PREFIX = "encrypted_secret_"
    }
}
