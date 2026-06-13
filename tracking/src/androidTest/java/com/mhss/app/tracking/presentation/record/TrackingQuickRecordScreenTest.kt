package com.mhss.app.tracking.presentation.record

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.mhss.app.tracking.domain.model.DurationConfig
import com.mhss.app.tracking.domain.model.MultiSelectConfig
import com.mhss.app.tracking.domain.model.TextConfig
import com.mhss.app.tracking.domain.model.TrackingFieldDraft
import com.mhss.app.tracking.domain.model.TrackingOptionDraft
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import com.mhss.app.tracking.domain.model.TrackingTrackerDraft
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import com.mhss.app.tracking.presentation.components.trackingOptionTag
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TrackingQuickRecordScreenTest {

    @Test
    fun threeFieldTemplateSelectsRequiredValueAndSavesInTwoActions() = runComposeUiTest {
        val template = template()
        var state by mutableStateOf(state(template))
        var saveCount = 0
        setContent {
            MaterialTheme {
                TrackingQuickRecordContent(
                    state = state,
                    onInputChange = { fieldId, input ->
                        state = state.copy(
                            fields = state.fields + (
                                fieldId to checkNotNull(state.fields[fieldId]).copy(input = input)
                            )
                        )
                    },
                    onSave = { saveCount++ }
                )
            }
        }

        onNodeWithTag(trackingOptionTag("chest")).performClick()
        onNodeWithTag(TRACKING_QUICK_RECORD_SAVE_TAG).performScrollTo().performClick()

        assertEquals(1, saveCount)
        assertEquals(
            setOf("chest"),
            (state.fields["parts"]?.input as TrackerInputValue.MultiSelect).optionIds
        )
    }

    @Test
    fun savingDisablesSubmitAndSavedResultShowsRealSessionId() = runComposeUiTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val template = template()
        var state by mutableStateOf(state(template).copy(isSaving = true))
        setContent {
            MaterialTheme {
                TrackingQuickRecordContent(state = state)
            }
        }

        onNodeWithTag(TRACKING_QUICK_RECORD_SAVE_TAG)
            .performScrollTo()
            .assertIsNotEnabled()

        state = state.copy(
            isSaving = false,
            savedResult = TrackingQuickRecordResult(
                sessionId = "session-real-123",
                templateName = template.name,
                occurredAtEpochMilli = 1_700_000_000_000
            )
        )

        onNodeWithTag(TRACKING_QUICK_RECORD_RESULT_TAG).assertIsDisplayed()
        onNodeWithText(
            context.getString(R.string.tracking_record_session_id, "session-real-123")
        ).assertIsDisplayed()
    }

    private fun state(template: TrackingTemplateSummary) = TrackingQuickRecordUiState(
        isLoading = false,
        template = template,
        occurredAtEpochMilli = 1_700_000_000_000,
        fields = mapOf(
            "parts" to TrackingQuickRecordFieldState(
                TrackerInputValue.MultiSelect(emptySet())
            ),
            "duration" to TrackingQuickRecordFieldState(
                TrackerInputValue.Duration(null)
            ),
            "note" to TrackingQuickRecordFieldState(
                TrackerInputValue.Text("")
            )
        )
    )

    private fun template() = TrackingTemplateSummary(
        id = "fitness",
        name = "Fitness",
        description = "",
        icon = "",
        color = 0xFF6750A4,
        isPinned = false,
        displayOrder = 0,
        createdAtEpochMilli = 1,
        updatedAtEpochMilli = 1,
        lastRecordedAtEpochMilli = null,
        fields = listOf(
            TrackingFieldDraft(
                id = "parts",
                trackerId = "parts-tracker",
                tracker = TrackingTrackerDraft(
                    name = "Parts",
                    config = MultiSelectConfig(),
                    options = listOf(
                        TrackingOptionDraft(
                            id = "chest",
                            label = "Chest",
                            displayOrder = 0
                        )
                    )
                ),
                displayOrder = 0,
                required = true
            ),
            TrackingFieldDraft(
                id = "duration",
                trackerId = "duration-tracker",
                tracker = TrackingTrackerDraft(
                    name = "Duration",
                    config = DurationConfig()
                ),
                displayOrder = 1
            ),
            TrackingFieldDraft(
                id = "note",
                trackerId = "note-tracker",
                tracker = TrackingTrackerDraft(
                    name = "Note",
                    config = TextConfig()
                ),
                displayOrder = 2
            )
        )
    )
}
