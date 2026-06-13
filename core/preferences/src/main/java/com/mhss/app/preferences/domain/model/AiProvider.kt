package com.mhss.app.preferences.domain.model

import com.mhss.app.preferences.PrefsConstants

enum class AiProviderCategory {
    RECOMMENDED,
    MAINLAND_CHINA,
    INTERNATIONAL,
    LOCAL
}

enum class AiProviderProtocol {
    OPENAI_COMPATIBLE,
    GOOGLE,
    ANTHROPIC,
    OPENROUTER,
    OLLAMA,
    NONE
}

enum class AiProvider(
    val id: Int,
    val displayName: String,
    val category: AiProviderCategory,
    val protocol: AiProviderProtocol,
    val recommendedRank: Int? = null,
    val keyPref: String? = null,
    val modelPref: String? = null,
    val defaultModel: String? = null,
    val keyInfoUrl: String? = null,
    val modelsInfoUrl: String? = null,
    val supportsCustomUrl: Boolean = false,
    val requiresCustomUrl: Boolean = false,
    val isLocalProvider: Boolean = false,
    val customUrlPref: String? = null,
    val customUrlEnabledPref: String? = null,
    val defaultBaseUrl: String? = null
) {
    None(
        id = 0,
        displayName = "None",
        category = AiProviderCategory.INTERNATIONAL,
        protocol = AiProviderProtocol.NONE
    ),
    DeepSeek(
        id = 7,
        displayName = "DeepSeek",
        category = AiProviderCategory.RECOMMENDED,
        protocol = AiProviderProtocol.OPENAI_COMPATIBLE,
        recommendedRank = 1,
        keyPref = PrefsConstants.DEEPSEEK_KEY,
        modelPref = PrefsConstants.DEEPSEEK_MODEL_KEY,
        defaultModel = "deepseek-v4-flash",
        keyInfoUrl = "https://platform.deepseek.com/api_keys",
        modelsInfoUrl = "https://api-docs.deepseek.com/quick_start/pricing",
        supportsCustomUrl = true,
        requiresCustomUrl = true,
        customUrlPref = PrefsConstants.DEEPSEEK_URL_KEY,
        defaultBaseUrl = "https://api.deepseek.com"
    ),
    OpenAI(
        id = 2,
        displayName = "OpenAI",
        category = AiProviderCategory.RECOMMENDED,
        protocol = AiProviderProtocol.OPENAI_COMPATIBLE,
        recommendedRank = 2,
        keyPref = PrefsConstants.OPENAI_KEY,
        modelPref = PrefsConstants.OPENAI_MODEL_KEY,
        defaultModel = "gpt-5.5",
        keyInfoUrl = "https://platform.openai.com/api-keys",
        modelsInfoUrl = "https://platform.openai.com/docs/models",
        supportsCustomUrl = true,
        customUrlPref = PrefsConstants.OPENAI_URL_KEY,
        customUrlEnabledPref = PrefsConstants.OPENAI_USE_URL_KEY,
        defaultBaseUrl = "https://api.openai.com/v1"
    ),
    Qwen(
        id = 8,
        displayName = "通义千问",
        category = AiProviderCategory.MAINLAND_CHINA,
        protocol = AiProviderProtocol.OPENAI_COMPATIBLE,
        keyPref = PrefsConstants.QWEN_KEY,
        modelPref = PrefsConstants.QWEN_MODEL_KEY,
        defaultModel = "qwen-plus",
        keyInfoUrl = "https://bailian.console.aliyun.com/",
        modelsInfoUrl = "https://help.aliyun.com/zh/model-studio/models",
        supportsCustomUrl = true,
        requiresCustomUrl = true,
        customUrlPref = PrefsConstants.QWEN_URL_KEY,
        defaultBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    ),
    Kimi(
        id = 9,
        displayName = "Kimi",
        category = AiProviderCategory.MAINLAND_CHINA,
        protocol = AiProviderProtocol.OPENAI_COMPATIBLE,
        keyPref = PrefsConstants.KIMI_KEY,
        modelPref = PrefsConstants.KIMI_MODEL_KEY,
        defaultModel = "kimi-k2.6",
        keyInfoUrl = "https://platform.moonshot.cn/console/api-keys",
        modelsInfoUrl = "https://platform.moonshot.cn/docs/pricing/chat",
        supportsCustomUrl = true,
        requiresCustomUrl = true,
        customUrlPref = PrefsConstants.KIMI_URL_KEY,
        defaultBaseUrl = "https://api.moonshot.cn/v1"
    ),
    Glm(
        id = 10,
        displayName = "智谱 GLM",
        category = AiProviderCategory.MAINLAND_CHINA,
        protocol = AiProviderProtocol.OPENAI_COMPATIBLE,
        keyPref = PrefsConstants.GLM_KEY,
        modelPref = PrefsConstants.GLM_MODEL_KEY,
        defaultModel = "glm-5.1",
        keyInfoUrl = "https://open.bigmodel.cn/usercenter/apikeys",
        modelsInfoUrl = "https://docs.bigmodel.cn/cn/guide/start/model-overview",
        supportsCustomUrl = true,
        requiresCustomUrl = true,
        customUrlPref = PrefsConstants.GLM_URL_KEY,
        defaultBaseUrl = "https://open.bigmodel.cn/api/paas/v4/"
    ),
    Gemini(
        id = 1,
        displayName = "Gemini",
        category = AiProviderCategory.INTERNATIONAL,
        protocol = AiProviderProtocol.GOOGLE,
        keyPref = PrefsConstants.GEMINI_KEY,
        modelPref = PrefsConstants.GEMINI_MODEL_KEY,
        defaultModel = "gemini-3.1-pro-preview",
        keyInfoUrl = "https://aistudio.google.com/apikey",
        modelsInfoUrl = "https://ai.google.dev/gemini-api/docs/models"
    ),
    Anthropic(
        id = 3,
        displayName = "Anthropic",
        category = AiProviderCategory.INTERNATIONAL,
        protocol = AiProviderProtocol.ANTHROPIC,
        keyPref = PrefsConstants.ANTHROPIC_KEY,
        modelPref = PrefsConstants.ANTHROPIC_MODEL_KEY,
        defaultModel = "claude-opus-4-6",
        keyInfoUrl = "https://console.anthropic.com/settings/keys",
        modelsInfoUrl = "https://platform.claude.com/docs/en/about-claude/models/overview"
    ),
    OpenRouter(
        id = 4,
        displayName = "OpenRouter",
        category = AiProviderCategory.INTERNATIONAL,
        protocol = AiProviderProtocol.OPENROUTER,
        keyPref = PrefsConstants.OPEN_ROUTER_KEY,
        modelPref = PrefsConstants.OPEN_ROUTER_MODEL_KEY,
        defaultModel = "openrouter/auto",
        keyInfoUrl = "https://openrouter.ai/keys",
        modelsInfoUrl = "https://openrouter.ai/models"
    ),
    Ollama(
        id = 6,
        displayName = "Ollama",
        category = AiProviderCategory.LOCAL,
        protocol = AiProviderProtocol.OLLAMA,
        modelPref = PrefsConstants.OLLAMA_MODEL_KEY,
        defaultModel = "gpt-oss:latest",
        keyInfoUrl = "",
        modelsInfoUrl = "https://ollama.com/library",
        supportsCustomUrl = true,
        requiresCustomUrl = true,
        isLocalProvider = true,
        customUrlPref = PrefsConstants.OLLAMA_URL_KEY,
        defaultBaseUrl = "http://192.168.1.100:11434"
    ),
    LmStudio(
        id = 5,
        displayName = "LM Studio",
        category = AiProviderCategory.LOCAL,
        protocol = AiProviderProtocol.OPENAI_COMPATIBLE,
        modelPref = PrefsConstants.LM_STUDIO_MODEL_KEY,
        defaultModel = "openai/gpt-oss-20b",
        keyInfoUrl = "",
        modelsInfoUrl = "https://lmstudio.ai/models",
        supportsCustomUrl = true,
        requiresCustomUrl = true,
        isLocalProvider = true,
        customUrlPref = PrefsConstants.LM_STUDIO_URL_KEY,
        defaultBaseUrl = "http://192.168.1.100:1234"
    );

    val requiresApiKey: Boolean
        get() = keyPref != null

    companion object {
        val selectable: List<AiProvider> = entries
            .filterNot { it == None }
            .sortedWith(
                compareBy<AiProvider> { it.category.ordinal }
                    .thenBy { it.recommendedRank ?: Int.MAX_VALUE }
                    .thenBy { it.id }
            )
    }
}

fun Int.toAiProvider() = AiProvider.entries.firstOrNull { entry -> entry.id == this } ?: AiProvider.None

val AiProvider.modelPrefsKey: PrefsKey<String>?
    get() = modelPref?.let(::stringPreferencesKey)

val AiProvider.customUrlPrefsKey: PrefsKey<String>?
    get() = customUrlPref?.let(::stringPreferencesKey)

val AiProvider.customUrlEnabledPrefsKey: PrefsKey<Boolean>?
    get() = customUrlEnabledPref?.let(::booleanPreferencesKey)
