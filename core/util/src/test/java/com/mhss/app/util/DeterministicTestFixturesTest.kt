package com.mhss.app.util

import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

internal object TestClock {
    val now: Instant = Instant.parse("2026-01-15T08:00:00Z")
}

internal val TestZone: TimeZone = TimeZone.of("Asia/Shanghai")

internal class TestIdGenerator {
    private var nextValue = 1

    fun nextId(): String = "test-id-${nextValue++}"
}

class DeterministicTestFixturesTest {
    @Test
    fun fixturesRemainStable() {
        val ids = TestIdGenerator()

        assertEquals("2026-01-15T08:00:00Z", TestClock.now.toString())
        assertEquals("Asia/Shanghai", TestZone.id)
        assertEquals("test-id-1", ids.nextId())
        assertEquals("test-id-2", ids.nextId())
    }
}
