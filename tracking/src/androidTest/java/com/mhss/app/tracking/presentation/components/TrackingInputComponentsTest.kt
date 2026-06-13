package com.mhss.app.tracking.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.mhss.app.ui.R as UiR
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TrackingInputComponentsTest {

    @Test
    fun durationInputEmitsSecondsClearsAndRestoresState() = runComposeUiTest {
        var latestValue: Long? = null
        val restorationTester = StateRestorationTester(this)
        restorationTester.setContent {
            var value by rememberSaveable { mutableStateOf<Long?>(null) }
            MaterialTheme {
                DurationInput(
                    totalSeconds = value,
                    onTotalSecondsChange = {
                        value = it
                        latestValue = it
                    }
                )
            }
        }

        onNodeWithTag(DURATION_HOURS_TAG).performTextReplacement("1")
        onNodeWithTag(DURATION_MINUTES_TAG).performTextReplacement("2")
        onNodeWithTag(DURATION_SECONDS_TAG).performTextReplacement("3")
        assertEquals(3_723L, latestValue)

        restorationTester.emulateSaveAndRestore()
        onNodeWithTag(DURATION_HOURS_TAG).assertTextContains("1")
        onNodeWithTag(DURATION_MINUTES_TAG).assertTextContains("2")
        onNodeWithTag(DURATION_SECONDS_TAG).assertTextContains("3")

        onNodeWithTag(DURATION_CLEAR_TAG).performClick()
        assertEquals(null, latestValue)
    }

    @Test
    fun dateAndTimeDialogsConfirmTheCurrentSelection() = runComposeUiTest {
        val initial = 1_768_473_000_000L
        var selectionCount = 0
        setContent {
            MaterialTheme {
                DateTimeSelectorButtons(
                    selectedEpochMilli = initial,
                    onDateTimeSelected = { selectionCount++ }
                )
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val okText = context.getString(UiR.string.ok)

        onNodeWithTag(TRACKING_DATE_BUTTON_TAG).performClick()
        onNodeWithText(okText).assertIsDisplayed().performClick()
        assertEquals(1, selectionCount)

        onNodeWithTag(TRACKING_TIME_BUTTON_TAG).performClick()
        onNodeWithText(okText).assertIsDisplayed().performClick()
        assertEquals(2, selectionCount)
    }
}
