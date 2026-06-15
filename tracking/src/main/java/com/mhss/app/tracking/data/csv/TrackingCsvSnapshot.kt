package com.mhss.app.tracking.data.csv

import com.mhss.app.tracking.data.database.entity.DataPointEntity
import com.mhss.app.tracking.data.database.entity.RecordSessionEntity
import com.mhss.app.tracking.data.database.entity.RecordTemplateEntity
import com.mhss.app.tracking.data.database.entity.TemplateFieldEntity
import com.mhss.app.tracking.data.database.entity.TrackerEntity
import com.mhss.app.tracking.data.database.entity.TrackerOptionEntity

data class TrackingCsvSnapshot(
    val templates: List<RecordTemplateEntity> = emptyList(),
    val trackers: List<TrackerEntity> = emptyList(),
    val options: List<TrackerOptionEntity> = emptyList(),
    val fields: List<TemplateFieldEntity> = emptyList(),
    val sessions: List<RecordSessionEntity> = emptyList(),
    val dataPoints: List<DataPointEntity> = emptyList()
) {
    val totalRows: Int
        get() = templates.size + trackers.size + options.size +
            fields.size + sessions.size + dataPoints.size

    fun counts(): TrackingCsvCounts = TrackingCsvCounts(
        templates = templates.size,
        trackers = trackers.size,
        options = options.size,
        fields = fields.size,
        sessions = sessions.size,
        dataPoints = dataPoints.size
    )
}

data class TrackingCsvCounts(
    val templates: Int,
    val trackers: Int,
    val options: Int,
    val fields: Int,
    val sessions: Int,
    val dataPoints: Int
) {
    val totalRows: Int
        get() = templates + trackers + options + fields + sessions + dataPoints
}

data class TrackingCsvImportPreview(
    val sourceName: String,
    val snapshot: TrackingCsvSnapshot
) {
    val counts: TrackingCsvCounts = snapshot.counts()
}
