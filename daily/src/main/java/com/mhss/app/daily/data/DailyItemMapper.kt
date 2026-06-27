package com.mhss.app.daily.data

import com.mhss.app.daily.domain.model.CalendarSyncState
import com.mhss.app.daily.domain.model.DailyItem
import com.mhss.app.daily.domain.model.DailyItemCalendarSync
import com.mhss.app.daily.domain.model.DailyItemFrequency
import com.mhss.app.daily.domain.model.DailyItemKind
import com.mhss.app.daily.domain.model.DailyItemLegacySource
import com.mhss.app.daily.domain.model.DailyItemLegacySourceType
import com.mhss.app.daily.domain.model.DailyItemPriority
import com.mhss.app.daily.domain.model.DailyItemRecurrence
import com.mhss.app.daily.domain.model.DailyItemSchedule
import com.mhss.app.daily.domain.model.DailyItemStatus
import com.mhss.app.database.entity.DailyItemCalendarSyncEntity
import com.mhss.app.database.entity.DailyItemEntity
import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun DailyItemEntity.toDailyItem(
    sync: DailyItemCalendarSyncEntity?
): DailyItem {
    return DailyItem(
        id = id,
        title = title,
        description = description,
        kind = enumValueOrDefault(kind, DailyItemKind.TASK),
        schedule = DailyItemSchedule(
            startAtEpochMilli = startAt,
            endAtEpochMilli = endAt,
            dueAtEpochMilli = dueAt,
            allDay = allDay,
            timeZoneId = timeZoneId
        ),
        isCompletable = isCompletable,
        completedAtEpochMilli = completedAt,
        status = enumValueOrDefault(status, DailyItemStatus.ACTIVE),
        priority = enumValueOrDefault(priority, DailyItemPriority.LOW),
        recurrence = recurrenceFrequency?.let { frequencyName ->
            DailyItemRecurrence(
                frequency = enumValueOrDefault(frequencyName, DailyItemFrequency.DAILY),
                interval = recurrenceInterval?.coerceAtLeast(1) ?: 1,
                weekDays = decodeWeekDays(recurrenceWeekDays)
            )
        },
        color = color,
        calendarSync = sync?.toDailyItemCalendarSync() ?: DailyItemCalendarSync(),
        legacySource = legacySourceType?.let { type ->
            legacySourceId?.let { id ->
                DailyItemLegacySource(
                    type = enumValueOrDefault(type, DailyItemLegacySourceType.MANUAL),
                    id = id
                )
            }
        },
        subTasksJson = subTasksJson,
        createdAtEpochMilli = createdAt,
        updatedAtEpochMilli = updatedAt
    )
}

fun DailyItem.toDailyItemEntity(): DailyItemEntity {
    return DailyItemEntity(
        id = id,
        title = title,
        description = description,
        kind = kind.name,
        startAt = schedule.startAtEpochMilli,
        endAt = schedule.endAtEpochMilli,
        dueAt = schedule.dueAtEpochMilli,
        allDay = schedule.allDay,
        timeZoneId = schedule.timeZoneId,
        isCompletable = isCompletable,
        completedAt = completedAtEpochMilli,
        status = status.name,
        priority = priority.name,
        recurrenceFrequency = recurrence?.frequency?.name,
        recurrenceInterval = recurrence?.interval,
        recurrenceWeekDays = encodeWeekDays(recurrence?.weekDays.orEmpty()),
        color = color,
        subTasksJson = subTasksJson.ifBlank { "[]" },
        legacySourceType = legacySource?.type?.name,
        legacySourceId = legacySource?.id,
        createdAt = createdAtEpochMilli,
        updatedAt = updatedAtEpochMilli
    )
}

fun DailyItemCalendarSyncEntity.toDailyItemCalendarSync() = DailyItemCalendarSync(
    enabled = enabled,
    systemCalendarId = systemCalendarId,
    systemEventId = systemEventId,
    state = enumValueOrDefault(state, CalendarSyncState.NOT_SYNCED),
    lastSyncedAtEpochMilli = lastSyncedAt,
    lastLocalFingerprint = lastLocalFingerprint,
    lastProviderFingerprint = lastProviderFingerprint,
    lastError = lastError
)

fun DailyItem.toDailyItemCalendarSyncEntity() = calendarSync.toEntity(
    itemId = id,
    updatedAt = updatedAtEpochMilli
)

fun DailyItemCalendarSync.toEntity(
    itemId: String,
    updatedAt: Long
) = DailyItemCalendarSyncEntity(
    itemId = itemId,
    enabled = enabled,
    systemCalendarId = systemCalendarId,
    systemEventId = systemEventId,
    state = state.name,
    lastSyncedAt = lastSyncedAtEpochMilli,
    lastLocalFingerprint = lastLocalFingerprint,
    lastProviderFingerprint = lastProviderFingerprint,
    lastError = lastError,
    updatedAt = updatedAt
)

private fun decodeWeekDays(value: String): Set<DayOfWeek> {
    return runCatching {
        json.decodeFromString<List<String>>(value)
            .mapNotNull { name -> DayOfWeek.entries.firstOrNull { it.name == name } }
            .toSet()
    }.getOrDefault(emptySet())
}

private fun encodeWeekDays(value: Set<DayOfWeek>): String {
    return json.encodeToString(value.map { it.name }.sorted())
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(
    name: String,
    default: T
): T = enumValues<T>().firstOrNull { it.name == name } ?: default
