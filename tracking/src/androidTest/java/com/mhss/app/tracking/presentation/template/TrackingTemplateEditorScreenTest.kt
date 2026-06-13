package com.mhss.app.tracking.presentation.template

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.platform.app.InstrumentationRegistry
import com.mhss.app.tracking.R
import com.mhss.app.tracking.domain.model.TextConfig
import com.mhss.app.tracking.domain.model.TrackingFieldDraft
import com.mhss.app.tracking.domain.model.TrackingTemplateDraft
import com.mhss.app.tracking.domain.model.TrackingTrackerDraft
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TrackingTemplateEditorScreenTest {

    @Test
    fun restoresDraftNameAndSavesEditedValue() = runComposeUiTest {
        var saved: TrackingTemplateDraft? = null
        val restorationTester = StateRestorationTester(this)
        restorationTester.setContent {
            MaterialTheme {
                TrackingTemplateEditorContent(
                    sourceDraft = template(),
                    onSave = { saved = it }
                )
            }
        }

        onNodeWithTag(TRACKING_TEMPLATE_EDITOR_NAME_TAG)
            .performTextReplacement("Evening")
        restorationTester.emulateSaveAndRestore()
        onNodeWithTag(TRACKING_TEMPLATE_EDITOR_SAVE_TAG).performClick()

        assertEquals("Evening", saved?.name)
    }

    @Test
    fun addsNamesAndRemovesAFieldWithConfirmation() = runComposeUiTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        setContent {
            MaterialTheme {
                TrackingTemplateEditorContent(sourceDraft = template(fields = emptyList()))
            }
        }

        onNodeWithTag(TRACKING_TEMPLATE_EDITOR_ADD_FIELD_TAG).performClick()
        onNodeWithTag(trackingTemplateEditorFieldNameTag(0))
            .performScrollTo()
            .performTextReplacement("Notes")
        onNodeWithTag(trackingTemplateEditorDeleteFieldTag(0)).performClick()
        onNodeWithText(context.getString(R.string.tracking_remove_field_title))
            .assertIsDisplayed()
        onNodeWithText(context.getString(R.string.tracking_remove)).performClick()
        onNodeWithTag(TRACKING_TEMPLATE_EDITOR_ADD_FIELD_TAG).assertIsDisplayed()
    }

    @Test
    fun recordedFieldDisablesTypeChanges() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TrackingTemplateEditorContent(
                    sourceDraft = template(
                        fields = listOf(
                            field(hasRecordedData = true)
                        )
                    )
                )
            }
        }

        onNodeWithTag(trackingTemplateEditorFieldTypeTag(0))
            .performScrollTo()
            .assertIsNotEnabled()
    }

    private fun template(
        fields: List<TrackingFieldDraft> = listOf(field())
    ) = TrackingTemplateDraft(
        id = "template",
        name = "Morning",
        color = 0xFF4F6BED,
        fields = fields
    )

    private fun field(
        hasRecordedData: Boolean = false
    ) = TrackingFieldDraft(
        id = "field",
        trackerId = "tracker",
        tracker = TrackingTrackerDraft(
            name = "Note",
            config = TextConfig()
        ),
        displayOrder = 0,
        hasRecordedData = hasRecordedData
    )
}
