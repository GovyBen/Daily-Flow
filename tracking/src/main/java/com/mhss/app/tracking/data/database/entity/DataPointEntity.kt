/*
 * This file is part of Track & Graph.
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Daily Flow modification: replaced the compound feature/time key with
 * a generated ID and added session, option and update metadata.
 */

package com.mhss.app.tracking.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracking_data_points",
    foreignKeys = [
        ForeignKey(
            entity = RecordSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TrackerEntity::class,
            parentColumns = ["id"],
            childColumns = ["tracker_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = TrackerOptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["option_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["tracker_id", "epoch_milli"]),
        Index(value = ["option_id"])
    ]
)
data class DataPointEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String? = null,
    @ColumnInfo(name = "tracker_id")
    val trackerId: String,
    @ColumnInfo(name = "epoch_milli")
    val epochMilli: Long,
    @ColumnInfo(name = "utc_offset_seconds")
    val utcOffsetSeconds: Int,
    val value: Double? = null,
    val label: String? = null,
    val note: String? = null,
    @ColumnInfo(name = "option_id")
    val optionId: String? = null,
    @ColumnInfo(name = "created_at_epoch_milli")
    val createdAtEpochMilli: Long,
    @ColumnInfo(name = "updated_at_epoch_milli")
    val updatedAtEpochMilli: Long
)
