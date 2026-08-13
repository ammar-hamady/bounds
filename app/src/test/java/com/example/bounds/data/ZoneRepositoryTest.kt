package com.example.bounds.data

import com.example.bounds.model.Zone
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Zone serialization/deserialization logic used by ZoneRepository.
 *
 * ZoneRepository stores zones as JSON via DataStore (which requires Android context and
 * cannot run on the JVM). These tests cover the Gson round-trip that is the core
 * persistence logic, verifying that every Zone field survives a write → read cycle.
 */
class ZoneRepositoryTest {

    private lateinit var gson: Gson
    private val zonesType = object : TypeToken<List<Zone>>() {}.type

    @Before
    fun setUp() {
        gson = Gson()
    }

    // ---------------------------------------------------------------------------
    // Helper: mirrors the read path in ZoneRepository.zonesFlow
    // ---------------------------------------------------------------------------
    private fun deserialize(json: String): List<Zone> =
        runCatching {
            gson.fromJson<List<Zone>>(json, zonesType) ?: emptyList()
        }.getOrDefault(emptyList())

    // ---------------------------------------------------------------------------
    // Helper: mirrors the write path in ZoneRepository.saveZones
    // ---------------------------------------------------------------------------
    private fun serialize(zones: List<Zone>): String = gson.toJson(zones)

    // ---------------------------------------------------------------------------
    // Basic round-trip
    // ---------------------------------------------------------------------------

    @Test
    fun `single zone with all explicit fields round-trips correctly`() {
        val original = Zone(
            id = "zone-1",
            name = "Library",
            isEnabled = true,
            isTimeSensitive = false,
            startTime = "22:00",
            endTime = "07:00",
            blockedApps = listOf("com.twitter.android", "com.instagram.android"),
            latitude = 51.5074,
            longitude = -0.1278,
            radiusMeters = 100
        )

        val json = serialize(listOf(original))
        val restored = deserialize(json)

        assertEquals(1, restored.size)
        assertEquals(original, restored[0])
    }

    @Test
    fun `multiple zones round-trip correctly`() {
        val zones = listOf(
            Zone(id = "a", name = "Home", latitude = 40.7128, longitude = -74.0060, radiusMeters = 50),
            Zone(id = "b", name = "Work", latitude = 37.7749, longitude = -122.4194, radiusMeters = 200),
            Zone(id = "c", name = "Gym", latitude = 48.8566, longitude = 2.3522, radiusMeters = 75)
        )

        val json = serialize(zones)
        val restored = deserialize(json)

        assertEquals(zones.size, restored.size)
        zones.forEachIndexed { i, zone -> assertEquals(zone, restored[i]) }
    }

    @Test
    fun `empty zone list serializes and deserializes to empty list`() {
        val json = serialize(emptyList())
        val restored = deserialize(json)
        assertTrue(restored.isEmpty())
    }

    // ---------------------------------------------------------------------------
    // Default field values
    // ---------------------------------------------------------------------------

    @Test
    fun `zone with only required fields uses correct defaults after round-trip`() {
        val original = Zone(id = "minimal", name = "Minimal Zone")

        val json = serialize(listOf(original))
        val restored = deserialize(json)

        assertEquals(1, restored.size)
        val z = restored[0]
        assertEquals("minimal", z.id)
        assertEquals("Minimal Zone", z.name)
        assertTrue("isEnabled should default true", z.isEnabled)
        assertEquals(false, z.isTimeSensitive)
        assertEquals("22:00", z.startTime)
        assertEquals("07:00", z.endTime)
        assertTrue("blockedApps should default empty", z.blockedApps.isEmpty())
        assertEquals(0.0, z.latitude, 0.0)
        assertEquals(0.0, z.longitude, 0.0)
        assertEquals(50, z.radiusMeters)
    }

    @Test
    fun `isEnabled false is preserved through round-trip`() {
        val original = Zone(id = "z", name = "Disabled Zone", isEnabled = false)
        val restored = deserialize(serialize(listOf(original)))
        assertEquals(false, restored[0].isEnabled)
    }

    @Test
    fun `isTimeSensitive true is preserved through round-trip`() {
        val original = Zone(id = "z", name = "Time Zone", isTimeSensitive = true, startTime = "08:00", endTime = "17:00")
        val restored = deserialize(serialize(listOf(original)))
        val z = restored[0]
        assertEquals(true, z.isTimeSensitive)
        assertEquals("08:00", z.startTime)
        assertEquals("17:00", z.endTime)
    }

    // ---------------------------------------------------------------------------
    // Numeric precision
    // ---------------------------------------------------------------------------

    @Test
    fun `latitude and longitude are preserved with full precision`() {
        val original = Zone(id = "z", name = "Precise", latitude = 51.507351, longitude = -0.127758)
        val restored = deserialize(serialize(listOf(original)))
        assertEquals(51.507351, restored[0].latitude, 1e-9)
        assertEquals(-0.127758, restored[0].longitude, 1e-9)
    }

    @Test
    fun `large radiusMeters value round-trips correctly`() {
        val original = Zone(id = "z", name = "Wide", radiusMeters = 10_000)
        val restored = deserialize(serialize(listOf(original)))
        assertEquals(10_000, restored[0].radiusMeters)
    }

    // ---------------------------------------------------------------------------
    // blockedApps list
    // ---------------------------------------------------------------------------

    @Test
    fun `blockedApps list with multiple entries round-trips correctly`() {
        val apps = listOf("com.twitter.android", "com.instagram.android", "com.tiktok.android")
        val original = Zone(id = "z", name = "Focus", blockedApps = apps)
        val restored = deserialize(serialize(listOf(original)))
        assertEquals(apps, restored[0].blockedApps)
    }

    @Test
    fun `blockedApps empty list is preserved`() {
        val original = Zone(id = "z", name = "No Block", blockedApps = emptyList())
        val restored = deserialize(serialize(listOf(original)))
        assertTrue(restored[0].blockedApps.isEmpty())
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
    fun `missing optional fields in JSON deserialize with JVM defaults not Kotlin defaults`() {
        // Gson bypasses Kotlin constructors and uses JVM primitive defaults for absent fields.
        // isEnabled → false (JVM Boolean default, NOT Kotlin's `true`)
        // blockedApps → null (Gson does not call the constructor; the field is unset)
        // radiusMeters → 0 (JVM int default, NOT Kotlin's 50)
        // This test documents the actual Gson behaviour so a future developer isn't surprised.
        val partialJson = """[{"id":"z1","name":"Old Zone"}]"""
        val restored = deserialize(partialJson)
        assertEquals(1, restored.size)
        val z = restored[0]
        assertEquals("z1", z.id)
        assertEquals("Old Zone", z.name)
        // Required fields that ARE present survive correctly
        // Absent fields fall back to JVM defaults, not Kotlin parameter defaults
        assertEquals(false, z.isEnabled)   // JVM default for Boolean
        assertEquals(0, z.radiusMeters)    // JVM default for Int
        assertEquals(0.0, z.latitude, 0.0)
        assertEquals(0.0, z.longitude, 0.0)
    }

    @Test
    fun `order of zones is preserved after round-trip`() {
        val zones = (1..5).map { i -> Zone(id = "z$i", name = "Zone $i") }
        val restored = deserialize(serialize(zones))
        assertEquals(zones.map { it.id }, restored.map { it.id })
    }
}
