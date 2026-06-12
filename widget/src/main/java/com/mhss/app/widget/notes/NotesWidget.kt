package com.mhss.app.widget.notes

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
import com.mhss.app.domain.use_case.GetAllNotesUseCase
import com.mhss.app.preferences.PrefsConstants
import com.mhss.app.preferences.domain.model.Order
import com.mhss.app.preferences.domain.model.OrderType
import com.mhss.app.preferences.domain.model.booleanPreferencesKey
import com.mhss.app.preferences.domain.model.intPreferencesKey
import com.mhss.app.preferences.domain.model.toInt
import com.mhss.app.preferences.domain.model.toOrder
import com.mhss.app.preferences.domain.use_case.GetPreferenceUseCase
import com.mhss.app.ui.ThemeSettings
import com.mhss.app.widget.WidgetTheme
import com.mhss.app.widget.widgetDarkColorScheme
import com.mhss.app.widget.widgetLightColorScheme
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotesWidget : GlanceAppWidget(), KoinComponent {

    private val getSettings: GetPreferenceUseCase by inject()
    private val getAllNotes: GetAllNotesUseCase by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        provideContent {
            val order by getSettings(
                intPreferencesKey(PrefsConstants.NOTES_ORDER_KEY),
                Order.DateModified(OrderType.ASC).toInt()
            ).collectAsState(Order.DateModified(OrderType.ASC).toInt())
            val useMaterialYou by getSettings(
                booleanPreferencesKey(PrefsConstants.SETTINGS_MATERIAL_YOU),
                false
            ).collectAsState(false)
            val showAllNotes by getSettings(
                booleanPreferencesKey(PrefsConstants.SHOW_ALL_NOTES_KEY),
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
            val notes by getAllNotes(
                order.toOrder(),
                showAllNotes
            ).collectAsState(emptyList())
            val limitedNotes = remember(notes) { notes.take(10) }

            WidgetTheme(
                if (useMaterialYou) GlanceTheme.colors
                else if (isDarkMode) ColorProviders(widgetDarkColorScheme)
                else ColorProviders(widgetLightColorScheme)
            ) {
                NotesHomeScreenWidget(
                    limitedNotes
                )
            }
        }
    }
}

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotesWidget()
}

