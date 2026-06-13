package com.mhss.app.tracking.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracking_tracker_options",
    foreignKeys = [
        ForeignKey(
            entity = TrackerEntity::class,
            parentColumns = ["id"],
            childColumns = ["tracker_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["tracker_id"]),
        Index(value = ["tracker_id", "display_order", "id"])
    ]
)
data class TrackerOptionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "tracker_id")
    val trackerId: String,
    val label: String,
    @ColumnInfo(name = "numeric_value")
    val numericValue: Double? = null,
    val color: Long? = null,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int = 0,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)
