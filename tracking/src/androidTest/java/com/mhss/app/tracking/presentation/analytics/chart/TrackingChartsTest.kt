package com.mhss.app.tracking.presentation.analytics.chart

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TrackingChartsTest {

    @Test
    fun emptyChartsUseSharedEmptyState() = runComposeUiTest {
        setContent {
            MaterialTheme {
                Column {
                    TrackingLineChart(emptyList(), "Line")
                    TrackingBarChart(emptyList(), "Bar")
                    TrackingPieChart(emptyList(), "Pie")
                }
            }
        }

        onAllNodesWithTag(TRACKING_CHART_EMPTY_TAG).assertCountEquals(3)
    }

    @Test
    fun lineAndBarChartsRenderAtLargeFontScale() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 2f)
            ) {
                MaterialTheme {
                    Column {
                        TrackingLineChart(values(), "Weekly trend")
                        TrackingBarChart(values(), "Weekly totals")
                    }
                }
            }
        }

        onNodeWithTag(TRACKING_LINE_CHART_TAG).assertIsDisplayed()
        onNodeWithTag(TRACKING_BAR_CHART_TAG).assertIsDisplayed()
    }

    @Test
    fun pieChartAndLegendRemainUsableAtLargeFontScale() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 2f)
            ) {
                MaterialTheme {
                    TrackingPieChart(values(), "Option distribution")
                }
            }
        }

        onNodeWithTag(TRACKING_PIE_CHART_TAG).assertIsDisplayed()
        onNodeWithText("Chest: 4").assertIsDisplayed()
        onNodeWithText("Back: 6").assertIsDisplayed()
    }

    private fun values() = listOf(
        TrackingChartValue("Chest", 4.0),
        TrackingChartValue("Back", 6.0)
    )
}
