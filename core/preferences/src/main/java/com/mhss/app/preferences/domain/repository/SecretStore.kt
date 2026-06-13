package com.mhss.app.preferences.domain.repository

import kotlinx.coroutines.flow.Flow

interface SecretStore {
    fun observe(secretId: String): Flow<String>

    suspend fun get(secretId: String): String

    suspend fun set(secretId: String, value: String)

    suspend fun remove(secretId: String)
}
