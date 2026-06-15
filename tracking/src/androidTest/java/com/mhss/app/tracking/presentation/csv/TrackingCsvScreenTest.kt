package com.mhss.app.tracking.presentation.csv

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.platform.app.InstrumentationRegistry
import com.mhss.app.tracking.R
import com.mhss.app.tracking.data.csv.TrackingCsvErrorReason
import com.mhss.app.tracking.data.csv.TrackingCsvImportPreview
import com.mhss.app.tracking.data.csv.TrackingCsvSnapshot
import com.mhss.app.tracking.data.database.entity.DataPointEntity
import com.mhss.app.tracking.data.database.entity.RecordSessionEntity
import com.mhss.app.tracking.data.database.entity.RecordTemplateEntity
import com.mhss.app.tracking.data.database.entity.TemplateFieldEntity
import com.mhss.app.tracking.data.database.entity.TrackerEntity
import com.mhss.app.tracking.data.database.entity.TrackerOptionEntity
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TrackingCsvScreenTest {

    @Test
    fun actionButtonsEmitExportAndFileSelectionRequests() = runComposeUiTest {
        var exportRequested = false
        var importRequested = false
        setContent {
            MaterialTheme {
                TrackingCsvContent(
                    state = TrackingCsvUiState(),
                    onExport = { exportRequested = true },
                    onChooseImport = { importRequested = true }
                )
            }
        }

        onNodeWithTag(TRACKING_CSV_EXPORT_TAG).performClick()
        onNodeWithTag(TRACKING_CSV_IMPORT_TAG).performScrollTo().performClick()

        assertTrue(exportRequested)
        assertTrue(importRequested)
    }

    @Test
    fun validatedPreviewShowsEveryCountAndRequiresConfirmation() = runComposeUiTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var confirmed = false
        val preview = TrackingCsvImportPreview(
            sourceName = "DailyFlow_Tracking.csv",
            snapshot = completeSnapshot()
        )
        setContent {
            MaterialTheme {
                TrackingCsvContent(
                    state = TrackingCsvUiState(preview = preview),
                    onConfirmImport = { confirmed = true }
                )
            }
        }

        onNodeWithTag(TRACKING_CSV_PREVIEW_TAG).performScrollTo().assertIsDisplayed()
        onNodeWithText("DailyFlow_Tracking.csv").assertIsDisplayed()
        onNodeWithText(
            context.getString(R.string.tracking_csv_count_templates, 1)
        ).assertIsDisplayed()
        onNodeWithText(
            context.getString(R.string.tracking_csv_count_trackers, 1)
        ).assertIsDisplayed()
        onNodeWithText(
            context.getString(R.string.tracking_csv_count_options, 1)
        ).assertIsDisplayed()
        onNodeWithText(
            context.getString(R.string.tracking_csv_count_fields, 1)
        ).assertIsDisplayed()
        onNodeWithText(
            context.getString(R.string.tracking_csv_count_sessions, 1)
        ).assertIsDisplayed()
        onNodeWithText(
            context.getString(R.string.tracking_csv_count_points, 1)
        ).assertIsDisplayed()

        onNodeWithTag(TRACKING_CSV_CONFIRM_TAG).performClick()
        assertTrue(confirmed)
    }

    @Test
    fun validationErrorDisplaysCsvLineAndDetail() = runComposeUiTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val state = TrackingCsvUiState(
            error = TrackingCsvUiError(
                reason = TrackingCsvErrorReason.INVALID_VALUE,
                lineNumber = 7,
                detail = "utc_offset_seconds"
            )
        )
        setContent {
            MaterialTheme {
                TrackingCsvContent(state = state)
            }
        }

        onNodeWithText(
            context.getString(
                R.string.tracking_csv_error_line_detail,
                7,
                context.getString(R.string.tracking_csv_error_invalid_value),
                "utc_offset_seconds"
            )
        ).assertIsDisplayed()
    }

    private fun completeSnapshot(): TrackingCsvSnapshot = TrackingCsvSnapshot(
        templates = listOf(
            RecordTemplateEntity(
                id = "template",
                name = "Health",
                color = 1,
                createdAtEpochMilli = 1,
                updatedAtEpochMilli = 1
            )
        ),
        trackers = listOf(
            TrackerEntity(
                id = "tracker",
                name = "Mood",
                type = "SCALE",
                configJson = "{}",
                createdAtEpochMilli = 1,
                updatedAtEpochMilli = 1
            )
        ),
        options = listOf(
            TrackerOptionEntity(
                id = "option",
                trackerId = "tracker",
                label = "Good"
            )
        ),
        fields = listOf(
            TemplateFieldEntity(
                id = "field",
                templateId = "template",
                trackerId = "tracker"
            )
        ),
        sessions = listOf(
            RecordSessionEntity(
                id = "session",
                templateId = "template",
                occurredAtEpochMilli = 1,
                zoneId = "UTC",
                source = "MANUAL",
                createdAtEpochMilli = 1,
                updatedAtEpochMilli = 1
            )
        ),
        dataPoints = listOf(
            DataPointEntity(
                id = "point",
                sessionId = "session",
                trackerId = "tracker",
                epochMilli = 1,
                utcOffsetSeconds = 0,
                createdAtEpochMilli = 1,
                updatedAtEpochMilli = 1
            )
        )
    )
}
