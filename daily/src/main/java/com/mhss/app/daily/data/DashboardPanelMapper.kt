package com.mhss.app.daily.data

import com.mhss.app.daily.domain.model.DailyItemsPanelConfig
import com.mhss.app.daily.domain.model.DashboardPanel
import com.mhss.app.daily.domain.model.DashboardPanelConfig
import com.mhss.app.daily.domain.model.DashboardPanelSize
import com.mhss.app.daily.domain.model.DashboardPanelType
import com.mhss.app.daily.domain.model.EmptyDashboardPanelConfig
import com.mhss.app.daily.domain.model.PendingRemindersPanelConfig
import com.mhss.app.daily.domain.model.QuickRecordPanelConfig
import com.mhss.app.daily.domain.model.TrackingSummaryPanelConfig
import com.mhss.app.daily.domain.model.defaultConfig
import com.mhss.app.database.entity.DashboardPanelEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val dashboardJson = Json { ignoreUnknownKeys = true }

fun DashboardPanelEntity.toDashboardPanel(): DashboardPanel {
    val type = enumValueOrDefault(type, DashboardPanelType.DAILY_ITEMS)
    return DashboardPanel(
        id = id,
        type = type,
        enabled = enabled,
        displayOrder = displayOrder,
        size = enumValueOrDefault(size, DashboardPanelSize.MEDIUM),
        config = decodeConfig(type, configJson),
        createdAtEpochMilli = createdAt,
        updatedAtEpochMilli = updatedAt
    )
}

fun DashboardPanel.toDashboardPanelEntity(): DashboardPanelEntity {
    return DashboardPanelEntity(
        id = id,
        type = type.name,
        enabled = enabled,
        displayOrder = displayOrder,
        size = size.name,
        configJson = encodeConfig(config),
        createdAt = createdAtEpochMilli,
        updatedAt = updatedAtEpochMilli
    )
}

private fun decodeConfig(
    type: DashboardPanelType,
    configJson: String
): DashboardPanelConfig = runCatching {
    when (type) {
        DashboardPanelType.DAILY_ITEMS,
        DashboardPanelType.OVERDUE_ITEMS -> dashboardJson.decodeFromString<DailyItemsPanelConfig>(configJson)
        DashboardPanelType.QUICK_RECORD -> dashboardJson.decodeFromString<QuickRecordPanelConfig>(configJson)
        DashboardPanelType.TRACKING_SUMMARY,
        DashboardPanelType.TRACKING_TRENDS -> dashboardJson.decodeFromString<TrackingSummaryPanelConfig>(configJson)
        DashboardPanelType.PENDING_REMINDERS -> dashboardJson.decodeFromString<PendingRemindersPanelConfig>(configJson)
        DashboardPanelType.POMODORO,
        DashboardPanelType.AI_ASSISTANT,
        DashboardPanelType.CALENDAR_SYNC_STATUS -> dashboardJson.decodeFromString<EmptyDashboardPanelConfig>(configJson)
    }
}.getOrElse { type.defaultConfig() }

private fun encodeConfig(config: DashboardPanelConfig): String = when (config) {
    is DailyItemsPanelConfig -> dashboardJson.encodeToString(config)
    is QuickRecordPanelConfig -> dashboardJson.encodeToString(config)
    is TrackingSummaryPanelConfig -> dashboardJson.encodeToString(config)
    is PendingRemindersPanelConfig -> dashboardJson.encodeToString(config)
    is EmptyDashboardPanelConfig -> dashboardJson.encodeToString(config)
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(
    name: String,
    default: T
): T = enumValues<T>().firstOrNull { it.name == name } ?: default
