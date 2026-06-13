package com.mhss.app.preferences.data.repository

import com.mhss.app.preferences.domain.repository.SecretStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class InMemorySecretStore(
    initialValues: Map<String, String> = emptyMap()
) : SecretStore {
    private val values = MutableStateFlow(initialValues)

    override fun observe(secretId: String): Flow<String> =
        values.map { secrets -> secrets[secretId].orEmpty() }

    override suspend fun get(secretId: String): String = values.value[secretId].orEmpty()

    override suspend fun set(secretId: String, value: String) {
        values.value = values.value.toMutableMap().apply {
            if (value.isBlank()) remove(secretId) else put(secretId, value)
        }
    }

    override suspend fun remove(secretId: String) {
        values.value = values.value - secretId
    }
}
