package com.mhss.app.widget.tracking

import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingWidgetModelsTest {

    @Test
    fun selectorOnlyReturnsFirstFourPinnedTemplatesInDisplayOrder() {
        val templates = listOf(
            template("inactive-looking", pinned = false, order = 0),
            template("fifth", pinned = true, order = 5),
            template("third", pinned = true, order = 3),
            template("first", pinned = true, order = 1),
            template("fourth", pinned = true, order = 4),
            template("second", pinned = true, order = 2)
        )

        val selected = selectTrackingWidgetTemplates(templates)

        assertEquals(
            listOf("first", "second", "third", "fourth"),
            selected.map(TrackingTemplateSummary::id)
        )
    }

    private fun template(
        id: String,
        pinned: Boolean,
        order: Int
    ) = TrackingTemplateSummary(
        id = id,
        name = id,
        description = "",
        icon = "",
        color = 0,
        isPinned = pinned,
        displayOrder = order,
        createdAtEpochMilli = 1,
        updatedAtEpochMilli = 1,
        lastRecordedAtEpochMilli = null,
        fields = emptyList()
    )
}
