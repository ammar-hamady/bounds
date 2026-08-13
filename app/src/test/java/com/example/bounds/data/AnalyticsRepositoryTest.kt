package com.example.bounds.data

import com.example.bounds.model.AnalyticsEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AnalyticsEvent serialization/deserialization logic used by AnalyticsRepository.
 *
 * AnalyticsRepository stores events as JSON via DataStore (which requires Android context and
 * cannot run on the JVM). These tests cover the Gson round-trip that is the core
 * persistence logic, verifying that every AnalyticsEvent field survives a write → read cycle,
 * and that append/clear patterns behave correctly.
 */
class AnalyticsRepositoryTest {

    private lateinit var gson: Gson
    private val eventsType = object : TypeToken<List<AnalyticsEvent>>() {}.type

    @Before
    fun setUp() {
        gson = Gson()
    }

    // ---------------------------------------------------------------------------
    // Helpers: mirror the read/write paths in AnalyticsRepository
    // ---------------------------------------------------------------------------

    private fun deserialize(json: String): List<AnalyticsEvent> =
        runCatching {
            gson.fromJson<List<AnalyticsEvent>>(json, eventsType) ?: emptyList()
        }.getOrDefault(emptyList())

    private fun serialize(events: List<AnalyticsEvent>): String = gson.toJson(events)

    /** Mirrors the append pattern a ViewModel would use before calling saveEvents. */
    private fun appendEvent(existing: List<AnalyticsEvent>, event: AnalyticsEvent): List<AnalyticsEvent> =
        existing + event

    /** Mirrors the clear pattern (save an empty list). */
    private fun clear(): List<AnalyticsEvent> = emptyList()

    // ---------------------------------------------------------------------------
    // Basic round-trip
    // ---------------------------------------------------------------------------

    @Test
    fun `single event with all fields round-trips correctly`() {
        val original = AnalyticsEvent(
            id = "evt-001",
            appName = "Twitter",
            zoneName = "Library",
            durationMinutes = 42,
            timestampMs = 1_700_000_000_000L
        )

        val json = serialize(listOf(original))
        val restored = deserialize(json)

        assertEquals(1, restored.size)
        assertEquals(original, restored[0])
    }

    @Test
    fun `multiple events round-trip correctly`() {
        val events = listOf(
            AnalyticsEvent("e1", "Instagram", "Home",  5, 1_700_000_001_000L),
            AnalyticsEvent("e2", "TikTok",    "Work",  13, 1_700_000_002_000L),
            AnalyticsEvent("e3", "YouTube",   "Gym",   30, 1_700_000_003_000L)
        )

        val json = serialize(events)
        val restored = deserialize(json)

        assertEquals(events.size, restored.size)
        events.forEachIndexed { i, event -> assertEquals(event, restored[i]) }
    }

    @Test
    fun `empty event list serializes and deserializes to empty list`() {
        val json = serialize(emptyList())
        val restored = deserialize(json)
        assertTrue(restored.isEmpty())
    }

    // ---------------------------------------------------------------------------
    // Individual field preservation
    // ---------------------------------------------------------------------------

    @Test
    fun `id field is preserved`() {
        val event = AnalyticsEvent("unique-id-xyz", "App", "Zone", 1, 0L)
        assertEquals("unique-id-xyz", deserialize(serialize(listOf(event)))[0].id)
    }

    @Test
    fun `appName is preserved`() {
        val event = AnalyticsEvent("e", "com.example.myapp", "Zone", 1, 0L)
        assertEquals("com.example.myapp", deserialize(serialize(listOf(event)))[0].appName)
    }

    @Test
    fun `zoneName is preserved`() {
        val event = AnalyticsEvent("e", "App", "My Special Zone", 1, 0L)
        assertEquals("My Special Zone", deserialize(serialize(listOf(event)))[0].zoneName)
    }

    @Test
    fun `durationMinutes zero is preserved`() {
        val event = AnalyticsEvent("e", "App", "Zone", 0, 0L)
        assertEquals(0, deserialize(serialize(listOf(event)))[0].durationMinutes)
    }

    @Test
    fun `durationMinutes large value is preserved`() {
        val event = AnalyticsEvent("e", "App", "Zone", Int.MAX_VALUE, 0L)
        assertEquals(Int.MAX_VALUE, deserialize(serialize(listOf(event)))[0].durationMinutes)
    }

    @Test
    fun `timestampMs zero is preserved`() {
        val event = AnalyticsEvent("e", "App", "Zone", 1, 0L)
        assertEquals(0L, deserialize(serialize(listOf(event)))[0].timestampMs)
    }

    @Test
    fun `timestampMs large epoch value is preserved`() {
        val ts = 9_999_999_999_999L
        val event = AnalyticsEvent("e", "App", "Zone", 1, ts)
        assertEquals(ts, deserialize(serialize(listOf(event)))[0].timestampMs)
    }

    // ---------------------------------------------------------------------------
    // Append pattern
    // ---------------------------------------------------------------------------

    @Test
    fun `appending an event to an empty list persists correctly`() {
        val initial = deserialize("[]")
        val event = AnalyticsEvent("e1", "App", "Zone", 5, 1000L)
        val updated = appendEvent(initial, event)
        val json = serialize(updated)
        val restored = deserialize(json)

        assertEquals(1, restored.size)
        assertEquals(event, restored[0])
    }

    @Test
    fun `appending events sequentially accumulates correctly`() {
        var events = deserialize("[]")
        val toAppend = listOf(
            AnalyticsEvent("e1", "Twitter",   "Library", 10, 1000L),
            AnalyticsEvent("e2", "Instagram", "Library", 5,  2000L),
            AnalyticsEvent("e3", "TikTok",    "Library", 20, 3000L)
        )

        for (event in toAppend) {
            events = appendEvent(events, event)
        }

        val restored = deserialize(serialize(events))

        assertEquals(3, restored.size)
        toAppend.forEachIndexed { i, event -> assertEquals(event, restored[i]) }
    }

    @Test
    fun `order of events is preserved after multiple appends`() {
        val events = (1..5).map { i ->
            AnalyticsEvent("e$i", "App$i", "Zone", i, i * 1000L)
        }
        val json = serialize(events)
        val restored = deserialize(json)
        assertEquals(events.map { it.id }, restored.map { it.id })
    }

    // ---------------------------------------------------------------------------
    // Clear pattern
    // ---------------------------------------------------------------------------

    @Test
    fun `clearing events results in empty list`() {
        val events = listOf(
            AnalyticsEvent("e1", "Twitter", "Home", 5, 1000L),
            AnalyticsEvent("e2", "YouTube", "Home", 10, 2000L)
        )
        val json = serialize(events)
        // Verify events exist before clearing
        assertEquals(2, deserialize(json).size)

        // Clear
        val clearedJson = serialize(clear())
        val afterClear = deserialize(clearedJson)
        assertTrue(afterClear.isEmpty())
    }

    @Test
    fun `events can be appended again after clearing`() {
        // Populate, clear, re-populate
        val first = AnalyticsEvent("e1", "Twitter", "Zone", 5, 1000L)
        val second = AnalyticsEvent("e2", "Instagram", "Zone", 3, 2000L)

        var events = listOf(first)
        events = clear()
        events = appendEvent(events, second)

        val restored = deserialize(serialize(events))
        assertEquals(1, restored.size)
        assertEquals(second, restored[0])
    }

    // ---------------------------------------------------------------------------
    // Resilience
    // ---------------------------------------------------------------------------

    @Test
    fun `empty JSON string falls back to empty list`() {
        val restored = deserialize("[]")
        assertTrue(restored.isEmpty())
    }

    @Test
    fun `malformed JSON falls back to empty list without throwing`() {
        val restored = deserialize("NOT_VALID_JSON")
        assertTrue(restored.isEmpty())
    }

    @Test
    fun `null result from gson falls back to empty list`() {
        // Gson can return null for "null" literal
        val restored = deserialize("null")
        assertTrue(restored.isEmpty())
    }

    @Test
    fun `events with special characters in strings round-trip correctly`() {
        val event = AnalyticsEvent(
            id = "e1",
            appName = "App \"with\" quotes & slashes/",
            zoneName = "Zone\nwith\nnewlines",
            durationMinutes = 1,
            timestampMs = 0L
        )
        val restored = deserialize(serialize(listOf(event)))[0]
        assertEquals(event.appName, restored.appName)
        assertEquals(event.zoneName, restored.zoneName)
    }
}
