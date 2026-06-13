package com.mhss.app.mybrain.data.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mhss.app.preferences.PrefsConstants
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreSecretStoreTest {
    private lateinit var dataStoreFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var scope: CoroutineScope
    private lateinit var secretStore: AndroidKeystoreSecretStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dataStoreFile = File(context.filesDir, "secret-store-test.preferences_pb")
        dataStoreFile.delete()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { dataStoreFile }
        secretStore = AndroidKeystoreSecretStore(dataStore, Dispatchers.IO)
    }

    @After
    fun tearDown() {
        scope.cancel()
        dataStoreFile.delete()
    }

    @Test
    fun encryptedPayloadDoesNotContainPlaintext() = runBlocking {
        val secret = "test-secret-value"

        secretStore.set(PrefsConstants.OPENAI_KEY, secret)

        assertEquals(secret, secretStore.get(PrefsConstants.OPENAI_KEY))
        val storedValues = dataStore.data.first().asMap().values.map(Any::toString)
        assertFalse(storedValues.any { it.contains(secret) })
        assertTrue(storedValues.any { it.startsWith("1:") })
    }

    @Test
    fun migrationMovesLegacyValueAndDeletesPlaintextPreference() = runBlocking {
        val legacyKey = stringPreferencesKey(PrefsConstants.OPENAI_KEY)
        val secret = "legacy-openai-secret"
        dataStore.edit { preferences ->
            preferences[legacyKey] = secret
        }

        LegacySecretMigration(dataStore, secretStore).migrate()
        LegacySecretMigration(dataStore, secretStore).migrate()

        val preferences = dataStore.data.first()
        assertEquals(secret, secretStore.get(PrefsConstants.OPENAI_KEY))
        assertNull(preferences[legacyKey])
        assertTrue(preferences.asMap().values.none { it.toString().contains(secret) })
    }
}
