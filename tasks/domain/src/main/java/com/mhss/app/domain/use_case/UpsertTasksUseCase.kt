package com.mhss.app.domain.use_case

import com.mhss.app.domain.model.Task
import com.mhss.app.domain.repository.TaskRepository
import com.mhss.app.widget.WidgetUpdater
import org.koin.core.annotation.Factory

/**
 * Bulk upsert used by AI tooling. Delegates to [UpsertTaskUseCase] for each
 * task so that reminder lifecycle is consistently handled.
 */
@Factory
class UpsertTasksUseCase(
    private val tasksRepository: TaskRepository,
    private val upsertTask: UpsertTaskUseCase,
    private val widgetUpdater: WidgetUpdater
) {
    suspend operator fun invoke(
        tasks: List<Task>,
        updateWidget: Boolean = true
    ) {
        for (task in tasks) {
            upsertTask(task, updateWidget = false)
        }
        if (updateWidget) widgetUpdater.updateAll(WidgetUpdater.WidgetType.Tasks)
    }
}
