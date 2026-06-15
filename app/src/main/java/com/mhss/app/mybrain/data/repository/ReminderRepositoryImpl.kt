package com.mhss.app.mybrain.data.repository

import com.mhss.app.alarm.model.Reminder
import com.mhss.app.alarm.model.ReminderTargetType
import com.mhss.app.alarm.repository.ReminderRepository
import com.mhss.app.database.dao.ReminderDao
import com.mhss.app.database.entity.toReminder
import com.mhss.app.database.entity.toReminderEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class ReminderRepositoryImpl(
    private val reminderDao: ReminderDao,
    @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher
) : ReminderRepository {

    override suspend fun getAll(): List<Reminder> = withContext(ioDispatcher) {
        reminderDao.getAll().map { it.toReminder() }
    }

    override suspend fun getById(id: Long): Reminder? = withContext(ioDispatcher) {
        reminderDao.getById(id)?.toReminder()
    }

    override suspend fun getByTarget(
        targetType: ReminderTargetType,
        targetId: String
    ): List<Reminder> = withContext(ioDispatcher) {
        reminderDao.getByTarget(targetType.name, targetId).map { it.toReminder() }
    }

    override suspend fun getEnabled(): List<Reminder> = withContext(ioDispatcher) {
        reminderDao.getEnabled().map { it.toReminder() }
    }

    override suspend fun save(reminder: Reminder): Reminder = withContext(ioDispatcher) {
        if (reminder.id == 0L) {
            reminder.copy(id = reminderDao.insert(reminder.toReminderEntity()))
        } else {
            check(reminderDao.update(reminder.toReminderEntity()) == 1) {
                "Reminder ${reminder.id} does not exist"
            }
            reminder
        }
    }

    override suspend fun delete(id: Long) {
        withContext(ioDispatcher) {
            reminderDao.delete(id)
        }
    }
}
