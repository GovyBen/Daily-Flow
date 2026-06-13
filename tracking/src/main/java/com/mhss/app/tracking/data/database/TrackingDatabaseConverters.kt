package com.mhss.app.tracking.data.database

import androidx.room.TypeConverter
import com.mhss.app.tracking.domain.model.TrackerType

class TrackingDatabaseConverters {

    @TypeConverter
    fun trackerTypeToString(value: TrackerType): String = value.name

    @TypeConverter
    fun stringToTrackerType(value: String): TrackerType = TrackerType.valueOf(value)
}
