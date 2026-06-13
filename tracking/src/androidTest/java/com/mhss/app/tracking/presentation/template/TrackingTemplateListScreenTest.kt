package com.mhss.app.tracking.presentation.template

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.platform.app.InstrumentationRegistry
import com.mhss.app.tracking.R
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TrackingTemplateListScreenTest {

    @Test
    fun createDialogRestoresNameAndEmitsCreate() = runComposeUiTest {
        var createdName: String? = null
        val restorationTester = StateRestorationTester(this)
        restorationTester.setContent {
            MaterialTheme {
                TrackingTemplateListContent(
                    state = TrackingTemplateListUiState(),
                    onCreate = { createdName = it }
                )
            }
        }

        onNodeWithTag(TRACKING_TEMPLATE_CREATE_TAG).performClick()
        onNodeWithTag(TRACKING_TEMPLATE_NAME_TAG).performTextReplacement("Morning")
        restorationTester.emulateSaveAndRestore()
        onNodeWithTag(TRACKING_TEMPLATE_NAME_TAG).assertTextContains("Morning")
        onNodeWithTag(TRACKING_TEMPLATE_CREATE_CONFIRM_TAG).performClick()

        assertEquals("Morning", createdName)
    }

    @Test
    fun createCallbackBypassesTheQuickCreateDialog() = runComposeUiTest {
        var createRequested = false
        setContent {
            MaterialTheme {
                TrackingTemplateListContent(
                    state = TrackingTemplateListUiState(),
                    onCreateTemplate = { createRequested = true }
                )
            }
        }

        onNodeWithTag(TRACKING_TEMPLATE_CREATE_TAG).performClick()

        assertEquals(true, createRequested)
    }

    @Test
    fun templateMenuEmitsPinAndDuplicateActions() = runComposeUiTest {
        val template = template(id = "health", name = "Health")
        var pinnedId: String? = null
        var duplicatedId: String? = null
        var editedId: String? = null
        setContent {
            MaterialTheme {
                TrackingTemplateListContent(
                    state = TrackingTemplateListUiState(listOf(template)),
                    onPin = { pinnedId = it.id },
                    onDuplicate = { duplicatedId = it },
                    onEditTemplate = { editedId = it }
                )
            }
        }

        onNodeWithTag(trackingTemplateCardTag("health")).assertIsDisplayed()
        onNodeWithTag(trackingTemplateMenuTag("health")).performClick()
        onNodeWithTag(trackingTemplatePinTag("health")).performClick()
        onNodeWithTag(trackingTemplateMenuTag("health")).performClick()
        onNodeWithTag(trackingTemplateDuplicateTag("health")).performClick()
        onNodeWithTag(trackingTemplateMenuTag("health")).performClick()
        onNodeWithTag(trackingTemplateEditTag("health")).performClick()

        assertEquals("health", pinnedId)
        assertEquals("health", duplicatedId)
        assertEquals("health", editedId)
    }

    @Test
    fun deactivateRequiresConfirmation() = runComposeUiTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val template = template(id = "temporary", name = "Temporary")
        var deactivatedId: String? = null
        setContent {
            MaterialTheme {
                TrackingTemplateListContent(
                    state = TrackingTemplateListUiState(listOf(template)),
                    onDeactivate = { deactivatedId = it }
                )
            }
        }

        onNodeWithTag(trackingTemplateMenuTag("temporary")).performClick()
        onNodeWithTag(trackingTemplateDeactivateTag("temporary")).performClick()
        onNodeWithText(
            context.getString(R.string.tracking_deactivate_template_title)
        ).assertIsDisplayed()
        onNodeWithText(context.getString(R.string.tracking_deactivate)).performClick()

        assertEquals("temporary", deactivatedId)
    }

    private fun template(
        id: String,
        name: String,
        isPinned: Boolean = false
    ) = TrackingTemplateSummary(
        id = id,
        name = name,
        description = "",
        icon = "",
        color = 0xFF4F6BED,
        isPinned = isPinned,
        displayOrder = 0,
        createdAtEpochMilli = 1_000,
        updatedAtEpochMilli = 1_000,
        lastRecordedAtEpochMilli = null,
        fields = emptyList()
    )
}
