package com.mhss.app.presentation.integrations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.data.noteMarkdownModule
import com.mhss.app.data.noteRoomModule
import com.mhss.app.domain.repository.FileUtilsRepository
import com.mhss.app.domain.use_case.UpdateExternalNotesFolderUseCase
import com.mhss.app.preferences.PrefsConstants
import com.mhss.app.preferences.domain.model.AiProvider
import com.mhss.app.preferences.domain.model.PrefsKey
import com.mhss.app.preferences.domain.model.PrefsKey.BooleanKey
import com.mhss.app.preferences.domain.model.PrefsKey.IntKey
import com.mhss.app.preferences.domain.model.customUrlEnabledPrefsKey
import com.mhss.app.preferences.domain.model.customUrlPrefsKey
import com.mhss.app.preferences.domain.model.modelPrefsKey
import com.mhss.app.preferences.domain.model.stringPreferencesKey
import com.mhss.app.preferences.domain.model.toAiProvider
import com.mhss.app.preferences.domain.repository.SecretStore
import com.mhss.app.preferences.domain.use_case.GetPreferenceUseCase
import com.mhss.app.preferences.domain.use_case.SavePreferenceUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.context.GlobalContext.loadKoinModules
import org.koin.core.context.GlobalContext.unloadKoinModules

@KoinViewModel
class IntegrationsViewModel(
    private val savePreference: SavePreferenceUseCase,
    private val getPreference: GetPreferenceUseCase,
    private val updateExternalNotesFolder: UpdateExternalNotesFolderUseCase,
    private val fileUtilsRepository: FileUtilsRepository,
    private val secretStore: SecretStore
) : ViewModel() {

    fun <T> getSettings(key: PrefsKey<T>, defaultValue: T): Flow<T> {
        return getPreference(key, defaultValue)
    }

    fun getAiProvider(): Flow<AiProvider> = getPreference(
        IntKey(PrefsConstants.AI_PROVIDER_KEY),
        AiProvider.None.id
    ).map { it.toAiProvider() }

    fun getApiKey(provider: AiProvider): Flow<String> =
        provider.keyPref?.let(secretStore::observe) ?: kotlinx.coroutines.flow.flowOf("")

    fun getExternalNotesFolderPath(): Flow<String?> {
        return getPreference(
            stringPreferencesKey(PrefsConstants.EXTERNAL_NOTES_FOLDER_URI),
            ""
        ).map { uri ->
            if (uri.isBlank()) null
            else fileUtilsRepository.getPathFromUri(uri)
        }
    }

    fun onEvent(event: IntegrationsEvent) {
        when (event) {
            is IntegrationsEvent.ToggleAiProvider -> {
                saveSettings(
                    IntKey(PrefsConstants.AI_PROVIDER_KEY),
                    if (event.enabled) AiProvider.DeepSeek.id else AiProvider.None.id
                )
            }

            is IntegrationsEvent.SelectProvider -> {
                saveSettings(
                    IntKey(PrefsConstants.AI_PROVIDER_KEY),
                    event.provider.id
                )
            }

            is IntegrationsEvent.UpdateApiKey -> {
                event.provider.keyPref?.let { secretId ->
                    viewModelScope.launch {
                        secretStore.set(secretId, event.key)
                    }
                }
            }

            is IntegrationsEvent.UpdateModel -> {
                event.provider.modelPrefsKey?.let { key ->
                    saveSettings(key, event.model)
                }
            }

            is IntegrationsEvent.ToggleCustomURL -> {
                if (!event.provider.supportsCustomUrl) return
                event.provider.customUrlEnabledPrefsKey?.let { key ->
                    saveSettings(key, event.enabled)
                }
                val defaultBaseUrl = event.provider.defaultBaseUrl
                if (!event.enabled && defaultBaseUrl != null) {
                    event.provider.customUrlPrefsKey?.let { key ->
                        saveSettings(key, defaultBaseUrl)
                    }
                }
            }

            is IntegrationsEvent.UpdateCustomURL -> {
                if (!event.provider.supportsCustomUrl) return
                event.provider.customUrlPrefsKey?.let { key ->
                    saveSettings(key, event.url)
                }
            }

            is IntegrationsEvent.ToggleAiTools -> {
                saveSettings(
                    BooleanKey(PrefsConstants.AI_TOOLS_ENABLED_KEY),
                    event.enabled
                )
            }

            is IntegrationsEvent.SelectExternalNotesFolder -> {
                viewModelScope.launch {
                    updateExternalNotesFolder(event.folderUri)
                    unloadKoinModules(noteRoomModule)
                    loadKoinModules(noteMarkdownModule(event.folderUri))
                }
            }

            is IntegrationsEvent.SetExternalNotesEnabled -> {
                saveSettings(
                    BooleanKey(PrefsConstants.EXTERNAL_NOTES_ENABLED),
                    event.enabled
                )
                viewModelScope.launch {
                    if (event.enabled) {
                        val rootUri = getPreference(
                            stringPreferencesKey(PrefsConstants.EXTERNAL_NOTES_FOLDER_URI),
                            ""
                        ).first()
                        if (rootUri.isNotBlank()) {
                            loadKoinModules(noteMarkdownModule(rootUri))
                        }
                    } else {
                        loadKoinModules(noteRoomModule)
                    }
                }
            }
        }
    }

    private fun <T> saveSettings(key: PrefsKey<T>, value: T) {
        viewModelScope.launch {
            savePreference(key, value)
        }
    }
}

