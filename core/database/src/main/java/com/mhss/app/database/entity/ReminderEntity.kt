package com.mhss.app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mhss.app.alarm.model.Reminder
import com.mhss.app.alarm.model.ReminderStatus
import com.mhss.app.alarm.model.ReminderTargetType

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["target_type", "target_id"]),
        Index(value = ["enabled", "status"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "target_type")
    val targetType: String,
    @ColumnInfo(name = "target_id")
    val targetId: String,
    @ColumnInfo(name = "absolute_trigger_at")
    val absoluteTriggerAt: Long?,
    @ColumnInfo(name = "relative_offset_minutes")
    val relativeOffsetMinutes: Int?,
    val enabled: Boolean,
    val status: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

fun ReminderEntity.toReminder() = Reminder(
    id = id,
    targetType = ReminderTargetType.valueOf(targetType),
    targetId = targetId,
    absoluteTriggerAt = absoluteTriggerAt,
    relativeOffsetMinutes = relativeOffsetMinutes,
    enabled = enabled,
    status = ReminderStatus.valueOf(status),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Reminder.toReminderEntity() = ReminderEntity(
    id = id,
    targetType = targetType.name,
    targetId = targetId,
    absoluteTriggerAt = absoluteTriggerAt,
    relativeOffsetMinutes = relativeOffsetMinutes,
    enabled = enabled,
    status = status.name,
    createdAt = createdAt,
    updatedAt = updatedAt
)
