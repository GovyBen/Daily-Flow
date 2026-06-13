package com.mhss.app.tracking.data.database

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.mhss.app.tracking.data.database.dao.TrackingDataPointDao
import com.mhss.app.tracking.data.database.dao.TrackingSessionDao
import com.mhss.app.tracking.data.database.dao.TrackingTemplateDao
import com.mhss.app.tracking.data.database.dao.TrackingTrackerDao
import com.mhss.app.tracking.data.database.entity.DataPointEntity
import com.mhss.app.tracking.data.database.entity.RecordSessionEntity
import com.mhss.app.tracking.data.database.entity.RecordTemplateEntity
import com.mhss.app.tracking.data.database.entity.TemplateFieldEntity

class TrackingTransactionStore(
    private val database: RoomDatabase,
    private val templateDao: TrackingTemplateDao,
    private val trackerDao: TrackingTrackerDao,
    private val sessionDao: TrackingSessionDao,
    private val dataPointDao: TrackingDataPointDao
) {

    suspend fun createTemplate(
        template: RecordTemplateEntity,
        fields: List<TemplateFieldEntity>
    ) {
        require(fields.all { it.templateId == template.id }) {
            "Every field must reference the template being created"
        }
        database.withTransaction {
            templateDao.insertTemplate(template)
            templateDao.insertFields(fields)
        }
    }

    suspend fun saveSession(
        session: RecordSessionEntity,
        points: List<DataPointEntity>
    ) {
        requireSessionPoints(session, points)
        database.withTransaction {
            sessionDao.insertSession(session)
            dataPointDao.insertDataPoints(points)
        }
    }

    suspend fun updateSession(
        session: RecordSessionEntity,
        points: List<DataPointEntity>
    ) {
        requireSessionPoints(session, points)
        database.withTransaction {
            checkNotNull(sessionDao.getSession(session.id)) {
                "Cannot update a record session that does not exist"
            }
            sessionDao.updateSession(session)
            dataPointDao.deleteDataPointsForSession(session.id)
            dataPointDao.insertDataPoints(points)
        }
    }

    suspend fun deleteSession(sessionId: String) {
        database.withTransaction {
            val session = checkNotNull(sessionDao.getSession(sessionId)) {
                "Cannot delete a record session that does not exist"
            }
            sessionDao.deleteSession(session)
        }
    }

    suspend fun deactivateTemplate(id: String, updatedAtEpochMilli: Long) {
        check(templateDao.deactivateTemplate(id, updatedAtEpochMilli) == 1) {
            "Cannot deactivate a template that does not exist"
        }
    }

    suspend fun deactivateTracker(id: String, updatedAtEpochMilli: Long) {
        check(trackerDao.deactivateTracker(id, updatedAtEpochMilli) == 1) {
            "Cannot deactivate a tracker that does not exist"
        }
    }

    suspend fun deactivateOption(id: String) {
        check(trackerDao.deactivateOption(id) == 1) {
            "Cannot deactivate an option that does not exist"
        }
    }

    private fun requireSessionPoints(
        session: RecordSessionEntity,
        points: List<DataPointEntity>
    ) {
        require(points.all { it.sessionId == session.id }) {
            "Every data point must reference the record session being saved"
        }
    }
}
