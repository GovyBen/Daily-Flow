package com.mhss.app.mybrain.data.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mhss.app.preferences.PrefsConstants
import com.mhss.app.preferences.domain.model.AiProvider
import com.mhss.app.preferences.domain.repository.SecretStore
import kotlinx.coroutines.flow.first

class LegacySecretMigration(
    private val dataStore: DataStore<Preferences>,
    private val secretStore: SecretStore
) {
    suspend fun migrate() {
        val migrationKey = booleanPreferencesKey(PrefsConstants.SECRET_STORE_MIGRATION_KEY)
        if (dataStore.data.first()[migrationKey] == true) return

        AiProvider.selectable.mapNotNull { it.keyPref }.distinct().forEach { secretId ->
            val legacyKey = stringPreferencesKey(secretId)
            val legacyValue = dataStore.data.first()[legacyKey].orEmpty()
            if (legacyValue.isBlank()) return@forEach

            secretStore.set(secretId, legacyValue)
            check(secretStore.get(secretId) == legacyValue) {
                "Secret migration verification failed"
            }
            dataStore.edit { preferences ->
                preferences.remove(legacyKey)
            }
        }

        dataStore.edit { preferences ->
            preferences[migrationKey] = true
        }
    }
}
