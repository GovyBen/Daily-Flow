package com.mhss.app.presentation.integrations

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhss.app.preferences.PrefsConstants
import com.mhss.app.preferences.domain.model.booleanPreferencesKey
import com.mhss.app.presentation.integrations.components.ExternalNotesCard
import com.mhss.app.ui.R
import com.mhss.app.ui.components.common.MyBrainAppBar
import org.koin.androidx.compose.koinViewModel

@Composable
fun IntegrationsScreen(
    viewModel: IntegrationsViewModel = koinViewModel()
) {

    Scaffold(
        topBar = {
            MyBrainAppBar(
                title = stringResource(R.string.integrations)
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxWidth().imePadding(), contentPadding = paddingValues) {
            item {
                AiProviderSection(
                    getAiProvider = viewModel::getAiProvider,
                    getApiKey = viewModel::getApiKey,
                    getStringSetting = viewModel::getSettings,
                    getBooleanSetting = viewModel::getSettings,
                    onEvent = viewModel::onEvent
                )
            }
            item {
                val isExternalNotesEnabled by viewModel.getSettings(
                    booleanPreferencesKey(PrefsConstants.EXTERNAL_NOTES_ENABLED),
                    false
                ).collectAsStateWithLifecycle(false)
                val selectedFolder by viewModel.getExternalNotesFolderPath()
                    .collectAsStateWithLifecycle(null)
                ExternalNotesCard(
                    isEnabled = isExternalNotesEnabled,
                    selectedFolder = selectedFolder,
                    onSwitchToggled = {
                        viewModel.onEvent(IntegrationsEvent.SetExternalNotesEnabled(it))
                    },
                    onFolderSelected = {
                        viewModel.onEvent(IntegrationsEvent.SelectExternalNotesFolder(it))
                    }
                )
            }
        }
    }
}
