package com.mhss.app.tracking.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mhss.app.tracking.data.database.TrackingDatabaseConverters
import com.mhss.app.tracking.domain.model.RecordSource

@Entity(
    tableName = "tracking_record_sessions",
    foreignKeys = [
        ForeignKey(
            entity = RecordTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["template_id", "occurred_at_epoch_milli"])
    ]
)
@TypeConverters(TrackingDatabaseConverters::class)
data class RecordSessionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "template_id")
    val templateId: String,
    @ColumnInfo(name = "occurred_at_epoch_milli")
    val occurredAtEpochMilli: Long,
    @ColumnInfo(name = "zone_id")
    val zoneId: String,
    val note: String? = null,
    val source: RecordSource,
    @ColumnInfo(name = "created_at_epoch_milli")
    val createdAtEpochMilli: Long,
    @ColumnInfo(name = "updated_at_epoch_milli")
    val updatedAtEpochMilli: Long
)
