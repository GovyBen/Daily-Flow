package com.mhss.app.preferences

import com.mhss.app.preferences.data.repository.InMemorySecretStore
import com.mhss.app.preferences.domain.model.AiProvider
import com.mhss.app.preferences.domain.model.AiProviderProtocol
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiProviderTest {
    @Test
    fun `existing provider ids remain stable`() {
        assertEquals(1, AiProvider.Gemini.id)
        assertEquals(2, AiProvider.OpenAI.id)
        assertEquals(3, AiProvider.Anthropic.id)
        assertEquals(4, AiProvider.OpenRouter.id)
        assertEquals(5, AiProvider.LmStudio.id)
        assertEquals(6, AiProvider.Ollama.id)
        assertEquals(AiProvider.selectable.size, AiProvider.selectable.map { it.id }.distinct().size)
    }

    @Test
    fun `recommended providers lead the registry`() {
        assertEquals(
            listOf(AiProvider.DeepSeek, AiProvider.OpenAI),
            AiProvider.selectable.take(2)
        )
        assertTrue(AiProvider.DeepSeek.requiresApiKey)
        assertEquals(AiProviderProtocol.OPENAI_COMPATIBLE, AiProvider.DeepSeek.protocol)
        assertFalse(AiProvider.Ollama.requiresApiKey)
    }

    @Test
    fun `in memory secret store supports replacement and removal`() = runBlocking {
        val store = InMemorySecretStore()

        store.set("provider", "first")
        assertEquals("first", store.get("provider"))
        store.set("provider", "second")
        assertEquals("second", store.observe("provider").first())
        store.remove("provider")

        assertEquals("", store.get("provider"))
    }
}
