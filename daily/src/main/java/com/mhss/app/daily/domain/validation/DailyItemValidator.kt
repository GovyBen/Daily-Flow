package com.mhss.app.daily.domain.validation

import com.mhss.app.daily.domain.model.DailyItem
import com.mhss.app.daily.domain.model.DailyItemStatus
import kotlinx.datetime.TimeZone

class DailyItemValidator {
    fun validate(item: DailyItem): List<DailyItemValidationError> {
        val errors = mutableListOf<DailyItemValidationError>()
        if (item.title.isBlank()) errors += DailyItemValidationError.BlankTitle

        val schedule = item.schedule
        if (schedule.timeZoneId.isBlank()) {
            errors += DailyItemValidationError.InvalidTimeZone
        } else {
            runCatching { TimeZone.of(schedule.timeZoneId) }
                .onFailure { errors += DailyItemValidationError.InvalidTimeZone }
        }

        if (schedule.endAtEpochMilli != null && schedule.startAtEpochMilli == null) {
            errors += DailyItemValidationError.EndWithoutStart
        }
        if (
            schedule.startAtEpochMilli != null &&
            schedule.endAtEpochMilli != null &&
            schedule.endAtEpochMilli < schedule.startAtEpochMilli
        ) {
            errors += DailyItemValidationError.EndBeforeStart
        }

        when (item.status) {
            DailyItemStatus.COMPLETED -> {
                if (item.completedAtEpochMilli == null) {
                    errors += DailyItemValidationError.CompletedWithoutTimestamp
                }
            }
            DailyItemStatus.ACTIVE -> {
                if (item.completedAtEpochMilli != null) {
                    errors += DailyItemValidationError.ActiveWithCompletedTimestamp
                }
            }
            DailyItemStatus.CANCELLED,
            DailyItemStatus.ARCHIVED -> Unit
        }

        if (item.updatedAtEpochMilli < item.createdAtEpochMilli) {
            errors += DailyItemValidationError.InvalidAuditTimestamps
        }

        if (item.recurrence?.interval != null && item.recurrence.interval < 1) {
            errors += DailyItemValidationError.InvalidRecurrenceInterval
        }

        return errors
    }

    fun requireValid(item: DailyItem) {
        val errors = validate(item)
        require(errors.isEmpty()) {
            errors.joinToString(prefix = "Invalid DailyItem: ")
        }
    }
}

enum class DailyItemValidationError {
    BlankTitle,
    InvalidTimeZone,
    EndWithoutStart,
    EndBeforeStart,
    CompletedWithoutTimestamp,
    ActiveWithCompletedTimestamp,
    InvalidAuditTimestamps,
    InvalidRecurrenceInterval
}
