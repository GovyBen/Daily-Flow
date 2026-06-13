package com.mhss.app.presentation.integrations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhss.app.preferences.PrefsConstants
import com.mhss.app.preferences.domain.model.AiProvider
import com.mhss.app.preferences.domain.model.AiProviderCategory
import com.mhss.app.preferences.domain.model.PrefsKey
import com.mhss.app.preferences.domain.model.customUrlEnabledPrefsKey
import com.mhss.app.preferences.domain.model.customUrlPrefsKey
import com.mhss.app.preferences.domain.model.modelPrefsKey
import com.mhss.app.presentation.components.ExperimentalBadge
import com.mhss.app.presentation.integrations.components.CustomURLSection
import com.mhss.app.presentation.integrations.components.SavableTextField
import com.mhss.app.ui.R
import com.mhss.app.ui.theme.MyBrainTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun AiProviderSection(
    getAiProvider: () -> Flow<AiProvider>,
    getApiKey: (AiProvider) -> Flow<String>,
    getStringSetting: (PrefsKey<String>, String) -> Flow<String>,
    getBooleanSetting: (PrefsKey<Boolean>, Boolean) -> Flow<Boolean>,
    onEvent: (IntegrationsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val provider by getAiProvider().collectAsStateWithLifecycle(AiProvider.None)
    val providerOptions = AiProvider.selectable.map { entry ->
        ProviderOption(
            provider = entry,
            label = entry.displayName,
            icon = painterResource(id = entry.iconResource())
        )
    }
    val aiEnabled = provider != AiProvider.None
    val aiToolsEnabled by getBooleanSetting(
        PrefsKey.BooleanKey(PrefsConstants.AI_TOOLS_ENABLED_KEY),
        false
    ).collectAsStateWithLifecycle(false)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(25.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.ai),
                    style = MaterialTheme.typography.titleLarge
                )
                Switch(
                    checked = aiEnabled,
                    onCheckedChange = {
                        onEvent(IntegrationsEvent.ToggleAiProvider(it))
                    }
                )
            }
            AnimatedVisibility(aiEnabled) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    ProviderSelector(
                        options = providerOptions,
                        selected = provider,
                        onSelected = { selected ->
                            onEvent(IntegrationsEvent.SelectProvider(selected))
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    val providerSettings = AiProvider.entries.associateWith { entry ->
                        entry.collectPreferences(
                            getApiKey = getApiKey,
                            getStringSetting = getStringSetting,
                            getBooleanSetting = getBooleanSetting
                        )
                    }
                    ProviderSettingsContent(
                        provider = provider,
                        settings = providerSettings[provider],
                        onEvent = onEvent
                    )
                    AiToolsSwitch(
                        checked = aiToolsEnabled,
                        onCheck = { onEvent(IntegrationsEvent.ToggleAiTools(it)) }
                    )
                    Text(
                        text = stringResource(R.string.enable_ai_tools_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AiToolsSwitch(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheck: (Boolean) -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable { onCheck(!checked) }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_tools),
                contentDescription = "",
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.enable_ai_tools),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.width(8.dp))
            ExperimentalBadge()
        }
        Switch(checked = checked, onCheckedChange = { onCheck(it) })
    }
}

private data class ProviderOption(
    val provider: AiProvider,
    val label: String,
    val icon: Painter
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderSelector(
    options: List<ProviderOption>,
    selected: AiProvider,
    onSelected: (AiProvider) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.firstOrNull { it.provider == selected } ?: options.first()
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            value = if (selectedOption.provider.recommendedRank != null) {
                "${selectedOption.label} · ${stringResource(R.string.recommended)}"
            } else {
                selectedOption.label
            },
            onValueChange = {},
            readOnly = true,
            shape = RoundedCornerShape(16.dp),
            label = { Text(text = stringResource(R.string.ai_provider)) },
            leadingIcon = { Icon(painter = selectedOption.icon, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AiProviderCategory.entries.forEach { category ->
                val categoryOptions = options.filter { it.provider.category == category }
                if (categoryOptions.isEmpty()) return@forEach
                DropdownMenuItem(
                    text = {
                        Text(
                            text = category.label(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    enabled = false,
                    onClick = {}
                )
                categoryOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (option.provider.recommendedRank != null) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.recommended),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        leadingIcon = { Icon(painter = option.icon, contentDescription = null) },
                        onClick = {
                            expanded = false
                            onSelected(option.provider)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

private data class ProviderPreferences(
    val key: String = "",
    val model: String = "",
    val useCustomUrl: Boolean = false,
    val customUrl: String = ""
)

@Composable
private fun AiProvider.collectPreferences(
    getApiKey: (AiProvider) -> Flow<String>,
    getStringSetting: (PrefsKey<String>, String) -> Flow<String>,
    getBooleanSetting: (PrefsKey<Boolean>, Boolean) -> Flow<Boolean>
): ProviderPreferences {
    val key = getApiKey(this).collectAsStateWithLifecycle("").value
    val modelDefault = defaultModel.orEmpty()
    val model = modelPrefsKey?.let { pref ->
        getStringSetting(pref, modelDefault).collectAsStateWithLifecycle(modelDefault).value
    } ?: modelDefault
    val customUrlDefault = defaultBaseUrl.orEmpty()
    val customUrlEnabled = customUrlEnabledPrefsKey?.let { pref ->
        getBooleanSetting(pref, false).collectAsStateWithLifecycle(false).value
    } ?: false
    val customUrl = customUrlPrefsKey?.let { pref ->
        getStringSetting(pref, customUrlDefault).collectAsStateWithLifecycle(customUrlDefault).value
    } ?: customUrlDefault

    return ProviderPreferences(
        key = key,
        model = model,
        useCustomUrl = customUrlEnabled,
        customUrl = customUrl
    )
}

@Composable
private fun ProviderSettingsContent(
    provider: AiProvider,
    settings: ProviderPreferences?,
    onEvent: (IntegrationsEvent) -> Unit
) {
    if (provider == AiProvider.None || settings == null) return
    provider.keyPref?.let {
        SavableTextField(
            text = settings.key,
            infoURL = provider.keyInfoUrl,
            label = stringResource(R.string.api_key),
            isSecret = true,
            onSave = { onEvent(IntegrationsEvent.UpdateApiKey(provider, it)) }
        )
        Spacer(Modifier.height(8.dp))
    }
    SavableTextField(
        text = settings.model,
        infoURL = provider.modelsInfoUrl,
        label = stringResource(R.string.model),
        resetValue = provider.defaultModel,
        onSave = { onEvent(IntegrationsEvent.UpdateModel(provider, it)) }
    )
    if (provider.supportsCustomUrl &&
        provider.customUrlPrefsKey != null &&
        (provider.customUrlEnabledPrefsKey != null || provider.requiresCustomUrl)
    ) {
        Spacer(Modifier.height(4.dp))
        val showCustomUrl = settings.useCustomUrl || provider.requiresCustomUrl
        val warning =
            if (showCustomUrl && !provider.isLocalProvider && settings.customUrl.startsWith(
                    "http://",
                    true
                )
            ) {
                stringResource(R.string.insecure_url_warning)
            } else null
        CustomURLSection(
            enabled = showCustomUrl,
            url = settings.customUrl,
            label = stringResource(R.string.base_url),
            showCheckbox = !provider.requiresCustomUrl,
            warningText = warning,
            defaultUrl = provider.defaultBaseUrl,
            onSave = { onEvent(IntegrationsEvent.UpdateCustomURL(provider, it)) },
            onEnable = { onEvent(IntegrationsEvent.ToggleCustomURL(provider, it)) }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AiProviderSectionPreview() {
    MyBrainTheme {
        val providerFlow = flowOf(AiProvider.OpenAI)
        val stringSetting: (PrefsKey<String>, String) -> Flow<String> = { _, default ->
            flowOf(default)
        }
        val booleanSetting: (PrefsKey<Boolean>, Boolean) -> Flow<Boolean> = { _, default ->
            flowOf(true)
        }

        AiProviderSection(
            getAiProvider = { providerFlow },
            getApiKey = { flowOf("") },
            getStringSetting = stringSetting,
            getBooleanSetting = booleanSetting,
            onEvent = {}
        )
    }
}

@DrawableRes
private fun AiProvider.iconResource(): Int = when (this) {
    AiProvider.OpenAI -> R.drawable.ic_openai
    AiProvider.Gemini -> R.drawable.ic_gemini
    AiProvider.Anthropic -> R.drawable.ic_anthropic
    AiProvider.OpenRouter -> R.drawable.ic_openrouter
    AiProvider.LmStudio -> R.drawable.ic_lmstudio
    AiProvider.Ollama -> R.drawable.ic_ollama
    AiProvider.DeepSeek,
    AiProvider.Qwen,
    AiProvider.Kimi,
    AiProvider.Glm,
    AiProvider.None -> R.drawable.ic_integrations
}

@Composable
private fun AiProviderCategory.label(): String = when (this) {
    AiProviderCategory.RECOMMENDED -> stringResource(R.string.provider_category_recommended)
    AiProviderCategory.MAINLAND_CHINA -> stringResource(R.string.provider_category_mainland_china)
    AiProviderCategory.INTERNATIONAL -> stringResource(R.string.provider_category_international)
    AiProviderCategory.LOCAL -> stringResource(R.string.provider_category_local)
}

