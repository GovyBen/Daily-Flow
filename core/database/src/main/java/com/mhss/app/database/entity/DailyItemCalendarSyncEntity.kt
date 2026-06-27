package com.mhss.app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "daily_item_calendar_sync",
    foreignKeys = [
        ForeignKey(
            entity = DailyItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["enabled", "state"]),
        Index(value = ["system_event_id"], unique = true)
    ]
)
data class DailyItemCalendarSyncEntity(
    @PrimaryKey
    @SerialName("itemId")
    @ColumnInfo(name = "item_id")
    val itemId: String,
    @SerialName("enabled")
    @ColumnInfo(defaultValue = "0")
    val enabled: Boolean = false,
    @SerialName("systemCalendarId")
    @ColumnInfo(name = "system_calendar_id")
    val systemCalendarId: Long?,
    @SerialName("systemEventId")
    @ColumnInfo(name = "system_event_id")
    val systemEventId: Long?,
    @SerialName("state")
    val state: String,
    @SerialName("lastSyncedAt")
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long?,
    @SerialName("lastLocalFingerprint")
    @ColumnInfo(name = "last_local_fingerprint")
    val lastLocalFingerprint: String?,
    @SerialName("lastProviderFingerprint")
    @ColumnInfo(name = "last_provider_fingerprint")
    val lastProviderFingerprint: String?,
    @SerialName("lastError")
    @ColumnInfo(name = "last_error")
    val lastError: String?,
    @SerialName("updatedAt")
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
