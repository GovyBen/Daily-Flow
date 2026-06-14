package com.mhss.app.tracking.presentation.analytics

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.platform.app.InstrumentationRegistry
import com.mhss.app.tracking.R
import com.mhss.app.tracking.analytics.aggregation.AggregationOperation
import com.mhss.app.tracking.analytics.aggregation.FixedBinSize
import com.mhss.app.tracking.analytics.model.TrackingDailySummary
import com.mhss.app.tracking.analytics.model.TrackingOptionDistribution
import com.mhss.app.tracking.analytics.model.TrackingOptionDistributionItem
import com.mhss.app.tracking.analytics.model.TrackingSeries
import com.mhss.app.tracking.analytics.model.TrackingSeriesPoint
import com.mhss.app.tracking.analytics.model.TrackingStreak
import com.mhss.app.tracking.domain.model.TrackerType
import com.mhss.app.tracking.presentation.analytics.chart.TRACKING_LINE_CHART_TAG
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TrackingAnalyticsScreenTest {

    @Test
    fun emptyTrackerStateExplainsHowToEnableStatistics() = runComposeUiTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        setContent {
            MaterialTheme {
                TrackingAnalyticsContent(
                    state = TrackingAnalyticsUiState(isLoading = false)
                )
            }
        }

        onNodeWithText(
            context.getString(R.string.tracking_analytics_no_trackers)
        ).assertIsDisplayed()
        onNodeWithText(
            context.getString(R.string.tracking_analytics_no_trackers_detail)
        ).assertIsDisplayed()
    }

    @Test
    fun statisticsRenderSummarySeriesDistributionAndStreaks() = runComposeUiTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        setContent {
            MaterialTheme {
                TrackingAnalyticsContent(state = populatedState())
            }
        }

        onNodeWithText("Health - Exercise").assertIsDisplayed()
        onNodeWithText(
            context.getString(R.string.tracking_analytics_streak_days, 3)
        ).performScrollTo().assertIsDisplayed()
        onNodeWithText(
            context.getString(R.string.tracking_analytics_streak_days, 8)
        ).performScrollTo().assertIsDisplayed()
        onNodeWithTag(TRACKING_ANALYTICS_CONTENT_TAG)
            .performScrollToNode(hasTestTag(TRACKING_LINE_CHART_TAG))
        onNodeWithTag(TRACKING_LINE_CHART_TAG).assertIsDisplayed()
        onNodeWithTag(TRACKING_ANALYTICS_CONTENT_TAG)
            .performScrollToNode(hasText("Walk: 3"))
        onNodeWithText("Walk: 3").assertIsDisplayed()
    }

    @Test
    fun selectorsEmitTrackerRangeAndAggregationChanges() = runComposeUiTest {
        var trackerId: String? = null
        var range: TrackingAnalyticsRange? = null
        var aggregation: AggregationOperation? = null
        setContent {
            MaterialTheme {
                TrackingAnalyticsContent(
                    state = populatedState(),
                    onTrackerSelected = { trackerId = it },
                    onRangeSelected = { range = it },
                    onAggregationSelected = { aggregation = it }
                )
            }
        }

        onNodeWithTag(TRACKING_ANALYTICS_TRACKER_TAG).performClick()
        onNodeWithTag(trackingAnalyticsTrackerOptionTag("exercise")).performClick()
        onNodeWithTag(trackingAnalyticsRangeTag(TrackingAnalyticsRange.WEEK))
            .performClick()
        onNodeWithTag(TRACKING_ANALYTICS_AGGREGATION_TAG).performClick()
        onNodeWithTag(trackingAnalyticsAggregationTag(AggregationOperation.AVERAGE))
            .performClick()

        assertEquals("exercise", trackerId)
        assertEquals(TrackingAnalyticsRange.WEEK, range)
        assertEquals(AggregationOperation.AVERAGE, aggregation)
    }

    private fun populatedState(): TrackingAnalyticsUiState {
        val tracker = TrackingAnalyticsTrackerOption(
            trackerId = "exercise",
            templateId = "health",
            templateName = "Health",
            trackerName = "Exercise",
            trackerType = TrackerType.SINGLE_SELECT,
            unit = null
        )
        val date = LocalDate(2026, 6, 14)
        return TrackingAnalyticsUiState(
            isLoading = false,
            trackerOptions = listOf(tracker),
            selectedTracker = tracker,
            data = TrackingAnalyticsData(
                dailySummary = TrackingDailySummary(
                    trackerId = tracker.trackerId,
                    trackerName = tracker.trackerName,
                    trackerType = tracker.trackerType,
                    unit = null,
                    isTrackerActive = true,
                    date = date,
                    sampleCount = 3,
                    sum = 3.0,
                    average = 1.0,
                    minimum = 1.0,
                    maximum = 1.0
                ),
                series = TrackingSeries(
                    trackerId = tracker.trackerId,
                    trackerName = tracker.trackerName,
                    trackerType = tracker.trackerType,
                    unit = null,
                    isTrackerActive = true,
                    binSize = FixedBinSize.DAY,
                    points = listOf(
                        TrackingSeriesPoint(
                            startDate = date,
                            endDateExclusive = LocalDate(2026, 6, 15),
                            startEpochMilli = 0,
                            endEpochMilliExclusive = 1,
                            value = 3.0,
                            sampleCount = 3,
                            label = ""
                        )
                    )
                ),
                distribution = TrackingOptionDistribution(
                    trackerId = tracker.trackerId,
                    trackerName = tracker.trackerName,
                    isTrackerActive = true,
                    totalSelections = 3,
                    items = listOf(
                        TrackingOptionDistributionItem("Walk", 3, 1.0)
                    )
                ),
                currentStreak = TrackingStreak(
                    tracker.trackerId,
                    tracker.trackerName,
                    true,
                    3,
                    LocalDate(2026, 6, 12),
                    date
                ),
                longestStreak = TrackingStreak(
                    tracker.trackerId,
                    tracker.trackerName,
                    true,
                    8,
                    LocalDate(2026, 5, 1),
                    LocalDate(2026, 5, 8)
                )
            )
        )
    }
}
