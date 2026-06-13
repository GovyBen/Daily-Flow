package com.mhss.app.tracking.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracking_templates",
    indices = [
        Index(value = ["display_order", "created_at_epoch_milli", "id"])
    ]
)
data class RecordTemplateEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val icon: String = "",
    val color: Long,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int = 0,
    @ColumnInfo(name = "created_at_epoch_milli")
    val createdAtEpochMilli: Long,
    @ColumnInfo(name = "updated_at_epoch_milli")
    val updatedAtEpochMilli: Long
)
