package com.mhss.app.tracking.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.tracking.analytics.aggregation.AggregationOperation
import com.mhss.app.tracking.domain.model.TrackerType
import com.mhss.app.tracking.domain.usecase.ObserveTemplatesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.android.annotation.KoinViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
@KoinViewModel
class TrackingAnalyticsViewModel(
    observeTemplates: ObserveTemplatesUseCase,
    private val loader: TrackingAnalyticsLoader
) : ViewModel() {

    private val loaded = MutableStateFlow(false)
    private val initialTemplateId = MutableStateFlow<String?>(null)
    private val requestedTrackerId = MutableStateFlow<String?>(null)
    private val range = MutableStateFlow(TrackingAnalyticsRange.DAY)
    private val aggregation = MutableStateFlow(AggregationOperation.SUM)
    private val chartType = MutableStateFlow(TrackingAnalyticsChartType.LINE)
    private val retryToken = MutableStateFlow(0)
    private val asOfDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

    private val trackerOptions = observeTemplates()
        .map { templates ->
            templates.flatMap { template ->
                template.fields.mapNotNull { field ->
                    val trackerId = field.trackerId ?: field.tracker.id ?: return@mapNotNull null
                    val trackerType = field.tracker.config.trackerType
                    if (trackerType == TrackerType.TEXT) return@mapNotNull null
                    TrackingAnalyticsTrackerOption(
                        trackerId = trackerId,
                        templateId = template.id,
                        templateName = template.name,
                        trackerName = field.displayNameOverride
                            ?.takeIf(String::isNotBlank)
                            ?: field.tracker.name,
                        trackerType = trackerType,
                        unit = field.tracker.unit
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val selectedTracker = combine(
        trackerOptions,
        requestedTrackerId,
        initialTemplateId,
        loaded
    ) { options, requestedId, templateId, isLoaded ->
        if (!isLoaded) {
            null
        } else {
            options.firstOrNull { it.trackerId == requestedId }
                ?: options.firstOrNull { it.templateId == templateId }
                ?: options.firstOrNull()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    private val loadState = combine(
        selectedTracker,
        range,
        aggregation,
        retryToken
    ) { tracker, selectedRange, selectedAggregation, _ ->
        tracker?.let {
            TrackingAnalyticsRequest(
                tracker = it,
                range = selectedRange,
                aggregation = selectedAggregation,
                asOfDate = asOfDate
            )
        }
    }.flatMapLatest { request ->
        if (request == null) {
            flowOf<TrackingAnalyticsLoadState>(TrackingAnalyticsLoadState.Idle)
        } else {
            flow<TrackingAnalyticsLoadState> {
                emit(TrackingAnalyticsLoadState.Loading)
                val result = try {
                    TrackingAnalyticsLoadState.Ready(loader.load(request))
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Throwable) {
                    TrackingAnalyticsLoadState.Failed
                }
                emit(result)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrackingAnalyticsLoadState.Idle
    )

    private val controls = combine(
        range,
        aggregation,
        chartType
    ) { selectedRange, selectedAggregation, selectedChart ->
        AnalyticsControls(
            range = selectedRange,
            aggregation = selectedAggregation,
            chartType = selectedChart
        )
    }

    val uiState = combine(
        trackerOptions,
        selectedTracker,
        controls,
        loadState,
        loaded
    ) { options, tracker, selectedControls, load, isLoaded ->
        TrackingAnalyticsUiState(
            isLoading = !isLoaded || load == TrackingAnalyticsLoadState.Loading,
            trackerOptions = options,
            selectedTracker = tracker,
            range = selectedControls.range,
            aggregation = selectedControls.aggregation,
            chartType = selectedControls.chartType,
            data = (load as? TrackingAnalyticsLoadState.Ready)?.data,
            loadFailed = load == TrackingAnalyticsLoadState.Failed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrackingAnalyticsUiState()
    )

    fun load(templateId: String?) {
        if (loaded.value) return
        initialTemplateId.value = templateId
        loaded.value = true
    }

    fun selectTracker(trackerId: String) {
        requestedTrackerId.value = trackerId
    }

    fun selectRange(value: TrackingAnalyticsRange) {
        range.value = value
    }

    fun selectAggregation(value: AggregationOperation) {
        aggregation.value = value
    }

    fun selectChartType(value: TrackingAnalyticsChartType) {
        chartType.value = value
    }

    fun retry() {
        retryToken.value += 1
    }
}

private data class AnalyticsControls(
    val range: TrackingAnalyticsRange,
    val aggregation: AggregationOperation,
    val chartType: TrackingAnalyticsChartType
)
