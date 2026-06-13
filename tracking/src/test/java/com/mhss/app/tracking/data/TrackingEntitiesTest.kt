package com.mhss.app.tracking.data

import com.mhss.app.tracking.data.factory.TrackingEntityFactory
import com.mhss.app.tracking.data.order.TrackingEntityOrder
import com.mhss.app.tracking.data.serialization.TrackerConfigJson
import com.mhss.app.tracking.domain.id.TrackingIdGenerator
import com.mhss.app.tracking.domain.model.BooleanConfig
import com.mhss.app.tracking.domain.model.CounterConfig
import com.mhss.app.tracking.domain.model.DurationConfig
import com.mhss.app.tracking.domain.model.MultiSelectConfig
import com.mhss.app.tracking.domain.model.NumberConfig
import com.mhss.app.tracking.domain.model.RecordSource
import com.mhss.app.tracking.domain.model.ScaleConfig
import com.mhss.app.tracking.domain.model.SingleSelectConfig
import com.mhss.app.tracking.domain.model.TextConfig
import com.mhss.app.tracking.domain.model.TrackerConfig
import com.mhss.app.tracking.domain.model.TrackerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TrackingEntitiesTest {

    @Test
    fun everyTrackerConfigRoundTripsThroughJson() {
        val configs: List<TrackerConfig> = listOf(
            MultiSelectConfig(maxSelections = 3),
            SingleSelectConfig,
            CounterConfig(minimum = 0, maximum = 100, step = 2),
            ScaleConfig(minimum = 1.0, maximum = 5.0, step = 0.5),
            BooleanConfig(trueLabel = "Done", falseLabel = "Skipped"),
            DurationConfig(maximumSeconds = 86_400),
            NumberConfig(minimum = -10.0, maximum = 10.0, decimalPlaces = 2),
            TextConfig(maximumLength = 2_000, multiline = true)
        )

        configs.forEach { config ->
            assertEquals(config, TrackerConfigJson.decode(TrackerConfigJson.encode(config)))
        }
        assertEquals(TrackerType.entries, configs.map(TrackerConfig::trackerType))
    }

    @Test
    fun factoryUsesInjectedIdsAndMatchesConfigType() {
        val factory = TrackingEntityFactory(SequenceIdGenerator())
        val template = factory.createTemplate(
            name = "Health",
            color = 0xFF336699,
            nowEpochMilli = 1_000L
        )
        val tracker = factory.createTracker(
            name = "Mood",
            config = ScaleConfig(),
            nowEpochMilli = 1_000L
        )
        val field = factory.createTemplateField(
            templateId = template.id,
            trackerId = tracker.id,
            displayOrder = 0
        )

        assertEquals("id-1", template.id)
        assertEquals("id-2", tracker.id)
        assertEquals("id-3", field.id)
        assertEquals(TrackerType.SCALE, tracker.type)
        assertEquals(template.id, field.templateId)
        assertEquals(tracker.id, field.trackerId)
    }

    @Test
    fun displayOrderingHasDeterministicTieBreakers() {
        val factory = TrackingEntityFactory(SequenceIdGenerator())
        val laterId = factory.createTemplate(
            name = "Later ID",
            color = 1,
            nowEpochMilli = 10,
            displayOrder = 0
        )
        val nextOrder = factory.createTemplate(
            name = "Next",
            color = 1,
            nowEpochMilli = 5,
            displayOrder = 1
        )
        val earlierId = laterId.copy(id = "id-0", name = "Earlier ID")

        val sorted = TrackingEntityOrder.templates(
            listOf(nextOrder, laterId, earlierId)
        )

        assertEquals(listOf("id-0", "id-1", "id-2"), sorted.map { it.id })
    }

    @Test
    fun deactivationPreservesIdentityAndReferences() {
        val factory = TrackingEntityFactory(SequenceIdGenerator())
        val template = factory.createTemplate("Health", 1, 1)
        val tracker = factory.createTracker("Mood", ScaleConfig(), 1)
        val field = factory.createTemplateField(template.id, tracker.id, 0)
        val option = factory.createTrackerOption(tracker.id, "Good", 0)

        val inactiveTemplate = template.copy(isActive = false)
        val inactiveTracker = tracker.copy(isActive = false)
        val inactiveOption = option.copy(isActive = false)

        assertFalse(inactiveTemplate.isActive)
        assertFalse(inactiveTracker.isActive)
        assertFalse(inactiveOption.isActive)
        assertEquals(template.id, field.templateId)
        assertEquals(tracker.id, field.trackerId)
        assertEquals(tracker.id, inactiveOption.trackerId)
    }

    @Test
    fun factoryCreatesSessionAndDistinctPointsForTheSameMoment() {
        val factory = TrackingEntityFactory(SequenceIdGenerator())
        val session = factory.createRecordSession(
            templateId = "template",
            occurredAtEpochMilli = 5_000,
            zoneId = "Asia/Shanghai",
            nowEpochMilli = 6_000,
            source = RecordSource.AI
        )
        val first = factory.createDataPoint(
            sessionId = session.id,
            trackerId = "tracker",
            epochMilli = session.occurredAtEpochMilli,
            utcOffsetSeconds = 28_800,
            value = 1.0,
            label = "Chest",
            optionId = "option-1",
            nowEpochMilli = 6_000
        )
        val second = factory.createDataPoint(
            sessionId = session.id,
            trackerId = "tracker",
            epochMilli = session.occurredAtEpochMilli,
            utcOffsetSeconds = 28_800,
            value = 1.0,
            label = "Back",
            optionId = "option-2",
            nowEpochMilli = 6_000
        )

        assertEquals(RecordSource.AI, session.source)
        assertEquals(session.id, first.sessionId)
        assertEquals(first.epochMilli, second.epochMilli)
        assertFalse(first.id == second.id)
    }
}

private class SequenceIdGenerator : TrackingIdGenerator {
    private var next = 1

    override fun newId(): String = "id-${next++}"
}
