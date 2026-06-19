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
    private val requestedTrackerIds = MutableStateFlow<Set<String>>(emptySet())
    private val analyticsRange = MutableStateFlow(TrackingAnalyticsRange.DAY)
    private val analyticsAggregation = MutableStateFlow(AggregationOperation.SUM)
    private val analyticsChartType = MutableStateFlow(TrackingAnalyticsChartType.LINE)
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

    private val selectedTrackers = combine(
        trackerOptions,
        requestedTrackerIds
    ) { options, ids ->
        options.filter { it.trackerId in ids }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val loadState = combine(
        selectedTrackers,
        selectedTracker,
        analyticsRange,
        analyticsAggregation,
        retryToken
    ) { trackers, tracker, selRange, selAggregation, _ ->
        when {
            trackers.size >= 2 -> MultiTrackerRequest(
                trackers = trackers,
                range = selRange,
                aggregation = selAggregation,
                asOfDate = asOfDate
            )
            tracker != null -> TrackingAnalyticsRequest(
                tracker = tracker,
                range = selRange,
                aggregation = selAggregation,
                asOfDate = asOfDate
            )
            else -> null
        }
    }.flatMapLatest { request ->
        when (request) {
            null -> flowOf<TrackingAnalyticsLoadState>(TrackingAnalyticsLoadState.Idle)
            is MultiTrackerRequest -> flow {
                emit(TrackingAnalyticsLoadState.Loading)
                val result = try {
                    TrackingAnalyticsLoadState.Ready(loader.loadMulti(request))
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Throwable) {
                    TrackingAnalyticsLoadState.Failed
                }
                emit(result)
            }
            is TrackingAnalyticsRequest -> flow {
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
            else -> flowOf(TrackingAnalyticsLoadState.Idle)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrackingAnalyticsLoadState.Idle
    )

    // Combine tracker+selection info into one flow to keep uiState under 5 parameters
    private val trackersAndSelection = combine(
        trackerOptions,
        selectedTracker,
        selectedTrackers
    ) { options, tracker, trackers ->
        Triple(options, tracker, trackers)
    }

    // Combine load-related info
    private val loadInfo = combine(
        loadState,
        loaded
    ) { load, isLoaded ->
        Pair(load, isLoaded)
    }

    val uiState = combine(
        trackersAndSelection,
        analyticsRange,
        analyticsAggregation,
        analyticsChartType,
        loadInfo
    ) { (options, tracker, trackers), selRange, selAggregation, selChart, (load, isLoaded) ->
        TrackingAnalyticsUiState(
            isLoading = !isLoaded || load == TrackingAnalyticsLoadState.Loading,
            trackerOptions = options,
            selectedTracker = tracker,
            selectedTrackers = trackers,
            range = selRange,
            aggregation = selAggregation,
            chartType = selChart,
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
        requestedTrackerIds.value = emptySet()
    }

    fun toggleTracker(trackerId: String) {
        val current = requestedTrackerIds.value.toMutableSet()
        if (trackerId in current) {
            current.remove(trackerId)
            if (current.isEmpty()) {
                requestedTrackerId.value = null
                requestedTrackerIds.value = emptySet()
            } else if (current.size == 1) {
                // Fell from multi to single
                requestedTrackerId.value = current.first()
                requestedTrackerIds.value = emptySet()
            } else {
                requestedTrackerIds.value = current
            }
        } else {
            // Adding a new tracker
            val existingSingle = requestedTrackerId.value
            if (existingSingle != null && existingSingle != trackerId) {
                // Switch from single to multi with both
                current.add(existingSingle)
                current.add(trackerId)
                requestedTrackerId.value = null
                requestedTrackerIds.value = current
            } else if (existingSingle != null) {
                // Already selected as single, do nothing (or toggle off)
            } else {
                // No current selection — start single
                requestedTrackerId.value = trackerId
                requestedTrackerIds.value = emptySet()
            }
        }
    }
    fun selectRange(value: TrackingAnalyticsRange) {
        analyticsRange.value = value
    }

    fun selectAggregation(value: AggregationOperation) {
        analyticsAggregation.value = value
    }

    fun selectChartType(value: TrackingAnalyticsChartType) {
        analyticsChartType.value = value
    }

    fun retry() {
        retryToken.value += 1
    }
}
