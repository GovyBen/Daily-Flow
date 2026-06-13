package com.mhss.app.tracking.domain.serialization

import com.mhss.app.tracking.domain.model.ScaleConfig
import com.mhss.app.tracking.domain.model.TrackingFieldDraft
import com.mhss.app.tracking.domain.model.TrackingTemplateDraft
import com.mhss.app.tracking.domain.model.TrackingTrackerDraft
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingTemplateDraftJsonTest {

    @Test
    fun restoresEditorDraftIncludingHistoryFlag() {
        val draft = TrackingTemplateDraft(
            id = "template",
            name = "Mood",
            description = "Daily mood",
            icon = "M",
            color = 0xFF386A20,
            fields = listOf(
                TrackingFieldDraft(
                    id = "field",
                    trackerId = "tracker",
                    tracker = TrackingTrackerDraft(
                        name = "Score",
                        config = ScaleConfig(1.0, 10.0, 1.0),
                        unit = "points"
                    ),
                    displayOrder = 0,
                    required = true,
                    hasRecordedData = true
                )
            )
        )

        assertEquals(
            draft,
            TrackingTemplateDraftJson.decode(TrackingTemplateDraftJson.encode(draft))
        )
    }
}
