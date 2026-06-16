package com.mhss.app.domain

import kotlinx.datetime.*

/**
 * Resolves ambiguous natural-language date/time expressions (DF-505).
 * Uses device time zone for deterministic calculation.
 */
object DateAmbiguityResolver {

    fun resolveRelativeDay(expression: String): DateResolution {
        val tz = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val today = now.toLocalDateTime(tz).date

        return when (expression.lowercase().trim()) {
            "today" -> DateResolution.Resolved(
                today.atTime(0, 0).toInstant(tz).toEpochMilliseconds()
            )
            "tomorrow" -> DateResolution.Resolved(
                today.plus(1, DateTimeUnit.DAY).atTime(0, 0).toInstant(tz).toEpochMilliseconds()
            )
            else -> tryResolveWeekday(expression, today)
        }
    }

    fun resolveTimeOfDay(timeExpression: String, baseDateEpochMilli: Long? = null): DateResolution {
        val tz = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(tz)
        val parsed = parseTimeString(timeExpression)
            ?: return DateResolution.Clarification("parse_time_failure", "Cannot understand time: $timeExpression")

        val baseDate = if (baseDateEpochMilli != null) {
            Instant.fromEpochMilliseconds(baseDateEpochMilli).toLocalDateTime(tz).date
        } else now.date
        val candidate = LocalDateTime(baseDate, parsed)
        val millis = candidate.toInstant(tz).toEpochMilliseconds()
        return DateResolution.Resolved(millis)
    }

    fun combineDateAndTime(dateResult: DateResolution, timeResult: DateResolution): DateResolution {
        if (dateResult is DateResolution.Clarification) return dateResult
        if (timeResult is DateResolution.Clarification) return timeResult
        val tz = TimeZone.currentSystemDefault()
        val dateMillis = (dateResult as DateResolution.Resolved).epochMilli
        val baseDate = Instant.fromEpochMilliseconds(dateMillis).toLocalDateTime(tz).date
        val time = Instant.fromEpochMilliseconds(
            (timeResult as DateResolution.Resolved).epochMilli
        ).toLocalDateTime(tz).time
        return DateResolution.Resolved(LocalDateTime(baseDate, time).toInstant(tz).toEpochMilliseconds())
    }

    fun checkCompleteness(dateExpression: String?, timeExpression: String?, missingField: String): DateResolution? {
        if (dateExpression == null && timeExpression == null)
            return DateResolution.Clarification(missingField, "When should this be?")
        if (dateExpression != null && timeExpression == null)
            return DateResolution.Clarification(missingField, "What time?")
        return null
    }

    private fun tryResolveWeekday(expression: String, today: LocalDate): DateResolution {
        val tz = TimeZone.currentSystemDefault()
        val target = when (expression.lowercase().trim()) {
            "monday","mon" -> DayOfWeek.MONDAY; "tuesday","tue" -> DayOfWeek.TUESDAY
            "wednesday","wed" -> DayOfWeek.WEDNESDAY; "thursday","thu" -> DayOfWeek.THURSDAY
            "friday","fri" -> DayOfWeek.FRIDAY; "saturday","sat" -> DayOfWeek.SATURDAY
            "sunday","sun" -> DayOfWeek.SUNDAY
            else -> return tryResolveWeekday(expression.removePrefix("next ").trim(), today)
        }
        var daysUntil = target.ordinal - today.dayOfWeek.ordinal
        if (daysUntil <= 0) daysUntil += 7
        return DateResolution.Resolved(
            today.plus(daysUntil, DateTimeUnit.DAY).atTime(0, 0).toInstant(tz).toEpochMilliseconds()
        )
    }

    private fun parseTimeString(input: String): LocalTime? {
        val cleaned = input.trim().lowercase()
        try {
            val parts = cleaned.split(":")
            if (parts.size == 2 && parts[0].toInt() in 0..23 && parts[1].toInt() in 0..59)
                return LocalTime(parts[0].toInt(), parts[1].toInt())
        } catch (_: Exception) {}
        val match = Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm)""").find(cleaned)
        if (match != null) {
            var hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].takeIf { it.isNotEmpty() }?.toInt() ?: 0
            if (match.groupValues[3] == "pm" && hour != 12) hour += 12
            if (match.groupValues[3] == "am" && hour == 12) hour = 0
            if (hour in 0..23 && minute in 0..59) return LocalTime(hour, minute)
        }
        return null
    }

    sealed class DateResolution {
        data class Resolved(val epochMilli: Long) : DateResolution()
        data class Clarification(val field: String, val question: String) : DateResolution()
    }
}
