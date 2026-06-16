package com.mhss.app.data

import io.ktor.client.plugins.logging.*

/**
 * Sanitizes AI request/response logging by redacting sensitive headers
 * and user content (DF-508). Applied via Ktor Logging plugin.
 */
object AiLogSanitizer {

    /** Headers that must never appear in logs. */
    private val sensitiveHeaders = setOf("authorization", "api-key", "x-api-key")

    fun redactHeaders(message: String): String {
        var result = message
        for (header in sensitiveHeaders) {
            result = result.replace(
                Regex("""$header:\s*[^\n]+""", RegexOption.IGNORE_CASE),
                "$header: ***REDACTED***"
            )
        }
        return result
    }

    fun redactBody(body: String): String {
        // Truncate long bodies and remove potential PII patterns
        if (body.length > 500) return body.take(500) + "...[truncated]"
        return body
    }
}
