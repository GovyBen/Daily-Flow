package com.mhss.app.tracking.presentation.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TrackingDashboardSectionTest {

    @Test
    fun quickTemplateAndHistoryButtonsEmitNavigationCallbacks() = runComposeUiTest {
        var quickTemplateId: String? = null
        var historyOpened = false
        setContent {
            MaterialTheme {
                TrackingDashboardContent(
                    state = TrackingDashboardUiState(
                        isLoading = false,
                        quickTemplates = listOf(template()),
                        todaySummaries = listOf(
                            TrackingTodayTemplateSummary(
                                templateId = "fitness",
                                name = "Fitness",
                                color = 0xFF6750A4,
                                sessionCount = 2,
                                lastRecordedAtEpochMilli = 2_000
                            )
                        ),
                        totalSessionCount = 2
                    ),
                    onQuickRecord = { quickTemplateId = it },
                    onOpenHistory = { historyOpened = true }
                )
            }
        }

        onNodeWithTag(TRACKING_DASHBOARD_SECTION_TAG).assertIsDisplayed()
        onNodeWithTag(trackingDashboardQuickTemplateTag("fitness")).performClick()
        onNodeWithTag(TRACKING_DASHBOARD_HISTORY_TAG).performClick()

        assertEquals("fitness", quickTemplateId)
        assertEquals(true, historyOpened)
    }

    private fun template() = TrackingTemplateSummary(
        id = "fitness",
        name = "Fitness",
        description = "",
        icon = "",
        color = 0xFF6750A4,
        isPinned = true,
        displayOrder = 0,
        createdAtEpochMilli = 1,
        updatedAtEpochMilli = 1,
        lastRecordedAtEpochMilli = 2_000,
        fields = emptyList()
    )
}
