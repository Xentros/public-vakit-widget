package com.vakit.widget.data.local

import com.vakit.widget.domain.model.DailyPrayerTimes
import com.vakit.widget.domain.model.PrayerCache
import com.vakit.widget.domain.model.PrayerType
import com.vakit.widget.domain.model.SavedCity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class PersistTest {

    private fun city(label: String? = null) = SavedCity(
        id = "city_9541",
        country = "TÜRKİYE",
        city = "İSTANBUL",
        region = "İSTANBUL",
        locationId = 9541,
        label = label,
        timezoneId = "Europe/Istanbul",
    )

    @Test
    fun `cities round trip`() {
        val cities = listOf(city("My City"), city(label = null))

        val restored = Persist.citiesFromJson(Persist.citiesToJson(cities))

        assertEquals(cities, restored)
    }

    @Test
    fun `corrupted cities json returns empty list`() {
        assertTrue(Persist.citiesFromJson("not json {").isEmpty())
        assertTrue(Persist.citiesFromJson(null).isEmpty())
    }

    @Test
    fun `cache round trip`() {
        val cache = PrayerCache(
            locationId = 9541,
            days = listOf(
                DailyPrayerTimes(
                    date = LocalDate.of(2026, 8, 18),
                    times = mapOf(
                        PrayerType.IMSAK to LocalTime.parse("04:34"),
                        PrayerType.ISHA to LocalTime.parse("21:36"),
                    ),
                )
            ),
            lastFetchedAt = Instant.parse("2026-08-18T10:00:00Z"),
        )

        val restored = Persist.cacheFromJson(Persist.cacheToJson(cache))

        assertEquals(cache, restored)
    }

    @Test
    fun `corrupted cache json returns null`() {
        assertNull(Persist.cacheFromJson("garbage"))
        assertNull(Persist.cacheFromJson(null))
    }

    @Test
    fun `cache ignores unknown prayer fields`() {
        val raw = """
            {"locationId":1,"days":[{"date":"2026-08-18","times":{"fajr":"04:34","unknown":"12:00"}}],"lastFetchedAt":0}
        """.trimIndent()

        val cache = Persist.cacheFromJson(raw)

        assertEquals(listOf(PrayerType.IMSAK), cache?.days?.first()?.times?.keys?.toList())
    }
}