package com.mhss.app.widget.tracking

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.material3.ColorProviders
import com.mhss.app.preferences.PrefsConstants
import com.mhss.app.preferences.domain.model.booleanPreferencesKey
import com.mhss.app.preferences.domain.model.intPreferencesKey
import com.mhss.app.preferences.domain.use_case.GetPreferenceUseCase
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import com.mhss.app.tracking.domain.usecase.ObserveTemplatesUseCase
import com.mhss.app.ui.ThemeSettings
import com.mhss.app.widget.WidgetTheme
import com.mhss.app.widget.widgetDarkColorScheme
import com.mhss.app.widget.widgetLightColorScheme
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TrackingWidget : GlanceAppWidget(), KoinComponent {

    private val getSettings: GetPreferenceUseCase by inject()
    private val observeTemplates: ObserveTemplatesUseCase by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val useMaterialYou by getSettings(
                booleanPreferencesKey(PrefsConstants.SETTINGS_MATERIAL_YOU),
                false
            ).collectAsState(false)
            val isSystemDarkMode = remember {
                val currentNightMode =
                    context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                currentNightMode == Configuration.UI_MODE_NIGHT_YES
            }
            val darkModeFlow = remember(isSystemDarkMode) {
                getSettings(
                    intPreferencesKey(PrefsConstants.SETTINGS_THEME_KEY),
                    ThemeSettings.AUTO.value
                ).map {
                    it == ThemeSettings.DARK.value ||
                        (it == ThemeSettings.AUTO.value && isSystemDarkMode)
                }
            }
            val isDarkMode by darkModeFlow.collectAsState(true)
            val templatesFlow = remember {
                observeTemplates().map(::selectTrackingWidgetTemplates)
            }
            val templates by templatesFlow.collectAsState(emptyList())

            WidgetTheme(
                if (useMaterialYou) GlanceTheme.colors
                else if (isDarkMode) ColorProviders(widgetDarkColorScheme)
                else ColorProviders(widgetLightColorScheme)
            ) {
                TrackingHomeScreenWidget(templates)
            }
        }
    }
}

class TrackingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TrackingWidget()
}

internal fun selectTrackingWidgetTemplates(
    templates: List<TrackingTemplateSummary>
): List<TrackingTemplateSummary> = templates
    .filter(TrackingTemplateSummary::isPinned)
    .sortedWith(
        compareBy(TrackingTemplateSummary::displayOrder)
            .thenBy(TrackingTemplateSummary::id)
    )
    .take(MAX_WIDGET_TEMPLATES)

private const val MAX_WIDGET_TEMPLATES = 4
