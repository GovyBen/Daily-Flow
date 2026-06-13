package com.mhss.app.tracking.data.mapping

import com.mhss.app.tracking.data.database.entity.TrackerOptionEntity
import com.mhss.app.tracking.data.factory.TrackingEntityFactory
import com.mhss.app.tracking.domain.id.TrackingIdGenerator
import com.mhss.app.tracking.domain.model.BooleanConfig
import com.mhss.app.tracking.domain.model.CounterConfig
import com.mhss.app.tracking.domain.model.DurationConfig
import com.mhss.app.tracking.domain.model.MultiSelectConfig
import com.mhss.app.tracking.domain.model.NumberConfig
import com.mhss.app.tracking.domain.model.ScaleConfig
import com.mhss.app.tracking.domain.model.SingleSelectConfig
import com.mhss.app.tracking.domain.model.TextConfig
import com.mhss.app.tracking.domain.model.TrackerConfig
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import com.mhss.app.tracking.domain.validation.TrackerValueError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackerValueMapperTest {

    private val mapper = TrackerValueMapper(
        TrackingEntityFactory(SequenceIdGenerator())
    )

    @Test
    fun multiSelectCreatesOrderedPointsWithOptionSnapshots() {
        val options = listOf(
            option("back", "Back", displayOrder = 2),
            option("chest", "Chest", numericValue = 4.0, displayOrder = 1)
        )

        val points = mapper.map(
            request(
                config = MultiSelectConfig(),
                input = TrackerInputValue.MultiSelect(setOf("back", "chest")),
                options = options
            )
        )

        assertEquals(listOf("chest", "back"), points.map { it.optionId })
        assertEquals(listOf("Chest", "Back"), points.map { it.label })
        assertEquals(listOf(4.0, 1.0), points.map { it.value })
        assertEquals(2, points.map { it.id }.distinct().size)
    }

    @Test
    fun singleSelectSnapshotsLabelAndNumericValue() {
        val point = mapper.map(
            request(
                config = SingleSelectConfig,
                input = TrackerInputValue.SingleSelect("good"),
                options = listOf(option("good", "Good", numericValue = 8.0))
            )
        ).single()

        assertEquals("good", point.optionId)
        assertEquals("Good", point.label)
        assertEquals(8.0, point.value)
    }

    @Test
    fun numericInputsUseTheSharedNumericColumn() {
        val cases = listOf(
            CounterConfig() to TrackerInputValue.Counter(3.0),
            ScaleConfig() to TrackerInputValue.Scale(7.0),
            BooleanConfig() to TrackerInputValue.BooleanValue(true),
            BooleanConfig() to TrackerInputValue.BooleanValue(false),
            DurationConfig() to TrackerInputValue.Duration(90),
            NumberConfig() to TrackerInputValue.NumberValue(2.5)
        )

        val values = cases.map { (config, input) ->
            mapper.map(request(config, input)).single().value
        }

        assertEquals(listOf(3.0, 7.0, 1.0, 0.0, 90.0, 2.5), values)
    }

    @Test
    fun textUsesNoteWithoutInventingNumericValue() {
        val point = mapper.map(
            request(
                config = TextConfig(),
                input = TrackerInputValue.Text("Private note")
            )
        ).single()

        assertEquals("Private note", point.note)
        assertNull(point.value)
        assertNull(point.label)
        assertNull(point.optionId)
    }

    @Test
    fun optionalEmptyInputProducesNoPoint() {
        val points = mapper.map(
            request(
                config = NumberConfig(),
                input = TrackerInputValue.NumberValue(null),
                required = false
            )
        )

        assertTrue(points.isEmpty())
    }

    @Test
    fun invalidInputReturnsValidatorErrors() {
        val error = runCatching {
            mapper.map(
                request(
                    config = SingleSelectConfig,
                    input = TrackerInputValue.SingleSelect("inactive"),
                    options = listOf(option("inactive", "Inactive", isActive = false))
                )
            )
        }.exceptionOrNull() as TrackerValueMappingException

        assertEquals(listOf(TrackerValueError.INACTIVE_OPTION), error.errors)
    }

    private fun request(
        config: TrackerConfig,
        input: TrackerInputValue,
        required: Boolean = true,
        options: List<TrackerOptionEntity> = emptyList()
    ) = TrackerValueMappingRequest(
        sessionId = "session",
        trackerId = "tracker",
        config = config,
        input = input,
        required = required,
        options = options,
        epochMilli = 1_000,
        utcOffsetSeconds = 28_800,
        nowEpochMilli = 2_000
    )

    private fun option(
        id: String,
        label: String,
        numericValue: Double? = null,
        displayOrder: Int = 0,
        isActive: Boolean = true
    ) = TrackerOptionEntity(
        id = id,
        trackerId = "tracker",
        label = label,
        numericValue = numericValue,
        displayOrder = displayOrder,
        isActive = isActive
    )
}

private class SequenceIdGenerator : TrackingIdGenerator {
    private var next = 1

    override fun newId(): String = "point-${next++}"
}
