package com.mhss.app.mybrain.presentation.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import com.mhss.app.preferences.PrefsConstants
import com.mhss.app.preferences.domain.model.AiProvider
import com.mhss.app.preferences.domain.model.intPreferencesKey
import com.mhss.app.preferences.domain.model.toAiProvider
import com.mhss.app.preferences.domain.use_case.GetPreferenceUseCase
import com.mhss.app.ui.navigation.Screen
import org.koin.compose.koinInject

@Composable
fun AiFloatingButton(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val getPreference = koinInject<GetPreferenceUseCase>()
    val aiProvider by getPreference(
        intPreferencesKey(PrefsConstants.AI_PROVIDER_KEY),
        AiProvider.None.id
    ).collectAsStateWithLifecycle(initialValue = AiProvider.None.id)

    val aiEnabled = remember(aiProvider) {
        aiProvider.toAiProvider() != AiProvider.None
    }

    val isOnAssistant = remember(navController) {
        navController.currentBackStackEntry?.destination?.route?.contains("Assistant") == true
    }

    AnimatedVisibility(
        visible = aiEnabled && !isOnAssistant,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 16.dp, bottom = 80.dp),
            contentAlignment = androidx.compose.ui.Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Screen.AssistantScreen)
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Assistant",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
