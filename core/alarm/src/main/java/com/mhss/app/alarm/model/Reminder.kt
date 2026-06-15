package com.mhss.app.alarm.model

data class Reminder(
    val id: Long = 0,
    val targetType: ReminderTargetType,
    val targetId: String,
    val absoluteTriggerAt: Long? = null,
    val relativeOffsetMinutes: Int? = null,
    val enabled: Boolean = true,
    val status: ReminderStatus = ReminderStatus.PENDING,
    val createdAt: Long,
    val updatedAt: Long
) {
    init {
        require(targetId.isNotBlank()) { "Reminder target ID must not be blank" }
        require((absoluteTriggerAt != null) xor (relativeOffsetMinutes != null)) {
            "Reminder must have exactly one trigger source"
        }
        require(absoluteTriggerAt == null || absoluteTriggerAt >= 0) {
            "Absolute trigger time must not be negative"
        }
        require(relativeOffsetMinutes == null || relativeOffsetMinutes >= 0) {
            "Relative offset must not be negative"
        }
        require(createdAt >= 0 && updatedAt >= createdAt) {
            "Reminder timestamps are invalid"
        }
    }

    fun requestCode(): Int {
        require(id in 1..Int.MAX_VALUE.toLong()) {
            "A persisted reminder ID is required for scheduling"
        }
        return id.toInt()
    }

    fun resolveTriggerAt(targetEpochMilli: Long?): Long? {
        return absoluteTriggerAt ?: targetEpochMilli?.minus(
            relativeOffsetMinutes!!.toLong() * MILLIS_PER_MINUTE
        )
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}

enum class ReminderTargetType {
    TASK,
    CALENDAR_EVENT,
    RECORD_PROMPT
}

enum class ReminderStatus {
    PENDING,
    SCHEDULED,
    DELIVERED,
    CANCELLED,
    MISSED
}
