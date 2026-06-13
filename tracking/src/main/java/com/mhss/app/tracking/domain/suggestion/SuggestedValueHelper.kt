/*
 * This file is adapted from Track & Graph's SuggestedValueHelper.kt.
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Daily Flow replaces the upstream streaming sampler and configurable
 * value/label ordering with repository-backed recent, frequent and default
 * groups for its sealed tracker input model.
 */
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
import com.mhss.app.tracking.domain.model.TrackingSuggestedValue
import com.mhss.app.tracking.domain.repository.TrackingRepository
import com.mhss.app.tracking.domain.serialization.TrackerInputValueJson
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import com.mhss.app.tracking.domain.validation.TrackerValueValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named
import kotlin.math.max
import kotlin.math.roundToLong

data class TrackingInputSuggestion(
    val input: TrackerInputValue,
    val usageCount: Int,
    val lastUsedAtEpochMilli: Long
)

data class TrackingValueSuggestions(
    val defaultValue: TrackerInputValue? = null,
    val recent: List<TrackingInputSuggestion> = emptyList(),
    val frequent: List<TrackingInputSuggestion> = emptyList(),
    val counterIncrement: TrackerInputValue.Counter? = null
)

@Factory
class SuggestedValueHelper(
    private val repository: TrackingRepository,
    @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun getSuggestions(
        field: TrackingFieldDraft,
        currentInput: TrackerInputValue? = null,
        limit: Int = DEFAULT_LIMIT,
        includeTextHistory: Boolean = false
    ): TrackingValueSuggestions = withContext(ioDispatcher) {
        require(limit > 0) { "Suggestion limit must be positive" }
        val historyLimit = max(limit * HISTORY_FETCH_MULTIPLIER, MINIMUM_HISTORY_FETCH)
        val history = field.trackerId?.let { trackerId ->
            repository.getSuggestedValues(trackerId, historyLimit)
        }.orEmpty()

        buildSuggestions(
            field = field,
            history = history,
            currentInput = currentInput,
            limit = limit,
            includeTextHistory = includeTextHistory
        )
    }

    companion object {
        const val DEFAULT_LIMIT = 5
        const val MAX_TEXT_HISTORY = 3
        private const val HISTORY_FETCH_MULTIPLIER = 4
        private const val MINIMUM_HISTORY_FETCH = 20
    }
}

internal fun buildSuggestions(
    field: TrackingFieldDraft,
    history: List<TrackingSuggestedValue>,
    currentInput: TrackerInputValue? = null,
    limit: Int = SuggestedValueHelper.DEFAULT_LIMIT,
    includeTextHistory: Boolean = false
): TrackingValueSuggestions {
    require(limit > 0) { "Suggestion limit must be positive" }
    val config = field.tracker.config
    val activeOptionIds = field.tracker.options
        .filter { it.isActive }
        .mapNotNullTo(mutableSetOf()) { it.id }
    val defaultValue = field.defaultValueJson
        ?.let { encoded -> runCatching { TrackerInputValueJson.decode(encoded) }.getOrNull() }
        ?.takeIf { input -> input.isValidFor(config, activeOptionIds) }
    val historyLimit = if (config is TextConfig) {
        if (includeTextHistory) minOf(limit, SuggestedValueHelper.MAX_TEXT_HISTORY) else 0
    } else {
        limit
    }
    val mappedHistory = history
        .mapNotNull { value ->
            value.toInput(config)
                ?.takeIf { input -> input.isValidFor(config, activeOptionIds) }
                ?.let { input ->
                    TrackingInputSuggestion(
                        input = input,
                        usageCount = value.usageCount.coerceAtLeast(1),
                        lastUsedAtEpochMilli = value.lastUsedAtEpochMilli
                    )
                }
        }
        .groupBy(TrackingInputSuggestion::input)
        .map { (input, matching) ->
            TrackingInputSuggestion(
                input = input,
                usageCount = matching.sumOf(TrackingInputSuggestion::usageCount),
                lastUsedAtEpochMilli = matching.maxOf(
                    TrackingInputSuggestion::lastUsedAtEpochMilli
                )
            )
        }
        .filterNot { it.input == defaultValue }

    val recent = mappedHistory
        .sortedWith(
            compareByDescending<TrackingInputSuggestion> { it.lastUsedAtEpochMilli }
                .thenBy { it.input.stableKey() }
        )
        .take(historyLimit)
    val frequent = mappedHistory
        .sortedWith(
            compareByDescending<TrackingInputSuggestion> { it.usageCount }
                .thenByDescending { it.lastUsedAtEpochMilli }
                .thenBy { it.input.stableKey() }
        )
        .take(historyLimit)

    return TrackingValueSuggestions(
        defaultValue = defaultValue,
        recent = recent,
        frequent = frequent,
        counterIncrement = counterIncrement(
            config = config,
            activeOptionIds = activeOptionIds,
            currentInput = currentInput,
            defaultValue = defaultValue,
            recent = recent
        )
    )
}

private fun TrackingSuggestedValue.toInput(config: TrackerConfig): TrackerInputValue? =
    when (config) {
        is MultiSelectConfig -> optionId?.let { TrackerInputValue.MultiSelect(setOf(it)) }
        SingleSelectConfig -> optionId?.let { TrackerInputValue.SingleSelect(it) }
        is CounterConfig -> value?.let { TrackerInputValue.Counter(it) }
        is ScaleConfig -> value?.let { TrackerInputValue.Scale(it) }
        is BooleanConfig -> when (value) {
            1.0 -> TrackerInputValue.BooleanValue(true)
            0.0 -> TrackerInputValue.BooleanValue(false)
            else -> null
        }
        is DurationConfig -> value
            ?.takeIf { it.isFinite() && it == it.roundToLong().toDouble() }
            ?.roundToLong()
            ?.let { TrackerInputValue.Duration(it) }
        is NumberConfig -> value?.let { TrackerInputValue.NumberValue(it) }
        is TextConfig -> note
            ?.takeIf(String::isNotBlank)
            ?.let { TrackerInputValue.Text(it) }
    }

private fun counterIncrement(
    config: TrackerConfig,
    activeOptionIds: Set<String>,
    currentInput: TrackerInputValue?,
    defaultValue: TrackerInputValue?,
    recent: List<TrackingInputSuggestion>
): TrackerInputValue.Counter? {
    if (config !is CounterConfig || config.step <= 0) return null
    val base = (currentInput as? TrackerInputValue.Counter)?.value
        ?: (defaultValue as? TrackerInputValue.Counter)?.value
        ?: (recent.firstOrNull()?.input as? TrackerInputValue.Counter)?.value
    val next = if (base == null) {
        config.minimum.toDouble()
    } else {
        base + config.step
    }
    return TrackerInputValue.Counter(next)
        .takeIf { it.isValidFor(config, activeOptionIds) }
}

private fun TrackerInputValue.isValidFor(
    config: TrackerConfig,
    activeOptionIds: Set<String>
): Boolean = TrackerValueValidator.validate(
    config = config,
    input = this,
    required = true,
    activeOptionIds = activeOptionIds
).isValid

private fun TrackerInputValue.stableKey(): String = when (this) {
    is TrackerInputValue.MultiSelect -> "0:${optionIds.sorted().joinToString("\u0000")}"
    is TrackerInputValue.SingleSelect -> "1:${optionId.orEmpty()}"
    is TrackerInputValue.Counter -> "2:${value?.toString().orEmpty()}"
    is TrackerInputValue.Scale -> "3:${value?.toString().orEmpty()}"
    is TrackerInputValue.BooleanValue -> "4:${value?.toString().orEmpty()}"
    is TrackerInputValue.Duration -> "5:${seconds?.toString().orEmpty()}"
    is TrackerInputValue.NumberValue -> "6:${value?.toString().orEmpty()}"
    is TrackerInputValue.Text -> "7:$value"
}
