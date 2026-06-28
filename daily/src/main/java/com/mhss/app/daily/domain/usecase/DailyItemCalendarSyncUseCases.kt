package com.mhss.app.daily.domain.usecase

import com.mhss.app.daily.domain.model.CalendarSyncState
import com.mhss.app.daily.domain.model.DailyItem
import com.mhss.app.daily.domain.repository.DailyItemRepository
import com.mhss.app.domain.model.CalendarEvent
import com.mhss.app.domain.model.CalendarEventFrequency
import com.mhss.app.domain.repository.CalendarRepository

class SyncDailyItemToCalendarUseCase(
    private val repository: DailyItemRepository,
    private val calendarRepository: CalendarRepository
) {
    suspend operator fun invoke(id: String): DailyItem? {
        val item = repository.getItem(id) ?: return null
        if (!item.calendarSync.enabled) return item

        val now = System.currentTimeMillis()
        if (!item.schedule.hasDate) {
            return repository.updateCalendarSync(
                id,
                item.calendarSync.copy(
                    state = CalendarSyncState.FAILED,
                    lastError = "Calendar sync requires a start time or due time"
                )
            )
        }
        return try {
            val calendarId = item.calendarSync.systemCalendarId ?: resolveCalendarId()
            val localFingerprint = item.localFingerprint()
            val existingEvent = item.calendarSync.systemEventId
                ?.let { calendarRepository.getEventById(it) }
            val lastProviderFingerprint = item.calendarSync.lastProviderFingerprint

            if (
                existingEvent != null &&
                lastProviderFingerprint != null &&
                existingEvent.providerFingerprint() != lastProviderFingerprint &&
                item.calendarSync.state == CalendarSyncState.SYNCED
            ) {
                return repository.updateCalendarSync(
                    id,
                    item.calendarSync.copy(
                        state = CalendarSyncState.EXTERNAL_DRIFT,
                        lastError = null
                    )
                )
            }

            val providerEvent = item.toCalendarEvent(
                eventId = existingEvent?.id ?: item.calendarSync.systemEventId ?: 0L,
                calendarId = calendarId
            )
            val providerEventId = if (existingEvent == null) {
                calendarRepository.addEvent(providerEvent) ?: error("Calendar insert failed")
            } else {
                calendarRepository.updateEvent(providerEvent)
                existingEvent.id
            }
            val syncedProvider = calendarRepository.getEventById(providerEventId) ?: providerEvent.copy(
                id = providerEventId
            )

            repository.updateCalendarSync(
                id,
                item.calendarSync.copy(
                    enabled = true,
                    systemCalendarId = calendarId,
                    systemEventId = providerEventId,
                    state = CalendarSyncState.SYNCED,
                    lastSyncedAtEpochMilli = now,
                    lastLocalFingerprint = localFingerprint,
                    lastProviderFingerprint = syncedProvider.providerFingerprint(),
                    lastError = null
                )
            )
        } catch (error: Exception) {
            repository.updateCalendarSync(
                id,
                item.calendarSync.copy(
                    state = CalendarSyncState.FAILED,
                    lastError = error.toCalendarSyncErrorMessage()
                )
            )
        }
    }

    private suspend fun resolveCalendarId(): Long {
        val calendars = calendarRepository.getCalendars()
        if (calendars.isNotEmpty()) return calendars.first().id
        calendarRepository.createCalendar()
        return calendarRepository.getCalendars().firstOrNull()?.id
            ?: error("No writable calendar is available")
    }
}

private fun Throwable.toCalendarSyncErrorMessage(): String {
    val rawMessage = message
    if (rawMessage?.contains("Permission Denial", ignoreCase = true) == true) {
        return CALENDAR_PERMISSION_REQUIRED_MESSAGE
    }
    return when (this) {
        is SecurityException -> CALENDAR_PERMISSION_REQUIRED_MESSAGE
        else -> rawMessage
            ?: this::class.simpleName
            ?: "Calendar sync failed"
    }
}

class DisableDailyItemCalendarSyncUseCase(
    private val repository: DailyItemRepository
) {
    suspend operator fun invoke(id: String): DailyItem? {
        val item = repository.getItem(id) ?: return null
        return repository.updateCalendarSync(
            id,
            item.calendarSync.copy(
                enabled = false,
                state = if (item.calendarSync.systemEventId != null) {
                    CalendarSyncState.UNLINKED
                } else {
                    CalendarSyncState.NOT_SYNCED
                },
                lastError = null
            )
        )
    }
}

class ReconcileDailyItemCalendarSyncUseCase(
    private val repository: DailyItemRepository,
    private val calendarRepository: CalendarRepository
) {
    suspend operator fun invoke(): List<DailyItem> {
        return repository.getAllItems()
            .filter { it.calendarSync.enabled && it.calendarSync.systemEventId != null }
            .mapNotNull { item ->
                val event = item.calendarSync.systemEventId?.let { calendarRepository.getEventById(it) }
                val state = when {
                    event == null -> CalendarSyncState.UNLINKED
                    item.calendarSync.lastProviderFingerprint != null &&
                        event.providerFingerprint() != item.calendarSync.lastProviderFingerprint ->
                        CalendarSyncState.EXTERNAL_DRIFT
                    item.localFingerprint() != item.calendarSync.lastLocalFingerprint ->
                        CalendarSyncState.DIRTY
                    else -> CalendarSyncState.SYNCED
                }
                repository.markSyncState(item.id, state)
            }
    }
}

private fun DailyItem.toCalendarEvent(
    eventId: Long,
    calendarId: Long
): CalendarEvent {
    val start = schedule.startAtEpochMilli ?: schedule.dueAtEpochMilli
        ?: error("Calendar sync requires a start time or due time")
    val end = schedule.endAtEpochMilli ?: (start + DEFAULT_EVENT_DURATION_MILLIS)
    return CalendarEvent(
        id = eventId,
        title = title,
        description = buildProviderDescription(),
        start = start,
        end = maxOf(start, end),
        allDay = schedule.allDay,
        color = color?.toInt() ?: 0,
        calendarId = calendarId,
        recurring = recurrence != null,
        frequency = when (recurrence?.frequency) {
            com.mhss.app.daily.domain.model.DailyItemFrequency.DAILY -> CalendarEventFrequency.DAILY
            com.mhss.app.daily.domain.model.DailyItemFrequency.WEEKLY -> CalendarEventFrequency.WEEKLY
            com.mhss.app.daily.domain.model.DailyItemFrequency.MONTHLY -> CalendarEventFrequency.MONTHLY
            com.mhss.app.daily.domain.model.DailyItemFrequency.YEARLY -> CalendarEventFrequency.YEARLY
            else -> CalendarEventFrequency.NEVER
        },
        interval = recurrence?.interval?.coerceAtLeast(1) ?: 1,
        weekDays = recurrence?.weekDays.orEmpty()
    )
}

private fun DailyItem.buildProviderDescription(): String {
    val completion = if (status == com.mhss.app.daily.domain.model.DailyItemStatus.COMPLETED) {
        "\n\nDaily Flow status: completed"
    } else {
        "\n\nDaily Flow status: active"
    }
    return description + completion
}

private fun DailyItem.localFingerprint(): String = listOf(
    title,
    description,
    kind.name,
    schedule.startAtEpochMilli,
    schedule.endAtEpochMilli,
    schedule.dueAtEpochMilli,
    schedule.allDay,
    schedule.timeZoneId,
    status.name,
    completedAtEpochMilli,
    priority.name,
    recurrence?.frequency?.name,
    recurrence?.interval,
    recurrence?.weekDays?.joinToString(",") { it.name }
).joinToString("|")

private fun CalendarEvent.providerFingerprint(): String = listOf(
    title,
    description,
    start,
    end,
    allDay,
    color,
    calendarId,
    frequency.name,
    interval,
    weekDays.joinToString(",") { it.name }
).joinToString("|")

private const val DEFAULT_EVENT_DURATION_MILLIS = 60 * 60 * 1000L
private const val CALENDAR_PERMISSION_REQUIRED_MESSAGE =
    "Calendar permission is required. Grant calendar access and try again."
