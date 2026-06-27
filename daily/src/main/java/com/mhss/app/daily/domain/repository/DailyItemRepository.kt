package com.mhss.app.daily.domain.repository

import com.mhss.app.daily.domain.model.CalendarSyncState
import com.mhss.app.daily.domain.model.DailyItem
import com.mhss.app.daily.domain.model.DailyItemCalendarSync
import com.mhss.app.daily.domain.model.DailyItemFilter
import kotlinx.coroutines.flow.Flow

interface DailyItemRepository {
    fun observeItems(filter: DailyItemFilter = DailyItemFilter()): Flow<List<DailyItem>>
    fun observeItem(id: String): Flow<DailyItem?>
    suspend fun getItem(id: String): DailyItem?
    suspend fun getAllItems(): List<DailyItem>
    suspend fun searchItems(query: String): List<DailyItem>
    suspend fun upsert(item: DailyItem): DailyItem
    suspend fun upsertAll(items: List<DailyItem>)
    suspend fun delete(id: String)
    suspend fun markCompleted(id: String, completedAt: Long): DailyItem?
    suspend fun reopen(id: String, updatedAt: Long): DailyItem?
    suspend fun archive(id: String, updatedAt: Long): DailyItem?
    suspend fun updateCalendarSync(id: String, sync: DailyItemCalendarSync): DailyItem?
    suspend fun markSyncState(
        id: String,
        state: CalendarSyncState,
        lastError: String? = null,
        updatedAt: Long = System.currentTimeMillis()
    ): DailyItem?
}
