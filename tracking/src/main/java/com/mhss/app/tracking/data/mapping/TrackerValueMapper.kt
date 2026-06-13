package com.mhss.app.tracking.data.mapping

import com.mhss.app.tracking.data.database.entity.DataPointEntity
import com.mhss.app.tracking.data.database.entity.TrackerOptionEntity
import com.mhss.app.tracking.data.factory.TrackingEntityFactory
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import com.mhss.app.tracking.domain.validation.TrackerValueValidator

class TrackerValueMapper(
    private val entityFactory: TrackingEntityFactory
) {

    fun map(request: TrackerValueMappingRequest): List<DataPointEntity> {
        val activeOptions = request.options.filter(TrackerOptionEntity::isActive)
        val validation = TrackerValueValidator.validate(
            config = request.config,
            input = request.input,
            required = request.required,
            activeOptionIds = activeOptions.mapTo(mutableSetOf(), TrackerOptionEntity::id)
        )
        if (!validation.isValid) {
            throw TrackerValueMappingException(validation.errors)
        }

        return when (val input = request.input) {
            is TrackerInputValue.MultiSelect -> input.optionIds
                .map { optionId -> activeOptions.option(optionId) }
                .sortedWith(compareBy<TrackerOptionEntity> { it.displayOrder }.thenBy { it.id })
                .map { option -> request.optionPoint(option) }

            is TrackerInputValue.SingleSelect -> input.optionId
                ?.let { optionId -> listOf(request.optionPoint(activeOptions.option(optionId))) }
                .orEmpty()

            is TrackerInputValue.Counter -> request.numericPoint(input.value)
            is TrackerInputValue.Scale -> request.numericPoint(input.value)
            is TrackerInputValue.BooleanValue ->
                request.numericPoint(input.value?.let { if (it) 1.0 else 0.0 })

            is TrackerInputValue.Duration -> request.numericPoint(input.seconds?.toDouble())
            is TrackerInputValue.NumberValue -> request.numericPoint(input.value)
            is TrackerInputValue.Text -> if (input.value.isBlank()) {
                emptyList()
            } else {
                listOf(request.point(note = input.value))
            }
        }
    }

    private fun TrackerValueMappingRequest.optionPoint(
        option: TrackerOptionEntity
    ): DataPointEntity = point(
        value = option.numericValue ?: 1.0,
        label = option.label,
        optionId = option.id
    )

    private fun TrackerValueMappingRequest.numericPoint(
        value: Double?
    ): List<DataPointEntity> = value?.let {
        listOf(point(value = it))
    }.orEmpty()

    private fun TrackerValueMappingRequest.point(
        value: Double? = null,
        label: String? = null,
        note: String? = null,
        optionId: String? = null
    ): DataPointEntity = entityFactory.createDataPoint(
        sessionId = sessionId,
        trackerId = trackerId,
        epochMilli = epochMilli,
        utcOffsetSeconds = utcOffsetSeconds,
        value = value,
        label = label,
        note = note,
        optionId = optionId,
        nowEpochMilli = nowEpochMilli
    )

    private fun List<TrackerOptionEntity>.option(id: String): TrackerOptionEntity {
        return checkNotNull(firstOrNull { it.id == id }) {
            "Validated option $id is missing"
        }
    }
}
