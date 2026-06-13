package com.mhss.app.tracking.data.repository

import com.mhss.app.tracking.data.database.TrackingTransactionStore
import com.mhss.app.tracking.data.database.dao.TrackingDataPointDao
import com.mhss.app.tracking.data.database.dao.TrackingSessionDao
import com.mhss.app.tracking.data.database.dao.TrackingTemplateDao
import com.mhss.app.tracking.data.database.dao.TrackingTrackerDao
import com.mhss.app.tracking.data.database.entity.DataPointEntity
import com.mhss.app.tracking.data.database.entity.RecordSessionEntity
import com.mhss.app.tracking.data.database.entity.RecordTemplateEntity
import com.mhss.app.tracking.data.database.entity.TemplateFieldEntity
import com.mhss.app.tracking.data.database.entity.TrackerEntity
import com.mhss.app.tracking.data.database.entity.TrackerOptionEntity
import com.mhss.app.tracking.data.factory.TrackingEntityFactory
import com.mhss.app.tracking.data.mapping.TrackerValueMapper
import com.mhss.app.tracking.data.mapping.TrackerValueMappingRequest
import com.mhss.app.tracking.data.serialization.TrackerConfigJson
import com.mhss.app.tracking.domain.id.TrackingIdGenerator
import com.mhss.app.tracking.domain.model.RecordSessionCommand
import com.mhss.app.tracking.domain.model.TrackingFieldDraft
import com.mhss.app.tracking.domain.model.TrackingOptionDraft
import com.mhss.app.tracking.domain.model.TrackingRecordHistory
import com.mhss.app.tracking.domain.model.TrackingRecordedPoint
import com.mhss.app.tracking.domain.model.TrackingSuggestedValue
import com.mhss.app.tracking.domain.model.TrackingTemplateDraft
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import com.mhss.app.tracking.domain.model.TrackingTrackerDraft
import com.mhss.app.tracking.domain.model.TrackerType
import com.mhss.app.tracking.domain.repository.TrackingRepository
import com.mhss.app.tracking.domain.validation.TrackerInputValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class RoomTrackingRepository(
    private val templateDao: TrackingTemplateDao,
    private val trackerDao: TrackingTrackerDao,
    private val sessionDao: TrackingSessionDao,
    private val dataPointDao: TrackingDataPointDao,
    private val transactionStore: TrackingTransactionStore,
    private val entityFactory: TrackingEntityFactory,
    private val valueMapper: TrackerValueMapper,
    private val idGenerator: TrackingIdGenerator,
    @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher
) : TrackingRepository {

    override suspend fun createTemplate(
        draft: TrackingTemplateDraft,
        nowEpochMilli: Long
    ): String = withContext(ioDispatcher) {
        saveTemplateAggregate(null, draft, nowEpochMilli)
    }

    override suspend fun createTemplateIfAbsent(
        draft: TrackingTemplateDraft,
        nowEpochMilli: Long
    ): Boolean = withContext(ioDispatcher) {
        val templateId = requireNotNull(draft.id) {
            "A default template must have a stable ID"
        }
        if (templateDao.getTemplate(templateId) != null) {
            false
        } else {
            saveTemplateAggregate(templateId, draft, nowEpochMilli)
            true
        }
    }

    override suspend fun updateTemplate(
        templateId: String,
        draft: TrackingTemplateDraft,
        nowEpochMilli: Long
    ) = withContext(ioDispatcher) {
        checkNotNull(templateDao.getTemplate(templateId)) {
            "Cannot update a template that does not exist"
        }
        saveTemplateAggregate(templateId, draft, nowEpochMilli)
        Unit
    }

    override suspend fun duplicateTemplate(
        templateId: String,
        nowEpochMilli: Long
    ): String = withContext(ioDispatcher) {
        val source = loadTemplateDraft(templateId)
        val draft = source.copy(
            id = null,
            fields = source.fields.map { field ->
                field.copy(
                    id = null,
                    trackerId = null,
                    tracker = field.tracker.copy(
                        options = field.tracker.options.map { it.copy(id = null) }
                    )
                )
            }
        )
        saveTemplateAggregate(null, draft, nowEpochMilli)
    }

    override suspend fun reorderTemplates(
        orderedTemplateIds: List<String>,
        nowEpochMilli: Long
    ) = withContext(ioDispatcher) {
        require(orderedTemplateIds.distinct().size == orderedTemplateIds.size) {
            "Template order must not contain duplicate IDs"
        }
        val templates = orderedTemplateIds.mapIndexed { index, id ->
            checkNotNull(templateDao.getTemplate(id)) {
                "Cannot reorder a template that does not exist"
            }.copy(
                displayOrder = index,
                updatedAtEpochMilli = nowEpochMilli
            )
        }
        transactionStore.reorderTemplates(templates)
    }

    override suspend fun setTemplatePinned(
        templateId: String,
        isPinned: Boolean,
        nowEpochMilli: Long
    ) = withContext(ioDispatcher) {
        check(templateDao.setTemplatePinned(templateId, isPinned, nowEpochMilli) == 1) {
            "Cannot pin a template that does not exist or is inactive"
        }
    }

    override suspend fun deactivateTemplate(
        templateId: String,
        nowEpochMilli: Long
    ) = withContext(ioDispatcher) {
        check(templateDao.deactivateTemplate(templateId, nowEpochMilli) == 1) {
            "Cannot deactivate a template that does not exist or is already inactive"
        }
    }

    override suspend fun saveRecordSession(
        command: RecordSessionCommand,
        nowEpochMilli: Long
    ): String = withContext(ioDispatcher) {
        require(command.id == null) { "A new record session must not already have an ID" }
        val session = entityFactory.createRecordSession(
            templateId = command.templateId,
            occurredAtEpochMilli = command.occurredAtEpochMilli,
            zoneId = command.zoneId,
            note = command.note,
            source = command.source,
            nowEpochMilli = nowEpochMilli
        )
        transactionStore.saveSession(
            session,
            mapPoints(session, command, nowEpochMilli)
        )
        session.id
    }

    override suspend fun updateRecordSession(
        command: RecordSessionCommand,
        nowEpochMilli: Long
    ) = withContext(ioDispatcher) {
        val sessionId = requireNotNull(command.id) {
            "An updated record session must have an ID"
        }
        val current = checkNotNull(sessionDao.getSession(sessionId)) {
            "Cannot update a record session that does not exist"
        }
        val session = current.copy(
            templateId = command.templateId,
            occurredAtEpochMilli = command.occurredAtEpochMilli,
            zoneId = command.zoneId,
            note = command.note,
            source = command.source.name,
            updatedAtEpochMilli = nowEpochMilli
        )
        transactionStore.updateSession(
            session,
            mapPoints(session, command, nowEpochMilli)
        )
    }

    override suspend fun deleteRecordSession(sessionId: String) {
        withContext(ioDispatcher) {
            transactionStore.deleteSession(sessionId)
        }
    }

    override fun observeTemplates(): Flow<List<TrackingTemplateSummary>> {
        return combine(
            templateDao.observeActiveTemplates(),
            sessionDao.observeLastRecordedTimes()
        ) { templates, lastRecordedTimes ->
            val lastRecordedByTemplate = lastRecordedTimes.associate {
                it.templateId to it.lastRecordedAtEpochMilli
            }
                templates.map { template ->
                    template.toSummary(
                        fields = loadTemplateDraft(template.id).fields,
                        lastRecordedAtEpochMilli = lastRecordedByTemplate[template.id]
                    )
                }
        }.flowOn(ioDispatcher)
    }

    override fun observeRecordHistory(
        templateId: String,
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<TrackingRecordHistory>> {
        return combine(
            sessionDao.observeSessionsInRange(templateId, startInclusive, endExclusive),
            dataPointDao.observeDataPointsForTemplateInRange(
                templateId,
                startInclusive,
                endExclusive
            )
        ) { sessions, points ->
            val pointsBySession = points.groupBy(DataPointEntity::sessionId)
            sessions.map { session ->
                session.toHistory(pointsBySession[session.id].orEmpty())
            }
        }.flowOn(ioDispatcher)
    }

    override suspend fun getSuggestedValues(
        trackerId: String,
        limit: Int
    ): List<TrackingSuggestedValue> = withContext(ioDispatcher) {
        require(limit > 0) { "Suggested value limit must be positive" }
        dataPointDao.getRecentDataPoints(trackerId, limit * 4)
            .groupBy { point ->
                SuggestedValueKey(
                    value = point.value,
                    label = point.label,
                    note = point.note,
                    optionId = point.optionId
                )
            }
            .values
            .map { matchingPoints ->
                matchingPoints.first().toSuggestedValue(matchingPoints.size)
            }
            .sortedWith(
                compareByDescending<TrackingSuggestedValue> { it.lastUsedAtEpochMilli }
                    .thenBy { it.stableKey() }
            )
            .take(limit)
    }

    private suspend fun saveTemplateAggregate(
        templateId: String?,
        draft: TrackingTemplateDraft,
        nowEpochMilli: Long
    ): String {
        require(draft.name.isNotBlank()) { "Template name must not be blank" }
        val currentTemplate = templateId?.let { templateDao.getTemplate(it) }
        val allowNewStableIds = currentTemplate == null && draft.id != null
        val template = RecordTemplateEntity(
            id = templateId ?: draft.id ?: idGenerator.newId(),
            name = draft.name,
            description = draft.description,
            icon = draft.icon,
            color = draft.color,
            displayOrder = draft.displayOrder,
            isActive = currentTemplate?.isActive ?: true,
            isPinned = currentTemplate?.isPinned ?: false,
            createdAtEpochMilli = currentTemplate?.createdAtEpochMilli ?: nowEpochMilli,
            updatedAtEpochMilli = nowEpochMilli
        )

        val trackers = mutableListOf<TrackerEntity>()
        val options = mutableListOf<TrackerOptionEntity>()
        val fields = draft.fields.map { field ->
            val tracker = field.toTrackerEntity(nowEpochMilli, allowNewStableIds)
            trackers += tracker
            options += field.tracker.options.map { option ->
                option.toEntity(tracker.id)
            }
            TemplateFieldEntity(
                id = field.id ?: idGenerator.newId(),
                templateId = template.id,
                trackerId = tracker.id,
                displayOrder = field.displayOrder,
                required = field.required,
                displayNameOverride = field.displayNameOverride,
                defaultValueJson = field.defaultValueJson
            )
        }

        transactionStore.replaceTemplateAggregate(template, trackers, options, fields)
        return template.id
    }

    private suspend fun TrackingFieldDraft.toTrackerEntity(
        nowEpochMilli: Long,
        allowNewStableIds: Boolean
    ): TrackerEntity {
        val current = trackerId?.let { trackerDao.getTracker(it) }
        val createsStableTracker = allowNewStableIds &&
            tracker.id != null &&
            tracker.id == trackerId
        if (trackerId != null && !createsStableTracker) {
            checkNotNull(current) { "Cannot reference a tracker that does not exist" }
        }
        return TrackerEntity(
            id = trackerId ?: tracker.id ?: idGenerator.newId(),
            name = tracker.name,
            type = tracker.config.trackerType.name,
            unit = tracker.unit,
            configJson = TrackerConfigJson.encode(tracker.config),
            isActive = current?.isActive ?: true,
            createdAtEpochMilli = current?.createdAtEpochMilli ?: nowEpochMilli,
            updatedAtEpochMilli = nowEpochMilli
        )
    }

    private fun TrackingOptionDraft.toEntity(trackerId: String) = TrackerOptionEntity(
        id = id ?: idGenerator.newId(),
        trackerId = trackerId,
        label = label,
        numericValue = numericValue,
        color = color,
        displayOrder = displayOrder,
        isActive = isActive
    )

    private suspend fun loadTemplateDraft(templateId: String): TrackingTemplateDraft {
        val template = checkNotNull(templateDao.getTemplate(templateId)) {
            "Cannot duplicate a template that does not exist"
        }
        val fields = templateDao.getFields(templateId).map { field ->
            val tracker = checkNotNull(trackerDao.getTracker(field.trackerId)) {
                "Template field references a missing tracker"
            }
            TrackingFieldDraft(
                id = field.id,
                trackerId = tracker.id,
                tracker = TrackingTrackerDraft(
                    name = tracker.name,
                    config = TrackerConfigJson.decode(tracker.configJson),
                    unit = tracker.unit,
                    options = trackerDao.getOptions(tracker.id).map { option ->
                        TrackingOptionDraft(
                            id = option.id,
                            label = option.label,
                            numericValue = option.numericValue,
                            color = option.color,
                            displayOrder = option.displayOrder,
                            isActive = option.isActive
                        )
                    }
                ),
                displayOrder = field.displayOrder,
                required = field.required,
                displayNameOverride = field.displayNameOverride,
                defaultValueJson = field.defaultValueJson
            )
        }
        return TrackingTemplateDraft(
            id = template.id,
            name = template.name,
            description = template.description,
            icon = template.icon,
            color = template.color,
            displayOrder = template.displayOrder,
            fields = fields
        )
    }

    private suspend fun mapPoints(
        session: RecordSessionEntity,
        command: RecordSessionCommand,
        nowEpochMilli: Long
    ): List<DataPointEntity> {
        val valuesByField = command.values.associateBy { it.fieldId }
        require(valuesByField.size == command.values.size) {
            "Record values must not contain duplicate field IDs"
        }
        val fields = templateDao.getFields(command.templateId)
        require(valuesByField.keys.all { id -> fields.any { it.id == id } }) {
            "Record values contain fields outside the selected template"
        }

        return fields.flatMap { field ->
            val tracker = checkNotNull(trackerDao.getTracker(field.trackerId)) {
                "Template field references a missing tracker"
            }
            val config = TrackerConfigJson.decode(tracker.configJson)
            val input = valuesByField[field.id]?.input ?: config.trackerType.emptyInput()
            valueMapper.map(
                TrackerValueMappingRequest(
                    sessionId = session.id,
                    trackerId = tracker.id,
                    config = config,
                    input = input,
                    required = field.required,
                    options = trackerDao.getOptions(tracker.id),
                    epochMilli = command.occurredAtEpochMilli,
                    utcOffsetSeconds = command.utcOffsetSeconds,
                    nowEpochMilli = nowEpochMilli
                )
            )
        }
    }

    private fun TrackerType.emptyInput(): TrackerInputValue = when (this) {
        TrackerType.MULTI_SELECT -> TrackerInputValue.MultiSelect(emptySet())
        TrackerType.SINGLE_SELECT -> TrackerInputValue.SingleSelect(null)
        TrackerType.COUNTER -> TrackerInputValue.Counter(null)
        TrackerType.SCALE -> TrackerInputValue.Scale(null)
        TrackerType.BOOLEAN -> TrackerInputValue.BooleanValue(null)
        TrackerType.DURATION -> TrackerInputValue.Duration(null)
        TrackerType.NUMBER -> TrackerInputValue.NumberValue(null)
        TrackerType.TEXT -> TrackerInputValue.Text("")
    }
}

private fun RecordTemplateEntity.toSummary(
    fields: List<TrackingFieldDraft>,
    lastRecordedAtEpochMilli: Long?
) = TrackingTemplateSummary(
    id = id,
    name = name,
    description = description,
    icon = icon,
    color = color,
    isPinned = isPinned,
    displayOrder = displayOrder,
    createdAtEpochMilli = createdAtEpochMilli,
    updatedAtEpochMilli = updatedAtEpochMilli,
    lastRecordedAtEpochMilli = lastRecordedAtEpochMilli,
    fields = fields
)

private fun RecordSessionEntity.toHistory(
    points: List<DataPointEntity>
) = TrackingRecordHistory(
    id = id,
    templateId = templateId,
    occurredAtEpochMilli = occurredAtEpochMilli,
    zoneId = zoneId,
    note = note,
    source = com.mhss.app.tracking.domain.model.RecordSource.valueOf(source),
    points = points.map(DataPointEntity::toRecordedPoint)
)

private fun DataPointEntity.toRecordedPoint() = TrackingRecordedPoint(
    id = id,
    trackerId = trackerId,
    epochMilli = epochMilli,
    utcOffsetSeconds = utcOffsetSeconds,
    value = value,
    label = label,
    note = note,
    optionId = optionId
)

private fun DataPointEntity.toSuggestedValue(usageCount: Int) = TrackingSuggestedValue(
    value = value,
    label = label,
    note = note,
    optionId = optionId,
    lastUsedAtEpochMilli = epochMilli,
    usageCount = usageCount
)

private fun TrackingSuggestedValue.stableKey(): String = listOf(
    value?.toString().orEmpty(),
    label.orEmpty(),
    note.orEmpty(),
    optionId.orEmpty()
).joinToString(separator = "\u0000")

private data class SuggestedValueKey(
    val value: Double?,
    val label: String?,
    val note: String?,
    val optionId: String?
)
