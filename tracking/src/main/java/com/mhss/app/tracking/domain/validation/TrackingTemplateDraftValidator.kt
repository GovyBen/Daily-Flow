package com.mhss.app.tracking.domain.validation

import com.mhss.app.tracking.domain.model.BooleanConfig
import com.mhss.app.tracking.domain.model.CounterConfig
import com.mhss.app.tracking.domain.model.DurationConfig
import com.mhss.app.tracking.domain.model.MultiSelectConfig
import com.mhss.app.tracking.domain.model.NumberConfig
import com.mhss.app.tracking.domain.model.ScaleConfig
import com.mhss.app.tracking.domain.model.SingleSelectConfig
import com.mhss.app.tracking.domain.model.TextConfig
import com.mhss.app.tracking.domain.model.TrackingTemplateDraft

object TrackingTemplateDraftValidator {

    fun validate(draft: TrackingTemplateDraft): List<TrackingTemplateDraftError> = buildList {
        if (draft.name.isBlank()) {
            add(TrackingTemplateDraftError(TrackingTemplateDraftErrorCode.TEMPLATE_NAME_REQUIRED))
        }

        draft.fields.forEachIndexed { fieldIndex, field ->
            if (field.tracker.name.isBlank()) {
                add(
                    TrackingTemplateDraftError(
                        code = TrackingTemplateDraftErrorCode.FIELD_NAME_REQUIRED,
                        fieldIndex = fieldIndex
                    )
                )
            }

            val activeOptions = field.tracker.options.filter { it.isActive }
            if (
                field.tracker.config is MultiSelectConfig ||
                field.tracker.config is SingleSelectConfig
            ) {
                if (activeOptions.isEmpty()) {
                    add(
                        TrackingTemplateDraftError(
                            code = TrackingTemplateDraftErrorCode.SELECT_OPTIONS_REQUIRED,
                            fieldIndex = fieldIndex
                        )
                    )
                }
                activeOptions.forEachIndexed { optionIndex, option ->
                    if (option.label.isBlank()) {
                        add(
                            TrackingTemplateDraftError(
                                code = TrackingTemplateDraftErrorCode.OPTION_LABEL_REQUIRED,
                                fieldIndex = fieldIndex,
                                optionIndex = optionIndex
                            )
                        )
                    }
                }
                val normalizedLabels = activeOptions
                    .map { it.label.trim().lowercase() }
                    .filter { it.isNotEmpty() }
                if (normalizedLabels.distinct().size != normalizedLabels.size) {
                    add(
                        TrackingTemplateDraftError(
                            code = TrackingTemplateDraftErrorCode.DUPLICATE_OPTION_LABEL,
                            fieldIndex = fieldIndex
                        )
                    )
                }
            }

            if (!field.tracker.config.isValid(activeOptions.size)) {
                add(
                    TrackingTemplateDraftError(
                        code = TrackingTemplateDraftErrorCode.INVALID_CONFIGURATION,
                        fieldIndex = fieldIndex
                    )
                )
            }
        }
    }

    fun requireValid(draft: TrackingTemplateDraft) {
        val errors = validate(draft)
        require(errors.isEmpty()) {
            "Invalid tracking template: ${errors.joinToString { it.code.name }}"
        }
    }

    private fun Any.isValid(activeOptionCount: Int): Boolean = when (this) {
        is MultiSelectConfig ->
            maxSelections == null || maxSelections in 1..activeOptionCount

        SingleSelectConfig -> true

        is CounterConfig ->
            minimum >= 0 && step > 0 && maximum?.let { it >= minimum } != false

        is ScaleConfig ->
            minimum.isFinite() &&
                maximum.isFinite() &&
                step.isFinite() &&
                maximum > minimum &&
                step > 0

        is BooleanConfig -> trueLabel.isNotBlank() && falseLabel.isNotBlank()

        is DurationConfig ->
            maximumSeconds == null ||
                maximumSeconds in 0..TrackerValueValidator.MAX_REASONABLE_DURATION_SECONDS

        is NumberConfig ->
            minimum?.isFinite() != false &&
                maximum?.isFinite() != false &&
                step?.isFinite() != false &&
                (minimum == null || maximum == null || maximum >= minimum) &&
                (step == null || step > 0) &&
                (decimalPlaces == null || decimalPlaces in 0..15)

        is TextConfig -> maximumLength == null || maximumLength > 0
        else -> false
    }
}

data class TrackingTemplateDraftError(
    val code: TrackingTemplateDraftErrorCode,
    val fieldIndex: Int? = null,
    val optionIndex: Int? = null
)

enum class TrackingTemplateDraftErrorCode {
    TEMPLATE_NAME_REQUIRED,
    FIELD_NAME_REQUIRED,
    SELECT_OPTIONS_REQUIRED,
    OPTION_LABEL_REQUIRED,
    DUPLICATE_OPTION_LABEL,
    INVALID_CONFIGURATION
}
