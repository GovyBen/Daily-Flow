package com.mhss.app.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM encryption + PBKDF2 key derivation (DF-601, DF-602).
 * Thread-safe, uses Android-available JCE providers only.
 */
object EncryptionUtils {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    private const val SALT_LENGTH = 16
    private const val PBKDF2_ITERATIONS = 600_000
    private const val KEY_LENGTH = 256

    // ---- Key Derivation (DF-602) ----

    fun deriveKey(password: String, salt: ByteArray? = null): Pair<SecretKey, ByteArray> {
        val actualSalt = salt ?: ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val spec = PBEKeySpec(password.toCharArray(), actualSalt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return Pair(SecretKeySpec(keyBytes, KEY_ALGORITHM), actualSalt)
    }

    // ---- AES-256-GCM Encrypt (DF-601) ----

    fun encrypt(plaintext: ByteArray, key: SecretKey): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val encrypted = cipher.doFinal(plaintext)
        return iv + encrypted
    }

    fun encryptString(plaintext: String, password: String): ByteArray {
        val (key, salt) = deriveKey(password)
        val encrypted = encrypt(plaintext.toByteArray(Charsets.UTF_8), key)
        return salt + encrypted
    }

    // ---- AES-256-GCM Decrypt (DF-601) ----

    fun decrypt(combined: ByteArray, key: SecretKey): ByteArray {
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(ciphertext)
    }

    fun decryptString(combined: ByteArray, password: String): String {
        val salt = combined.copyOfRange(0, SALT_LENGTH)
        val ciphertext = combined.copyOfRange(SALT_LENGTH, combined.size)
        val (key, _) = deriveKey(password, salt)
        return String(decrypt(ciphertext, key), Charsets.UTF_8)
    }
}
