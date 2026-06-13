package com.mhss.app.tracking.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.platform.app.InstrumentationRegistry
import com.mhss.app.tracking.R
import com.mhss.app.tracking.domain.model.BooleanConfig
import com.mhss.app.tracking.domain.model.CounterConfig
import com.mhss.app.tracking.domain.model.DurationConfig
import com.mhss.app.tracking.domain.model.MultiSelectConfig
import com.mhss.app.tracking.domain.model.NumberConfig
import com.mhss.app.tracking.domain.model.ScaleConfig
import com.mhss.app.tracking.domain.model.SingleSelectConfig
import com.mhss.app.tracking.domain.model.TextConfig
import com.mhss.app.tracking.domain.model.TrackingFieldDraft
import com.mhss.app.tracking.domain.model.TrackingOptionDraft
import com.mhss.app.tracking.domain.model.TrackingTrackerDraft
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TrackingFieldInputTest {

    @Test
    fun optionAndBooleanControlsEmitSealedInputs() = runComposeUiTest {
        var multiLatest: TrackerInputValue? = null
        var singleLatest: TrackerInputValue? = null
        var booleanLatest: TrackerInputValue? = null
        setContent {
            MaterialTheme {
                Column {
                    StatefulField(
                        field = field(
                            name = "Activities",
                            config = MultiSelectConfig(),
                            options = options("walk", "read")
                        ),
                        initial = TrackerInputValue.MultiSelect(emptySet()),
                        onLatest = { multiLatest = it }
                    )
                    StatefulField(
                        field = field(
                            name = "Mood",
                            config = SingleSelectConfig,
                            options = options("calm", "happy")
                        ),
                        initial = TrackerInputValue.SingleSelect(null),
                        onLatest = { singleLatest = it }
                    )
                    StatefulField(
                        field = field(name = "Done", config = BooleanConfig()),
                        initial = TrackerInputValue.BooleanValue(null),
                        onLatest = { booleanLatest = it }
                    )
                }
            }
        }

        onNodeWithTag(trackingOptionTag("walk")).performClick()
        onNodeWithTag(trackingOptionTag("happy")).performClick()
        onNodeWithTag(TRACKING_BOOLEAN_TRUE_TAG).performClick()

        assertEquals(setOf("walk"), (multiLatest as TrackerInputValue.MultiSelect).optionIds)
        assertEquals("happy", (singleLatest as TrackerInputValue.SingleSelect).optionId)
        assertEquals(true, (booleanLatest as TrackerInputValue.BooleanValue).value)
    }

    @Test
    fun counterScaleAndNumberControlsEmitSealedInputs() = runComposeUiTest {
        var counterLatest: TrackerInputValue? = null
        var scaleLatest: TrackerInputValue? = null
        var numberLatest: TrackerInputValue? = null
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    StatefulField(
                        field = field(
                            name = "Repetitions",
                            config = CounterConfig(minimum = 1, maximum = 10)
                        ),
                        initial = TrackerInputValue.Counter(null),
                        onLatest = { counterLatest = it }
                    )
                    StatefulField(
                        field = field(
                            name = "Energy",
                            config = ScaleConfig(minimum = 0.0, maximum = 10.0)
                        ),
                        initial = TrackerInputValue.Scale(null),
                        onLatest = { scaleLatest = it }
                    )
                    StatefulField(
                        field = field(name = "Weight", config = NumberConfig()),
                        initial = TrackerInputValue.NumberValue(null),
                        onLatest = { numberLatest = it }
                    )
                }
            }
        }

        onNodeWithTag(TRACKING_COUNTER_INCREMENT_TAG).performClick()
        onNodeWithTag(TRACKING_SCALE_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { action -> action(7f) }
        onNodeWithTag(TRACKING_NUMBER_TAG).performTextReplacement("72.5")

        assertEquals(1.0, (counterLatest as TrackerInputValue.Counter).value)
        assertEquals(7.0, (scaleLatest as TrackerInputValue.Scale).value!!, 0.01)
        assertEquals(72.5, (numberLatest as TrackerInputValue.NumberValue).value)
    }

    @Test
    fun durationAndTextControlsEmitSealedInputs() = runComposeUiTest {
        var durationLatest: TrackerInputValue? = null
        var textLatest: TrackerInputValue? = null
        setContent {
            MaterialTheme {
                Column {
                    StatefulField(
                        field = field(name = "Duration", config = DurationConfig()),
                        initial = TrackerInputValue.Duration(null),
                        onLatest = { durationLatest = it }
                    )
                    StatefulField(
                        field = field(name = "Notes", config = TextConfig()),
                        initial = TrackerInputValue.Text(""),
                        onLatest = { textLatest = it }
                    )
                }
            }
        }

        onNodeWithTag(DURATION_MINUTES_TAG).performTextReplacement("12")
        onNodeWithTag(TRACKING_TEXT_TAG).performTextReplacement("steady")

        assertEquals(720L, (durationLatest as TrackerInputValue.Duration).seconds)
        assertEquals("steady", (textLatest as TrackerInputValue.Text).value)
    }

    @Test
    fun validationErrorsComeFromUnifiedValidator() = runComposeUiTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val requiredMessage = context.getString(R.string.tracking_error_required)
        val tooLongMessage = context.getString(R.string.tracking_error_text_too_long)
        setContent {
            MaterialTheme {
                StatefulField(
                    field = field(
                        name = "Summary",
                        config = TextConfig(maximumLength = 3),
                        required = true
                    ),
                    initial = TrackerInputValue.Text("")
                )
            }
        }

        onNodeWithText(requiredMessage).assertIsDisplayed()
        onNodeWithTag(TRACKING_TEXT_TAG).performTextReplacement("abcd")
        onNodeWithText(requiredMessage).assertDoesNotExist()
        onNodeWithText(tooLongMessage).assertIsDisplayed()
    }

    @Test
    fun typeMismatchIsReportedInsteadOfCrashing() = runComposeUiTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mismatchMessage = context.getString(R.string.tracking_error_type_mismatch)
        setContent {
            MaterialTheme {
                TrackingFieldInput(
                    field = field(name = "Notes", config = TextConfig()),
                    input = TrackerInputValue.NumberValue(1.0),
                    onInputChange = {}
                )
            }
        }

        onNodeWithText(mismatchMessage).assertIsDisplayed()
    }

    @androidx.compose.runtime.Composable
    private fun StatefulField(
        field: TrackingFieldDraft,
        initial: TrackerInputValue,
        onLatest: (TrackerInputValue) -> Unit = {}
    ) {
        var input by remember { mutableStateOf(initial) }
        TrackingFieldInput(
            field = field,
            input = input,
            onInputChange = {
                input = it
                onLatest(it)
            }
        )
    }

    private fun field(
        name: String,
        config: com.mhss.app.tracking.domain.model.TrackerConfig,
        options: List<TrackingOptionDraft> = emptyList(),
        required: Boolean = false
    ) = TrackingFieldDraft(
        tracker = TrackingTrackerDraft(
            name = name,
            config = config,
            options = options
        ),
        displayOrder = 0,
        required = required
    )

    private fun options(vararg ids: String) = ids.mapIndexed { index, id ->
        TrackingOptionDraft(
            id = id,
            label = id,
            displayOrder = index
        )
    }
}
