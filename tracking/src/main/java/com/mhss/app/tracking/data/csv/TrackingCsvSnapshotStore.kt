package com.mhss.app.tracking.data.csv

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.mhss.app.tracking.data.database.dao.TrackingDataPointDao
import com.mhss.app.tracking.data.database.dao.TrackingSessionDao
import com.mhss.app.tracking.data.database.dao.TrackingTemplateDao
import com.mhss.app.tracking.data.database.dao.TrackingTrackerDao
import org.koin.core.annotation.Factory

@Factory
class TrackingCsvSnapshotStore(
    private val database: RoomDatabase,
    private val templateDao: TrackingTemplateDao,
    private val trackerDao: TrackingTrackerDao,
    private val sessionDao: TrackingSessionDao,
    private val dataPointDao: TrackingDataPointDao
) {
    suspend fun readSnapshot(): TrackingCsvSnapshot = database.withTransaction {
        TrackingCsvSnapshot(
            templates = templateDao.getAllTemplates(),
            trackers = trackerDao.getAllTrackers(),
            options = trackerDao.getAllOptions(),
            fields = templateDao.getAllFields(),
            sessions = sessionDao.getAllSessions(),
            dataPoints = dataPointDao.getAllDataPoints()
        )
    }

    suspend fun import(snapshot: TrackingCsvSnapshot) {
        database.withTransaction {
            trackerDao.upsertTrackers(snapshot.trackers)
            trackerDao.upsertOptions(snapshot.options)
            for (template in snapshot.templates) {
                templateDao.upsertTemplate(template)
            }
            templateDao.upsertFields(snapshot.fields)
            sessionDao.upsertSessions(snapshot.sessions)
            dataPointDao.upsertDataPoints(snapshot.dataPoints)
        }
    }
}
