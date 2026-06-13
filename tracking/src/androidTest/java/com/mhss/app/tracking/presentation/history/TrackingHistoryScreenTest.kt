package com.mhss.app.tracking.presentation.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.platform.app.InstrumentationRegistry
import com.mhss.app.tracking.R
import com.mhss.app.tracking.domain.model.RecordSource
import com.mhss.app.tracking.domain.model.TextConfig
import com.mhss.app.tracking.domain.model.TrackingFieldDraft
import com.mhss.app.tracking.domain.model.TrackingRecordHistory
import com.mhss.app.tracking.domain.model.TrackingRecordedPoint
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import com.mhss.app.tracking.domain.model.TrackingTrackerDraft
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TrackingHistoryScreenTest {

    @Test
    fun templateFilterAndHistoryCardShowRecordFields() = runComposeUiTest {
        val first = template("first", "First")
        val second = template("second", "Second")
        var selectedId: String? = null
        setContent {
            MaterialTheme {
                TrackingHistoryContent(
                    state = state(first, listOf(first, second)),
                    onTemplateSelected = { selectedId = it }
                )
            }
        }

        onNodeWithText("Note").assertIsDisplayed()
        onNodeWithText("Recorded text").assertIsDisplayed()
        onNodeWithTag(TRACKING_HISTORY_TEMPLATE_TAG).performClick()
        onNodeWithTag(trackingHistoryTemplateOptionTag("second")).performClick()

        assertEquals("second", selectedId)
    }

    @Test
    fun deleteRequiresConfirmationBeforeCallback() = runComposeUiTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val template = template("first", "First")
        var deletedId: String? = null
        setContent {
            MaterialTheme {
                TrackingHistoryContent(
                    state = state(template, listOf(template)),
                    onDelete = { deletedId = it }
                )
            }
        }

        onNodeWithTag(trackingHistoryDeleteTag("session")).performClick()
        onNodeWithText(context.getString(R.string.tracking_history_delete_title))
            .assertIsDisplayed()
        onNodeWithTag(TRACKING_HISTORY_DELETE_CONFIRM_TAG).performClick()

        assertEquals("session", deletedId)
    }

    @Test
    fun editorDisablesSaveWhileRepositoryUpdateIsRunning() = runComposeUiTest {
        val template = template("first", "First")
        setContent {
            MaterialTheme {
                TrackingRecordEditorContent(
                    template = template,
                    state = TrackingRecordEditorState(
                        sessionId = "session",
                        occurredAtEpochMilli = 1_700_000_000_000,
                        note = "",
                        source = RecordSource.MANUAL,
                        inputs = mapOf("text" to TrackerInputValue.Text("Recorded text")),
                        isSaving = true
                    )
                )
            }
        }

        onNodeWithTag(TRACKING_HISTORY_EDITOR_SAVE_TAG)
            .performScrollTo()
            .assertIsNotEnabled()
    }

    private fun state(
        selected: TrackingTemplateSummary,
        templates: List<TrackingTemplateSummary>
    ) = TrackingHistoryUiState(
        isLoading = false,
        templates = templates,
        selectedTemplate = selected,
        selectedDayEpochMilli = 1_700_000_000_000,
        records = listOf(
            TrackingRecordHistory(
                id = "session",
                templateId = selected.id,
                occurredAtEpochMilli = 1_700_000_000_000,
                zoneId = "Asia/Shanghai",
                note = "Session note",
                source = RecordSource.MANUAL,
                points = listOf(
                    TrackingRecordedPoint(
                        id = "point",
                        trackerId = "text-tracker",
                        epochMilli = 1_700_000_000_000,
                        utcOffsetSeconds = 28_800,
                        value = null,
                        label = null,
                        note = "Recorded text",
                        optionId = null
                    )
                )
            )
        )
    )

    private fun template(id: String, name: String) = TrackingTemplateSummary(
        id = id,
        name = name,
        description = "",
        icon = "",
        color = 0,
        isPinned = false,
        displayOrder = 0,
        createdAtEpochMilli = 1,
        updatedAtEpochMilli = 1,
        lastRecordedAtEpochMilli = 1_700_000_000_000,
        fields = listOf(
            TrackingFieldDraft(
                id = "text",
                trackerId = "text-tracker",
                tracker = TrackingTrackerDraft(
                    id = "text-tracker",
                    name = "Note",
                    config = TextConfig()
                ),
                displayOrder = 0
            )
        )
    )
}
