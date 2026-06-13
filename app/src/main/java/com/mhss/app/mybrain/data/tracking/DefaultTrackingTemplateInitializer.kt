package com.mhss.app.mybrain.data.tracking

import com.mhss.app.preferences.PrefsConstants
import com.mhss.app.preferences.domain.model.booleanPreferencesKey
import com.mhss.app.preferences.domain.repository.PreferenceRepository
import com.mhss.app.tracking.domain.defaults.DefaultTrackingTemplates
import com.mhss.app.tracking.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.first

class DefaultTrackingTemplateInitializer(
    private val preferenceRepository: PreferenceRepository,
    private val trackingRepository: TrackingRepository
) {
    suspend fun initialize(nowEpochMilli: Long) {
        val initializedKey = booleanPreferencesKey(
            PrefsConstants.DEFAULT_TRACKING_TEMPLATES_INITIALIZED
        )
        if (preferenceRepository.getPreference(initializedKey, false).first()) {
            return
        }

        DefaultTrackingTemplates.templates.forEach { template ->
            trackingRepository.createTemplateIfAbsent(template, nowEpochMilli)
        }
        preferenceRepository.savePreference(initializedKey, true)
    }
}
