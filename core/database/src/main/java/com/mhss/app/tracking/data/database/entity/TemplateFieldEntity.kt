package com.mhss.app.tracking.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracking_template_fields",
    foreignKeys = [
        ForeignKey(
            entity = RecordTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = TrackerEntity::class,
            parentColumns = ["id"],
            childColumns = ["tracker_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["template_id"]),
        Index(value = ["tracker_id"]),
        Index(value = ["template_id", "display_order", "id"])
    ]
)
data class TemplateFieldEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "template_id")
    val templateId: String,
    @ColumnInfo(name = "tracker_id")
    val trackerId: String,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int = 0,
    val required: Boolean = false,
    @ColumnInfo(name = "display_name_override")
    val displayNameOverride: String? = null,
    @ColumnInfo(name = "default_value_json")
    val defaultValueJson: String? = null
)
