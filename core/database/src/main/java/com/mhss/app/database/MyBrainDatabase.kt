package com.mhss.app.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mhss.app.database.converters.DBConverters
import com.mhss.app.database.dao.AlarmDao
import com.mhss.app.database.dao.BookmarkDao
import com.mhss.app.database.dao.DailyItemDao
import com.mhss.app.database.dao.DashboardPanelDao
import com.mhss.app.database.dao.DiaryDao
import com.mhss.app.database.dao.NoteDao
import com.mhss.app.database.dao.ReminderDao
import com.mhss.app.database.dao.TaskDao
import com.mhss.app.database.entity.AlarmEntity
import com.mhss.app.database.entity.BookmarkEntity
import com.mhss.app.database.entity.DailyItemCalendarSyncEntity
import com.mhss.app.database.entity.DailyItemEntity
import com.mhss.app.database.entity.DashboardPanelEntity
import com.mhss.app.database.entity.DiaryEntryEntity
import com.mhss.app.database.entity.NoteEntity
import com.mhss.app.database.entity.NoteFolderEntity
import com.mhss.app.database.entity.ReminderEntity
import com.mhss.app.database.entity.TaskEntity
import com.mhss.app.tracking.data.database.dao.TrackingDataPointDao
import com.mhss.app.tracking.data.database.dao.TrackingSessionDao
import com.mhss.app.tracking.data.database.dao.TrackingTemplateDao
import com.mhss.app.tracking.data.database.dao.TrackingTrackerDao
import com.mhss.app.tracking.data.database.entity.DataPointEntity
import com.mhss.app.tracking.data.database.entity.RecordSessionEntity
import com.mhss.app.tracking.data.database.entity.RecordTemplateEntity
import com.mhss.app.tracking.data.database.entity.TemplateFieldEntity
import com.mhss.app.tracking.data.database.entity.TrackerEntity
import com.mhss.app.tracking.data.database.entity.TrackerOptionEntity

@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
        DiaryEntryEntity::class,
        BookmarkEntity::class,
        AlarmEntity::class,
        NoteFolderEntity::class,
        ReminderEntity::class,
        RecordTemplateEntity::class,
        TrackerEntity::class,
        TemplateFieldEntity::class,
        TrackerOptionEntity::class,
        RecordSessionEntity::class,
        DataPointEntity::class,
        DailyItemEntity::class,
        DailyItemCalendarSyncEntity::class,
        DashboardPanelEntity::class
    ],
    version = 9,
    exportSchema = true
)
@TypeConverters(DBConverters::class)
abstract class MyBrainDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun diaryDao(): DiaryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun alarmDao(): AlarmDao
    abstract fun reminderDao(): ReminderDao
    abstract fun dailyItemDao(): DailyItemDao
    abstract fun dashboardPanelDao(): DashboardPanelDao
    abstract fun templateDao(): TrackingTemplateDao
    abstract fun trackerDao(): TrackingTrackerDao
    abstract fun sessionDao(): TrackingSessionDao
    abstract fun dataPointDao(): TrackingDataPointDao

    companion object {
        const val DATABASE_NAME = "by_brain_db"
    }
}
