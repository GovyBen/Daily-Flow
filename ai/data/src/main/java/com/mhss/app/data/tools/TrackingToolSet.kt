package com.mhss.app.data.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.mhss.app.data.nowMillis
import com.mhss.app.data.parseDateTimeFromLLM
import org.koin.core.annotation.Factory
import kotlin.uuid.Uuid

/**
 * AI tools for structured tracking records (DF-504).
 * Uses late-bound [TrackingDataProvider] to avoid a compile-time dependency
 * on the Android-only :tracking module from this JVM module.
 */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@Factory
class TrackingToolSet(
    private val trackingData: TrackingDataProvider
) : ToolSet {

    @Tool("getRecordTemplates")
    @LLMDescription("Get all record templates with their fields.")
    suspend fun getRecordTemplates(): String {
        return trackingData.getTemplates()
    }

    @Tool("searchRecordSessions")
    @LLMDescription("Search recent record sessions for a template.")
    suspend fun searchRecordSessions(templateId: String): String {
        return trackingData.getSessions(templateId)
    }

    @Tool("proposeCreateRecordSession")
    @LLMDescription("Propose creating a structured record. Returns a proposal ID.")
    suspend fun proposeCreateRecordSession(
        templateId: String,
        @LLMDescription("Format: yyyy-MM-dd HH:mm") occurredAt: String? = null,
        note: String? = null
    ): ProposalResult {
        return ProposalResult(
            proposalId = Uuid.random().toString(),
            summary = "Record session for template: $templateId",
            proposalJson = ""
        )
    }
}

/**
 * Late-bound provider so that [TrackingToolSet] (JVM module) can access
 * tracking data without a compile-time dependency on the Android-only
 * :tracking module.
 *
 * The real implementation is provided in the app module via Koin.
 */
interface TrackingDataProvider {
    suspend fun getTemplates(): String
    suspend fun getSessions(templateId: String): String
}
