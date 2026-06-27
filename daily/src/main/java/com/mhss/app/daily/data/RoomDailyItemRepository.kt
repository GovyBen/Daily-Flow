package com.mhss.app.daily.data

import androidx.room.withTransaction
import com.mhss.app.daily.domain.model.CalendarSyncState
import com.mhss.app.daily.domain.model.DailyItem
import com.mhss.app.daily.domain.model.DailyItemCalendarSync
import com.mhss.app.daily.domain.model.DailyItemFilter
import com.mhss.app.daily.domain.model.DailyItemPriority
import com.mhss.app.daily.domain.model.DailyItemRange
import com.mhss.app.daily.domain.model.DailyItemRangeResolver
import com.mhss.app.daily.domain.model.DailyItemStatus
import com.mhss.app.daily.domain.model.DailyItemStatusFilter
import com.mhss.app.daily.domain.repository.DailyItemRepository
import com.mhss.app.database.MyBrainDatabase
import com.mhss.app.database.dao.DailyItemDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone

class RoomDailyItemRepository(
    private val database: MyBrainDatabase,
    private val dailyItemDao: DailyItemDao,
    private val ioDispatcher: CoroutineDispatcher,
    private val defaultDispatcher: CoroutineDispatcher
) : DailyItemRepository {

    override fun observeItems(filter: DailyItemFilter): Flow<List<DailyItem>> {
        return observeAllItems().map { items ->
            items.filterAndSort(filter, System.currentTimeMillis())
        }.flowOn(defaultDispatcher)
    }

    override fun observeItem(id: String): Flow<DailyItem?> {
        return combine(
            dailyItemDao.observeItem(id),
            dailyItemDao.observeAllCalendarSync()
        ) { item, syncRows ->
            item?.toDailyItem(syncRows.firstOrNull { it.itemId == item.id })
        }.flowOn(defaultDispatcher)
    }

    override suspend fun getItem(id: String): DailyItem? = withContext(ioDispatcher) {
        val item = dailyItemDao.getItem(id) ?: return@withContext null
        item.toDailyItem(dailyItemDao.getCalendarSync(id))
    }

    override suspend fun getAllItems(): List<DailyItem> = withContext(ioDispatcher) {
        merge(dailyItemDao.getAll(), dailyItemDao.getAllCalendarSync())
    }

    override suspend fun searchItems(query: String): List<DailyItem> {
        val normalized = query.trim()
        if (normalized.isBlank()) return emptyList()
        return getAllItems()
            .filter {
                it.title.contains(normalized, ignoreCase = true) ||
                    it.description.contains(normalized, ignoreCase = true)
            }
            .sortedByDescending { it.updatedAtEpochMilli }
    }

    override suspend fun upsert(item: DailyItem): DailyItem = withContext(ioDispatcher) {
        database.withTransaction {
            dailyItemDao.upsert(item.toDailyItemEntity())
            dailyItemDao.upsertCalendarSync(item.toDailyItemCalendarSyncEntity())
        }
        item
    }

    override suspend fun upsertAll(items: List<DailyItem>) {
        withContext(ioDispatcher) {
            database.withTransaction {
                dailyItemDao.upsertAll(items.map { it.toDailyItemEntity() })
                dailyItemDao.upsertCalendarSync(items.map { it.toDailyItemCalendarSyncEntity() })
            }
        }
    }

    override suspend fun delete(id: String) {
        withContext(ioDispatcher) {
            dailyItemDao.delete(id)
        }
    }

    override suspend fun markCompleted(id: String, completedAt: Long): DailyItem? {
        val item = getItem(id) ?: return null
        val updated = item.copy(
            status = DailyItemStatus.COMPLETED,
            completedAtEpochMilli = completedAt,
            updatedAtEpochMilli = maxOf(item.createdAtEpochMilli, completedAt)
        )
        return upsert(updated)
    }

    override suspend fun reopen(id: String, updatedAt: Long): DailyItem? {
        val item = getItem(id) ?: return null
        val updated = item.copy(
            status = DailyItemStatus.ACTIVE,
            completedAtEpochMilli = null,
            updatedAtEpochMilli = maxOf(item.createdAtEpochMilli, updatedAt)
        )
        return upsert(updated)
    }

    override suspend fun archive(id: String, updatedAt: Long): DailyItem? {
        val item = getItem(id) ?: return null
        val updated = item.copy(
            status = DailyItemStatus.ARCHIVED,
            updatedAtEpochMilli = maxOf(item.createdAtEpochMilli, updatedAt)
        )
        return upsert(updated)
    }

    override suspend fun updateCalendarSync(
        id: String,
        sync: DailyItemCalendarSync
    ): DailyItem? {
        val item = getItem(id) ?: return null
        val updated = item.copy(calendarSync = sync)
        return upsert(updated)
    }

    override suspend fun markSyncState(
        id: String,
        state: CalendarSyncState,
        lastError: String?,
        updatedAt: Long
    ): DailyItem? {
        val item = getItem(id) ?: return null
        return updateCalendarSync(
            id = id,
            sync = item.calendarSync.copy(
                state = state,
                lastError = lastError,
                lastSyncedAtEpochMilli = if (state == CalendarSyncState.SYNCED) updatedAt else item.calendarSync.lastSyncedAtEpochMilli
            )
        )
    }

    private fun observeAllItems(): Flow<List<DailyItem>> {
        return combine(
            dailyItemDao.observeAll(),
            dailyItemDao.observeAllCalendarSync()
        ) { items, syncRows -> merge(items, syncRows) }
    }

    private fun merge(
        items: List<com.mhss.app.database.entity.DailyItemEntity>,
        syncRows: List<com.mhss.app.database.entity.DailyItemCalendarSyncEntity>
    ): List<DailyItem> {
        val syncByItemId = syncRows.associateBy { it.itemId }
        return items.map { item -> item.toDailyItem(syncByItemId[item.id]) }
    }
}

suspend fun DailyItemRepository.observeItemsOnce(
    filter: DailyItemFilter
): List<DailyItem> = observeItems(filter).first()

private fun List<DailyItem>.filterAndSort(
    filter: DailyItemFilter,
    nowEpochMilli: Long
): List<DailyItem> {
    val rangeBounds = DailyItemRangeResolver.bounds(
        range = filter.range,
        nowEpochMilli = nowEpochMilli,
        timeZone = TimeZone.currentSystemDefault()
    )
    val normalizedQuery = filter.query.trim()
    return asSequence()
        .filter { item -> item.status != DailyItemStatus.ARCHIVED }
        .filter { item ->
            when (filter.status) {
                DailyItemStatusFilter.Active -> item.status == DailyItemStatus.ACTIVE
                DailyItemStatusFilter.Completed -> item.status == DailyItemStatus.COMPLETED
                DailyItemStatusFilter.ActiveAndCompleted ->
                    item.status == DailyItemStatus.ACTIVE || item.status == DailyItemStatus.COMPLETED
                DailyItemStatusFilter.Any -> true
            }
        }
        .filter { item -> filter.includeCompleted || item.status != DailyItemStatus.COMPLETED }
        .filter { item -> filter.syncState == null || item.calendarSync.state == filter.syncState }
        .filter { item -> filter.priority == null || item.priority == filter.priority }
        .filter { item ->
            normalizedQuery.isBlank() ||
                item.title.contains(normalizedQuery, ignoreCase = true) ||
                item.description.contains(normalizedQuery, ignoreCase = true)
        }
        .filter { item ->
            item.matchesRange(filter.range, rangeBounds, nowEpochMilli, filter.includeNoDate)
        }
        .sortedWith(itemComparator(filter.range, nowEpochMilli))
        .toList()
}

private fun DailyItem.matchesRange(
    range: DailyItemRange,
    bounds: com.mhss.app.daily.domain.model.DailyItemRangeBounds,
    nowEpochMilli: Long,
    includeNoDate: Boolean
): Boolean {
    val start = schedule.startAtEpochMilli
    val due = schedule.dueAtEpochMilli
    val hasNoDate = start == null && due == null
    return when (range) {
        DailyItemRange.All -> true
        DailyItemRange.NoDate -> hasNoDate
        DailyItemRange.Completed -> status == DailyItemStatus.COMPLETED
        DailyItemRange.Overdue -> isOverdue(nowEpochMilli)
        else -> {
            (start != null && bounds.contains(start)) ||
                (due != null && bounds.contains(due)) ||
                (includeNoDate && hasNoDate) ||
                (bounds.includeOverdueCarry && isOverdue(nowEpochMilli))
        }
    }
}

private fun DailyItem.isOverdue(nowEpochMilli: Long): Boolean {
    if (status != DailyItemStatus.ACTIVE || !isCompletable) return false
    val time = schedule.dueAtEpochMilli ?: schedule.startAtEpochMilli ?: return false
    return time < nowEpochMilli
}

private fun itemComparator(
    range: DailyItemRange,
    nowEpochMilli: Long
): Comparator<DailyItem> {
    return when (range) {
        DailyItemRange.Completed -> compareByDescending<DailyItem> {
            it.completedAtEpochMilli ?: Long.MIN_VALUE
        }
        DailyItemRange.NoDate -> compareByDescending<DailyItem> { it.priority.sortWeight }
            .thenByDescending { it.updatedAtEpochMilli }
            .thenBy { it.title.lowercase() }
        else -> compareByDescending<DailyItem> { it.isOverdue(nowEpochMilli) }
            .thenBy { it.schedule.startAtEpochMilli ?: Long.MAX_VALUE }
            .thenBy { it.schedule.dueAtEpochMilli ?: Long.MAX_VALUE }
            .thenByDescending { it.priority.sortWeight }
            .thenByDescending { it.updatedAtEpochMilli }
            .thenBy { it.title.lowercase() }
    }
}

private val DailyItemPriority.sortWeight: Int
    get() = when (this) {
        DailyItemPriority.LOW -> 0
        DailyItemPriority.MEDIUM -> 1
        DailyItemPriority.HIGH -> 2
    }
