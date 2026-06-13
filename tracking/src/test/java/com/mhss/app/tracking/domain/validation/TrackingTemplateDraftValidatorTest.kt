package com.mhss.app.tracking.domain.validation

import com.mhss.app.tracking.domain.model.CounterConfig
import com.mhss.app.tracking.domain.model.MultiSelectConfig
import com.mhss.app.tracking.domain.model.TrackingFieldDraft
import com.mhss.app.tracking.domain.model.TrackingOptionDraft
import com.mhss.app.tracking.domain.model.TrackingTemplateDraft
import com.mhss.app.tracking.domain.model.TrackingTrackerDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingTemplateDraftValidatorTest {

    @Test
    fun validDraftHasNoErrors() {
        val errors = TrackingTemplateDraftValidator.validate(
            template(
                fields = listOf(
                    field(
                        config = MultiSelectConfig(maxSelections = 2),
                        options = listOf("Chest", "Back")
                    )
                )
            )
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun reportsRequiredNamesOptionsDuplicatesAndInvalidConfig() {
        val errors = TrackingTemplateDraftValidator.validate(
            template(
                name = " ",
                fields = listOf(
                    field(
                        name = "",
                        config = MultiSelectConfig(maxSelections = 3),
                        options = listOf("", "Same", "same")
                    ),
                    field(
                        name = "Count",
                        config = CounterConfig(minimum = -1, step = 0)
                    )
                )
            )
        )

        assertEquals(
            setOf(
                TrackingTemplateDraftErrorCode.TEMPLATE_NAME_REQUIRED,
                TrackingTemplateDraftErrorCode.FIELD_NAME_REQUIRED,
                TrackingTemplateDraftErrorCode.OPTION_LABEL_REQUIRED,
                TrackingTemplateDraftErrorCode.DUPLICATE_OPTION_LABEL,
                TrackingTemplateDraftErrorCode.INVALID_CONFIGURATION
            ),
            errors.map { it.code }.toSet()
        )
    }

    private fun template(
        name: String = "Fitness",
        fields: List<TrackingFieldDraft>
    ) = TrackingTemplateDraft(
        name = name,
        color = 1,
        fields = fields
    )

    private fun field(
        name: String = "Area",
        config: com.mhss.app.tracking.domain.model.TrackerConfig,
        options: List<String> = emptyList()
    ) = TrackingFieldDraft(
        tracker = TrackingTrackerDraft(
            name = name,
            config = config,
            options = options.mapIndexed { index, label ->
                TrackingOptionDraft(
                    id = "option-$index",
                    label = label,
                    displayOrder = index
                )
            }
        ),
        displayOrder = 0
    )
}
