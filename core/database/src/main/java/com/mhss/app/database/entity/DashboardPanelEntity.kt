package com.mhss.app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "dashboard_panels",
    indices = [
        Index(value = ["enabled", "display_order"]),
        Index(value = ["type"])
    ]
)
data class DashboardPanelEntity(
    @PrimaryKey
    @SerialName("id")
    val id: String,
    @SerialName("type")
    val type: String,
    @SerialName("enabled")
    val enabled: Boolean,
    @SerialName("displayOrder")
    @ColumnInfo(name = "display_order")
    val displayOrder: Int,
    @SerialName("size")
    val size: String,
    @SerialName("configJson")
    @ColumnInfo(name = "config_json")
    val configJson: String,
    @SerialName("createdAt")
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @SerialName("updatedAt")
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
