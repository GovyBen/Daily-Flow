package com.mhss.app.tracking.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracking_trackers",
    indices = [
        Index(value = ["type", "is_active"])
    ]
)
data class TrackerEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,
    val unit: String? = null,
    @ColumnInfo(name = "config_json")
    val configJson: String,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "created_at_epoch_milli")
    val createdAtEpochMilli: Long,
    @ColumnInfo(name = "updated_at_epoch_milli")
    val updatedAtEpochMilli: Long
)
