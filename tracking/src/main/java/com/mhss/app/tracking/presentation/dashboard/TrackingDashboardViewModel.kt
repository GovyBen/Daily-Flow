package com.mhss.app.tracking.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.tracking.domain.model.TrackingCalendarRecord
import com.mhss.app.tracking.domain.model.TrackingRecordHistory
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import com.mhss.app.tracking.domain.usecase.ObserveCalendarRecordsUseCase
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
    private var sparklineRange: TrackingDayRange? = null
    private var observeJob: Job? = null
    private var sparklineJob: Job? = null

    fun loadToday(nowEpochMilli: Long = System.currentTimeMillis()) {
        val range = trackingDayRange(nowEpochMilli)
        if (range == observedRange && observeJob?.isActive == true) return
        observedRange = range
        observeJob?.cancel()
        observeJob = observeTemplates()
            .flatMapLatest { templates ->
                if (templates.isEmpty()) {
                    flowOf(buildTrackingDashboardState(emptyList(), emptyMap(), emptyMap()))
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
                        buildTrackingDashboardState(templates, histories.toMap(), emptyMap())
                    }
                }
            }
            .onEach { mutableUiState.value = it }
            .launchIn(viewModelScope)

        // Load 7-day sparkline data
        loadSparklines(nowEpochMilli)
    }

    private fun loadSparklines(nowEpochMilli: Long) {
        val dayRange = 7L * 24 * 60 * 60 * 1000
        val sparklineRange = TrackingDayRange(
            startInclusive = nowEpochMilli - dayRange,
            endExclusive = nowEpochMilli
        )
        if (sparklineRange == this.sparklineRange && sparklineJob?.isActive == true) return
        this.sparklineRange = sparklineRange
        sparklineJob?.cancel()
        sparklineJob = observeTemplates()
            .flatMapLatest { templates ->
                if (templates.isEmpty()) {
                    flowOf(emptyMap<String, List<TrackingRecordHistory>>())
                } else {
                    combine(
                        templates.take(MAX_QUICK_TEMPLATES).map { template ->
                            observeRecordHistory(
                                template.id,
                                sparklineRange.startInclusive,
                                sparklineRange.endExclusive
                            ).map { history -> template.id to history }
                        }
                    ) { histories ->
                        histories.toMap()
                    }
                }
            }
            .onEach { historiesByTemplate ->
                val sparklines = mutableUiState.value.todaySummaries.map { summary ->
                    val history = historiesByTemplate[summary.templateId].orEmpty()
                    val dailyValues = computeDailyValues(history, sparklineRange!!)
                    summary.copy(sparklineValues = dailyValues)
                }
                mutableUiState.value = mutableUiState.value.copy(todaySummaries = sparklines)
            }
            .launchIn(viewModelScope)
    }

    private fun computeDailyValues(
        history: List<TrackingRecordHistory>,
        range: TrackingDayRange
    ): List<Double> {
        // Group history by day and count sessions per day
        val dayMs = 24L * 60 * 60 * 1000
        val days = 7
        val counts = DoubleArray(days) { 0.0 }
        history.forEach { record ->
            val dayOffset = ((record.occurredAtEpochMilli - range.startInclusive) / dayMs).toInt()
            if (dayOffset in 0 until days) {
                counts[dayOffset] += 1.0
            }
        }
        return counts.toList()
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
    val lastRecordedAtEpochMilli: Long,
    val sparklineValues: List<Double> = emptyList()
)

internal fun buildTrackingDashboardState(
    templates: List<TrackingTemplateSummary>,
    historyByTemplate: Map<String, List<TrackingRecordHistory>>,
    sparklineData: Map<String, List<TrackingRecordHistory>>
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
