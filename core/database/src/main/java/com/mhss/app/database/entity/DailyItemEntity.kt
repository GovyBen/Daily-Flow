package com.mhss.app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "daily_items",
    indices = [
        Index(value = ["status", "due_at"]),
        Index(value = ["status", "start_at"]),
        Index(value = ["completed_at"]),
        Index(value = ["legacy_source_type", "legacy_source_id"], unique = true)
    ]
)
data class DailyItemEntity(
    @PrimaryKey
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("description")
    @ColumnInfo(defaultValue = "''")
    val description: String = "",
    @SerialName("kind")
    val kind: String,
    @SerialName("startAt")
    @ColumnInfo(name = "start_at")
    val startAt: Long?,
    @SerialName("endAt")
    @ColumnInfo(name = "end_at")
    val endAt: Long?,
    @SerialName("dueAt")
    @ColumnInfo(name = "due_at")
    val dueAt: Long?,
    @SerialName("allDay")
    @ColumnInfo(name = "all_day", defaultValue = "0")
    val allDay: Boolean = false,
    @SerialName("timeZoneId")
    @ColumnInfo(name = "time_zone_id")
    val timeZoneId: String,
    @SerialName("isCompletable")
    @ColumnInfo(name = "is_completable", defaultValue = "1")
    val isCompletable: Boolean = true,
    @SerialName("completedAt")
    @ColumnInfo(name = "completed_at")
    val completedAt: Long?,
    @SerialName("status")
    val status: String,
    @SerialName("priority")
    val priority: String,
    @SerialName("recurrenceFrequency")
    @ColumnInfo(name = "recurrence_frequency")
    val recurrenceFrequency: String?,
    @SerialName("recurrenceInterval")
    @ColumnInfo(name = "recurrence_interval")
    val recurrenceInterval: Int?,
    @SerialName("recurrenceWeekDays")
    @ColumnInfo(name = "recurrence_week_days", defaultValue = "'[]'")
    val recurrenceWeekDays: String = "[]",
    @SerialName("color")
    val color: Long?,
    @SerialName("subTasksJson")
    @ColumnInfo(name = "sub_tasks_json", defaultValue = "'[]'")
    val subTasksJson: String = "[]",
    @SerialName("legacySourceType")
    @ColumnInfo(name = "legacy_source_type")
    val legacySourceType: String?,
    @SerialName("legacySourceId")
    @ColumnInfo(name = "legacy_source_id")
    val legacySourceId: String?,
    @SerialName("createdAt")
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @SerialName("updatedAt")
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
