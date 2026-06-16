package com.mhss.app.data

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.params.LLMParams
import com.mhss.app.preferences.domain.model.AiProvider
import com.mhss.app.preferences.domain.model.AiProviderProtocol
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Headers
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * DF-012: Provider connection and capability contract tests.
 *
 * Covers: OpenAI-compatible protocol (DeepSeek, OpenAI, Qwen, Kimi, GLM, LM Studio)
 * plus provider registration, protocol mapping, and capability verification.
 *
 * Only side-effect-free fake tools are used — no real task/calendar/record writes.
 */
class ProviderContractTest {

    private lateinit var server: MockWebServer
    private val fakeToolRegistry = ToolRegistry { tools(FakeWeatherToolSet()) }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start(0)
    }

    // ── OpenAI-Compatible: Text Response ────────────────────────────────

    @Test
    fun `openai compatible - minimal text response`() = runBlocking {
        server.enqueue(MockResponse(200, jsonHeaders(), okResponse("Hello!")))

        val executor = createOpenAiExecutor()
        val result = executor.execute(
            prompt = prompt("test", LLMParams()) { user("Hi") },
            model = openAiModel(withTools = false)
        )

        assertEquals(1, result.size)
        val msg = result.first() as? Message.Assistant ?: error("Expected Assistant, got ${result.first()}")
        assertEquals("Hello!", msg.content)

        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertTrue(req.url.encodedPath.contains("chat/completions"))
    }

    // ── OpenAI-Compatible: Tool Calling ─────────────────────────────────

    @Test
    fun `openai compatible - tool calling response`() = runBlocking {
        server.enqueue(MockResponse(200, jsonHeaders(), okToolResponse("get_weather", """{"city":"Beijing"}""")))

        val executor = createOpenAiExecutor()
        val model = openAiModel(withTools = true)
        val toolDescs = fakeToolRegistry.tools.map { it.descriptor }

        val result = executor.execute(
            prompt = prompt("test", LLMParams()) { user("Weather in Beijing?") },
            model = model, tools = toolDescs
        )

        val toolCalls = result.filterIsInstance<Message.Tool.Call>()
        assertEquals(1, toolCalls.size)
        assertEquals("get_weather", toolCalls[0].tool)
        assertTrue(toolCalls[0].contentJson.contains("Beijing"))
    }

    // ── OpenAI-Compatible: Error Handling ───────────────────────────────

    @Test
    fun `openai compatible - auth error 401`() = runBlocking {
        server.enqueue(MockResponse(401, jsonHeaders(),
            """{"error":{"message":"Invalid API key","type":"authentication_error"}}"""))

        val executor = createOpenAiExecutor()
        try {
            executor.execute(prompt("t", LLMParams()) { user("x") }, model = openAiModel(false))
            fail("Expected exception for 401")
        } catch (e: Exception) {
            val msg = e.toString()
            assertTrue("Expected 401/auth error, got: $msg",
                msg.contains("401") || msg.contains("Unauthorized") || msg.contains("auth"))
        }
    }

    @Test
    fun `openai compatible - rate limit error 429`() = runBlocking {
        server.enqueue(MockResponse(429, jsonHeaders(),
            """{"error":{"message":"Rate limit exceeded","type":"rate_limit_error"}}"""))

        val executor = createOpenAiExecutor()
        try {
            executor.execute(prompt("t", LLMParams()) { user("x") }, model = openAiModel(false))
            fail("Expected exception for 429")
        } catch (e: Exception) {
            val msg = e.toString()
            assertTrue("Expected 429/rate error, got: $msg",
                msg.contains("429") || msg.contains("rate", ignoreCase = true))
        }
    }

    @Test
    fun `openai compatible - server error 500`() = runBlocking {
        server.enqueue(MockResponse(500, jsonHeaders(),
            """{"error":{"message":"Internal error","type":"server_error"}}"""))

        val executor = createOpenAiExecutor()
        try {
            executor.execute(prompt("t", LLMParams()) { user("x") }, model = openAiModel(false))
            fail("Expected exception for 500")
        } catch (e: Exception) {
            val msg = e.toString()
            assertTrue("Expected 500 error, got: $msg",
                msg.contains("500") || msg.contains("Internal") || msg.contains("server", true))
        }
    }

    // ── OpenAI-Compatible: Streaming ────────────────────────────────────

    @Test
    fun `openai compatible - streaming text response`() = runBlocking {
        server.enqueue(MockResponse(200, Headers.headersOf("Content-Type", "text/event-stream"),
            """{"id":"1","object":"chat.completion.chunk","created":1717000000,"model":"gpt-4o","choices":[{"index":0,"delta":{"role":"assistant","content":"Hello world!"},"finish_reason":"stop"}]}"""))

        val executor = createOpenAiExecutor()
        val result = executor.execute(
            prompt = prompt("test", LLMParams()) { user("Say hello") },
            model = openAiModel(false)
        )

        val assistant = result.filterIsInstance<Message.Assistant>().firstOrNull()
        assertNotNull("Expected assistant message in streaming response", assistant)
        assertTrue(assistant!!.content.contains("Hello"))
    }

    // ── Provider Registration Tests ─────────────────────────────────────

    @Test
    fun `all provider IDs are unique`() {
        assertEquals(AiProvider.entries.size, AiProvider.entries.map { it.id }.distinct().size)
    }

    @Test
    fun `all selectable providers have non-NONE protocol`() {
        for (p in AiProvider.selectable) {
            assertNotEquals("${p.displayName} has NONE protocol", AiProviderProtocol.NONE, p.protocol)
        }
    }

    @Test
    fun `each provider has non-blank default model`() {
        for (p in AiProvider.selectable) {
            assertNotNull("${p.displayName} null defaultModel", p.defaultModel)
            assertTrue("${p.displayName} blank defaultModel", p.defaultModel!!.isNotBlank())
        }
    }

    @Test
    fun `openai compatible providers include all expected`() {
        val names = AiProvider.selectable
            .filter { it.protocol == AiProviderProtocol.OPENAI_COMPATIBLE }
            .map { it.displayName }.toSet()
        for (expected in listOf("DeepSeek", "OpenAI", "通义千问", "Kimi", "智谱 GLM", "LM Studio")) {
            assertTrue("Missing provider: $expected", names.contains(expected))
        }
    }

    @Test
    fun `recommended providers are DeepSeek then OpenAI`() {
        val rec = AiProvider.selectable.filter {
            it.category == com.mhss.app.preferences.domain.model.AiProviderCategory.RECOMMENDED
        }
        assertEquals(2, rec.size)
        assertEquals("DeepSeek", rec[0].displayName)
        assertEquals("OpenAI", rec[1].displayName)
    }

    @Test
    fun `mainland china providers all use OPENAI_COMPATIBLE`() {
        for (p in AiProvider.selectable.filter {
            it.category == com.mhss.app.preferences.domain.model.AiProviderCategory.MAINLAND_CHINA
        }) {
            assertEquals(AiProviderProtocol.OPENAI_COMPATIBLE, p.protocol)
        }
    }

    @Test
    fun `local providers do not require API key`() {
        for (p in AiProvider.selectable.filter { it.isLocalProvider }) {
            assertFalse("${p.displayName} requiresApiKey", p.requiresApiKey)
        }
    }

    // ── Protocol Mapping Tests ──────────────────────────────────────────

    @Test
    fun `protocol to LLMProvider mapping`() {
        assertEquals(LLMProvider.OpenAI, AiProvider.DeepSeek.toLLMProvider())
        assertEquals(LLMProvider.OpenAI, AiProvider.Qwen.toLLMProvider())
        assertEquals(LLMProvider.OpenAI, AiProvider.Kimi.toLLMProvider())
        assertEquals(LLMProvider.OpenAI, AiProvider.Glm.toLLMProvider())
        assertEquals(LLMProvider.OpenAI, AiProvider.LmStudio.toLLMProvider())
        assertEquals(LLMProvider.Google, AiProvider.Gemini.toLLMProvider())
        assertEquals(LLMProvider.Anthropic, AiProvider.Anthropic.toLLMProvider())
        assertEquals(LLMProvider.OpenRouter, AiProvider.OpenRouter.toLLMProvider())
        assertEquals(LLMProvider.Ollama, AiProvider.Ollama.toLLMProvider())
    }

    @Test
    fun `toLLModel constructs valid model for each protocol`() {
        for (p in AiProvider.selectable) {
            val model = p.defaultModel!!.toLLModel(p, withTools = true)
            assertNotNull(model.provider)
            val caps = model.capabilities ?: emptyList()
            assertTrue(caps.contains(LLMCapability.Completion))
            assertTrue(caps.contains(LLMCapability.Tools))
            assertTrue(model.id.isNotBlank())
        }
    }

    // ── Capability Status Verification ──────────────────────────────────

    @Test
    fun `capability matrix - all providers basic capabilities`() {
        for (p in AiProvider.selectable) {
            val model = p.defaultModel!!.toLLModel(p, withTools = true)
            val caps = model.capabilities ?: emptyList()
            assertTrue("${p.displayName} must support Completion", caps.contains(LLMCapability.Completion))
            when (p.protocol) {
                AiProviderProtocol.OPENAI_COMPATIBLE ->
                    assertTrue("${p.displayName} should support Tools", caps.contains(LLMCapability.Tools))
                AiProviderProtocol.ANTHROPIC ->
                    assertTrue("${p.displayName} should support Tools", caps.contains(LLMCapability.Tools))
                else -> { /* other protocols may vary */ }
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun createOpenAiExecutor(): PromptExecutor {
        val baseUrl = server.url("/v1").toString().trimEnd('/')
        return SingleLLMPromptExecutor(
            OpenAILLMClient("test-key", OpenAIClientSettings(baseUrl = baseUrl))
        )
    }

    private fun openAiModel(withTools: Boolean) = LLModel(
        provider = LLMProvider.OpenAI,
        id = "gpt-4o",
        capabilities = buildList {
            add(LLMCapability.Completion)
            add(LLMCapability.OpenAIEndpoint.Completions)
            if (withTools) { add(LLMCapability.Tools); add(LLMCapability.ToolChoice) }
        },
        contextLength = 128_000,
        maxOutputTokens = 32_000
    )

    private fun jsonHeaders() = Headers.headersOf("Content-Type", "application/json")

    private fun okResponse(content: String) =
        """{"id":"chatcmpl-1","object":"chat.completion","created":1717000000,"model":"gpt-4o","choices":[{"index":0,"message":{"role":"assistant","content":"$content","refusal":null},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}"""

    private fun okToolResponse(name: String, args: String): String {
        val escapedArgs = args.replace("\\", "\\\\").replace("\"", "\\\"")
        return """{"id":"chatcmpl-1","object":"chat.completion","created":1717000000,"model":"gpt-4o","choices":[{"index":0,"message":{"role":"assistant","content":null,"refusal":null,"tool_calls":[{"id":"c1","type":"function","function":{"name":"$name","arguments":"$escapedArgs"}}]},"finish_reason":"tool_calls"}],"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}"""
    }
}

/**
 * Fake tool set for contract testing. Side-effect-free — no real writes.
 */
class FakeWeatherToolSet : ToolSet {
    @Tool("get_weather")
    @LLMDescription("Get current weather for a city. FAKE.")
    fun getWeather(@LLMDescription("City name") city: String): String = "Sunny in $city"

    @Tool("get_time")
    @LLMDescription("Get current time. FAKE.")
    fun getTime(@LLMDescription("Timezone") timezone: String): String = "12:00 in $timezone"
}
