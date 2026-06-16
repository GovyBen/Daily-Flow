package com.mhss.app.data.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.mhss.app.data.llmDateTimeWithDayNameFormat
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Factory
import kotlin.time.Instant

@Factory
class UtilToolSet : ToolSet {

    @Tool(FORMAT_DATE_TOOL)
    @LLMDescription("Convert a date in milliseconds to a formatted date string.")
    fun formatDate(@LLMDescription("The date in milliseconds.") millis: Long): FormattedDateResult {
        val instant = Instant.fromEpochMilliseconds(millis)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return FormattedDateResult(localDateTime.format(llmDateTimeWithDayNameFormat))
    }

    @Tool("summarizeStatistics")
    @LLMDescription("Receive pre-aggregated statistics data for summarization.")
    fun summarizeStatistics(data: SummaryInput): String {
        val sb = StringBuilder()
        sb.appendLine("Statistics Summary:")
        sb.appendLine("Period: ${data.periodLabel}")
        sb.appendLine("Total records: ${data.totalRecords}")
        if (data.dailyAverage > 0) sb.appendLine("Daily average: $data.dailyAverage")
        if (data.currentStreak > 0) sb.appendLine("Current streak: ${data.currentStreak} days")
        if (data.longestStreak > 0) sb.appendLine("Longest streak: ${data.longestStreak} days")
        data.topLabels.forEach { (label, count) -> sb.appendLine("  - $label: $count times") }
        return sb.toString()
    }
}

@Serializable
data class FormattedDateResult(val formattedDate: String)

@Serializable
data class SummaryInput(
    val periodLabel: String, val totalRecords: Int,
    val dailyAverage: Double = 0.0, val weeklyAverage: Double = 0.0,
    val currentStreak: Int = 0, val longestStreak: Int = 0,
    val topLabels: List<LabelCount> = emptyList())

@Serializable
data class LabelCount(val label: String, val count: Int)
