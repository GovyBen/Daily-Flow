package com.mhss.app.mybrain.data

import com.mhss.app.data.tools.TrackingDataProvider
import com.mhss.app.tracking.domain.usecase.ObserveRecordHistoryUseCase
import com.mhss.app.tracking.domain.usecase.ObserveTemplatesUseCase
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single

@Single
class TrackingDataProviderImpl(
    private val observeTemplates: ObserveTemplatesUseCase,
    private val observeRecordHistory: ObserveRecordHistoryUseCase
) : TrackingDataProvider {

    override suspend fun getTemplates(): String {
        val templates = observeTemplates().first()
        if (templates.isEmpty()) return "No templates found."
        return templates.joinToString("\n") { t ->
            "Template: ${t.name} (id=${t.id}), fields: ${t.fields.joinToString { "${it.tracker.name}(${it.tracker.config})" }}"
        }
    }

    override suspend fun getSessions(templateId: String): String {
        val end = System.currentTimeMillis()
        val start = end - 90L * 24 * 3600 * 1000
        val sessions = observeRecordHistory(templateId, start, end).first()
        if (sessions.isEmpty()) return "No sessions found."
        return sessions.take(20).joinToString("\n") {
            "ID=${it.id}, at=${it.occurredAtEpochMilli}, note=${it.note ?: ""}"
        }
    }
}
