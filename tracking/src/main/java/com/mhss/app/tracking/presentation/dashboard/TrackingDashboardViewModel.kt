package com.mhss.app.tracking.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.tracking.domain.model.TrackingRecordHistory
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import com.mhss.app.tracking.domain.usecase.ObserveRecordHistoryUseCase
import com.mhss.app.tracking.domain.usecase.ObserveTemplatesUseCase
import com.mhss.app.tracking.presentation.history.TrackingDayRange
import com.mhss.app.tracking.presentation.history.trackingDayRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TrackingDashboardViewModel(
    private val observeTemplates: ObserveTemplatesUseCase,
    private val observeRecordHistory: ObserveRecordHistoryUseCase
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(TrackingDashboardUiState())
    val uiState = mutableUiState.asStateFlow()

    private var observedRange: TrackingDayRange? = null
    private var observeJob: Job? = null

    fun loadToday(nowEpochMilli: Long = System.currentTimeMillis()) {
        val range = trackingDayRange(nowEpochMilli)
        if (range == observedRange && observeJob?.isActive == true) return
        observedRange = range
        observeJob?.cancel()
        observeJob = observeTemplates()
            .flatMapLatest { templates ->
                if (templates.isEmpty()) {
                    flowOf(buildTrackingDashboardState(emptyList(), emptyMap()))
                } else {
                    combine(
                        templates.map { template ->
                            observeRecordHistory(
                                template.id,
                                range.startInclusive,
                                range.endExclusive
                            ).map { history -> template.id to history }
                        }
                    ) { histories ->
                        buildTrackingDashboardState(templates, histories.toMap())
                    }
                }
            }
            .onEach { mutableUiState.value = it }
            .launchIn(viewModelScope)
    }
}

data class TrackingDashboardUiState(
    val isLoading: Boolean = true,
    val quickTemplates: List<TrackingTemplateSummary> = emptyList(),
    val todaySummaries: List<TrackingTodayTemplateSummary> = emptyList(),
    val totalSessionCount: Int = 0
)

data class TrackingTodayTemplateSummary(
    val templateId: String,
    val name: String,
    val color: Long,
    val sessionCount: Int,
    val lastRecordedAtEpochMilli: Long
)

internal fun buildTrackingDashboardState(
    templates: List<TrackingTemplateSummary>,
    historyByTemplate: Map<String, List<TrackingRecordHistory>>
): TrackingDashboardUiState {
    val quickTemplates = templates
        .sortedWith(
            compareByDescending<TrackingTemplateSummary> { it.isPinned }
                .thenByDescending { it.lastRecordedAtEpochMilli ?: Long.MIN_VALUE }
                .thenBy(TrackingTemplateSummary::displayOrder)
                .thenBy(TrackingTemplateSummary::id)
        )
        .take(MAX_QUICK_TEMPLATES)
    val summaries = templates.mapNotNull { template ->
        val history = historyByTemplate[template.id].orEmpty()
        if (history.isEmpty()) {
            null
        } else {
            TrackingTodayTemplateSummary(
                templateId = template.id,
                name = template.name,
                color = template.color,
                sessionCount = history.size,
                lastRecordedAtEpochMilli = history.maxOf(
                    TrackingRecordHistory::occurredAtEpochMilli
                )
            )
        }
    }.sortedWith(
        compareByDescending<TrackingTodayTemplateSummary> {
            it.lastRecordedAtEpochMilli
        }.thenBy(TrackingTodayTemplateSummary::templateId)
    )
    return TrackingDashboardUiState(
        isLoading = false,
        quickTemplates = quickTemplates,
        todaySummaries = summaries,
        totalSessionCount = summaries.sumOf(TrackingTodayTemplateSummary::sessionCount)
    )
}

private const val MAX_QUICK_TEMPLATES = 4
