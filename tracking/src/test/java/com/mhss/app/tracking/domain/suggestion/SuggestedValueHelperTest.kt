package com.mhss.app.tracking.domain.suggestion

import com.mhss.app.tracking.domain.model.BooleanConfig
import com.mhss.app.tracking.domain.model.CounterConfig
import com.mhss.app.tracking.domain.model.DurationConfig
import com.mhss.app.tracking.domain.model.MultiSelectConfig
import com.mhss.app.tracking.domain.model.NumberConfig
import com.mhss.app.tracking.domain.model.ScaleConfig
import com.mhss.app.tracking.domain.model.SingleSelectConfig
import com.mhss.app.tracking.domain.model.TextConfig
import com.mhss.app.tracking.domain.model.TrackerConfig
import com.mhss.app.tracking.domain.model.TrackingFieldDraft
import com.mhss.app.tracking.domain.model.TrackingOptionDraft
import com.mhss.app.tracking.domain.model.TrackingSuggestedValue
import com.mhss.app.tracking.domain.model.TrackingTrackerDraft
import com.mhss.app.tracking.domain.serialization.TrackerInputValueJson
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SuggestedValueHelperTest {

    @Test
    fun buildsDefaultRecentAndFrequentGroupsWithStableOrdering() {
        val field = field(
            config = NumberConfig(),
            defaultValue = TrackerInputValue.NumberValue(2.0)
        )
        val history = listOf(
            value(number = 3.0, usedAt = 200, count = 2),
            value(number = 1.0, usedAt = 100, count = 5),
            value(number = 4.0, usedAt = 100, count = 5),
            value(number = 3.0, usedAt = 150, count = 1)
        )

        val suggestions = buildSuggestions(field, history, limit = 5)

        assertEquals(TrackerInputValue.NumberValue(2.0), suggestions.defaultValue)
        assertEquals(
            listOf(3.0, 1.0, 4.0),
            suggestions.recent.map { (it.input as TrackerInputValue.NumberValue).value }
        )
        assertEquals(
            listOf(1.0, 4.0, 3.0),
            suggestions.frequent.map { (it.input as TrackerInputValue.NumberValue).value }
        )
        assertEquals(listOf(3, 5, 5), suggestions.recent.map { it.usageCount })
    }

    @Test
    fun configuredDefaultIsNotRepeatedInHistoryGroups() {
        val field = field(
            config = NumberConfig(),
            defaultValue = TrackerInputValue.NumberValue(2.0)
        )

        val suggestions = buildSuggestions(
            field,
            history = listOf(
                value(number = 2.0, usedAt = 200, count = 10),
                value(number = 3.0, usedAt = 100, count = 1)
            )
        )

        assertEquals(
            listOf(3.0),
            suggestions.recent.map { (it.input as TrackerInputValue.NumberValue).value }
        )
    }

    @Test
    fun textHistoryIsPrivateByDefaultAndCappedWhenEnabled() {
        val field = field(config = TextConfig())
        val history = (1L..6L).map { index ->
            TrackingSuggestedValue(
                value = null,
                label = null,
                note = "note-$index",
                optionId = null,
                lastUsedAtEpochMilli = index,
                usageCount = index.toInt()
            )
        }

        val hidden = buildSuggestions(field, history, limit = 5)
        val visible = buildSuggestions(
            field,
            history,
            limit = 5,
            includeTextHistory = true
        )

        assertEquals(emptyList<TrackingInputSuggestion>(), hidden.recent)
        assertEquals(emptyList<TrackingInputSuggestion>(), hidden.frequent)
        assertEquals(3, visible.recent.size)
        assertEquals(
            listOf("note-6", "note-5", "note-4"),
            visible.recent.map { (it.input as TrackerInputValue.Text).value }
        )
    }

    @Test
    fun counterIncrementUsesCurrentThenDefaultThenRecentValue() {
        val field = field(config = CounterConfig(minimum = 1, maximum = 10, step = 2))
        val history = listOf(value(number = 5.0, usedAt = 100))

        val fromCurrent = buildSuggestions(
            field,
            history,
            currentInput = TrackerInputValue.Counter(3.0)
        )
        val fromRecent = buildSuggestions(field, history)

        assertEquals(TrackerInputValue.Counter(5.0), fromCurrent.counterIncrement)
        assertEquals(TrackerInputValue.Counter(7.0), fromRecent.counterIncrement)
    }

    @Test
    fun counterIncrementStopsAtMaximumAndRejectsInvalidDefault() {
        val field = field(
            config = CounterConfig(minimum = 0, maximum = 10, step = 2),
            defaultValue = TrackerInputValue.Counter(3.0)
        )

        val suggestions = buildSuggestions(
            field,
            history = emptyList(),
            currentInput = TrackerInputValue.Counter(10.0)
        )

        assertNull(suggestions.defaultValue)
        assertNull(suggestions.counterIncrement)
    }

    @Test
    fun malformedDefaultIsIgnored() {
        val field = field(config = NumberConfig()).copy(defaultValueJson = "{not-json")

        assertNull(buildSuggestions(field, emptyList()).defaultValue)
    }

    @Test
    fun historyMapsToAllEightSealedInputTypes() {
        val option = TrackingOptionDraft(id = "a", label = "A", displayOrder = 0)
        val cases = listOf(
            HistoryMappingCase(
                field(MultiSelectConfig(), options = listOf(option)),
                historical(optionId = "a"),
                TrackerInputValue.MultiSelect(setOf("a"))
            ),
            HistoryMappingCase(
                field(SingleSelectConfig, options = listOf(option)),
                historical(optionId = "a"),
                TrackerInputValue.SingleSelect("a")
            ),
            HistoryMappingCase(
                field(CounterConfig()),
                historical(number = 2.0),
                TrackerInputValue.Counter(2.0)
            ),
            HistoryMappingCase(
                field(ScaleConfig()),
                historical(number = 3.0),
                TrackerInputValue.Scale(3.0)
            ),
            HistoryMappingCase(
                field(BooleanConfig()),
                historical(number = 1.0),
                TrackerInputValue.BooleanValue(true)
            ),
            HistoryMappingCase(
                field(DurationConfig()),
                historical(number = 90.0),
                TrackerInputValue.Duration(90)
            ),
            HistoryMappingCase(
                field(NumberConfig()),
                historical(number = 2.5),
                TrackerInputValue.NumberValue(2.5)
            ),
            HistoryMappingCase(
                field(TextConfig()),
                historical(note = "private"),
                TrackerInputValue.Text("private"),
                includeTextHistory = true
            )
        )

        cases.forEach { case ->
            val result = buildSuggestions(
                field = case.field,
                history = listOf(case.history),
                includeTextHistory = case.includeTextHistory
            )
            assertEquals(case.expected, result.recent.single().input)
        }
    }

    private fun field(
        config: TrackerConfig,
        defaultValue: TrackerInputValue? = null,
        options: List<TrackingOptionDraft> = emptyList()
    ) = TrackingFieldDraft(
        trackerId = "tracker",
        tracker = TrackingTrackerDraft(
            name = "Field",
            config = config,
            options = options
        ),
        displayOrder = 0,
        defaultValueJson = defaultValue?.let(TrackerInputValueJson::encode)
    )

    private fun value(
        number: Double,
        usedAt: Long,
        count: Int = 1
    ) = TrackingSuggestedValue(
        value = number,
        label = null,
        note = null,
        optionId = null,
        lastUsedAtEpochMilli = usedAt,
        usageCount = count
    )

    private fun historical(
        number: Double? = null,
        note: String? = null,
        optionId: String? = null
    ) = TrackingSuggestedValue(
        value = number,
        label = null,
        note = note,
        optionId = optionId,
        lastUsedAtEpochMilli = 100
    )
}

private data class HistoryMappingCase(
    val field: TrackingFieldDraft,
    val history: TrackingSuggestedValue,
    val expected: TrackerInputValue,
    val includeTextHistory: Boolean = false
)
