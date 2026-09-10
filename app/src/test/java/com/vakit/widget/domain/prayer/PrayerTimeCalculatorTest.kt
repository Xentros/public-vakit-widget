package com.vakit.widget.domain.prayer

import com.vakit.widget.domain.model.DailyPrayerTimes
import com.vakit.widget.domain.model.PrayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PrayerTimeCalculatorTest {

    private val zone = ZoneId.of("Europe/Istanbul")

    private fun day(
        date: LocalDate,
        fajr: String = "05:00",
        sun: String = "06:30",
        dhuhr: String = "13:00",
        asr: String = "16:30",
        maghrib: String = "19:45",
        isha: String = "21:15",
    ): DailyPrayerTimes {
        val times = mapOf(
            PrayerType.IMSAK to LocalTime.parse(fajr),
            PrayerType.SUNRISE to LocalTime.parse(sun),
            PrayerType.DHUHR to LocalTime.parse(dhuhr),
            PrayerType.ASR to LocalTime.parse(asr),
            PrayerType.MAGHRIB to LocalTime.parse(maghrib),
            PrayerType.ISHA to LocalTime.parse(isha),
        )
        return DailyPrayerTimes(date, times)
    }

    private fun zoned(date: LocalDate, time: String): ZonedDateTime =
        ZonedDateTime.of(date, LocalTime.parse(time), zone)

    @Test
    fun `next prayer during the day`() {
        val today = LocalDate.of(2026, 8, 18)
        val days = listOf(day(today))
        val now = zoned(today, "12:00")

        val next = PrayerTimeCalculator.nextPrayer(days, zone, now)

        assertEquals(PrayerType.DHUHR, next?.prayer)
        assertEquals(zoned(today, "13:00"), next?.at)
    }

    @Test
    fun `next prayer after last prayer belongs to the following day`() {
        val today = LocalDate.of(2026, 8, 18)
        val tomorrow = today.plusDays(1)
        val days = listOf(day(today), day(tomorrow))
        val now = zoned(today, "22:00")

        val next = PrayerTimeCalculator.nextPrayer(days, zone, now)

        assertEquals(PrayerType.IMSAK, next?.prayer)
        assertEquals(zoned(tomorrow, "05:00"), next?.at)
    }

    @Test
    fun `now before first prayer returns imsak today`() {
        val today = LocalDate.of(2026, 8, 18)
        val days = listOf(day(today))
        val now = zoned(today, "04:00")

        val next = PrayerTimeCalculator.nextPrayer(days, zone, now)

        assertEquals(PrayerType.IMSAK, next?.prayer)
    }

    @Test
    fun `empty days returns null`() {
        assertNull(PrayerTimeCalculator.nextPrayer(emptyList(), zone, zoned(LocalDate.of(2026, 8, 18), "12:00")))
    }

    @Test
    fun `data only in the past returns null`() {
        val today = LocalDate.of(2026, 8, 18)
        val yesterday = today.minusDays(1)
        val days = listOf(day(yesterday))
        val now = zoned(today, "12:00")

        assertNull(PrayerTimeCalculator.nextPrayer(days, zone, now))
    }

    @Test
    fun `skips days without entries`() {
        val today = LocalDate.of(2026, 8, 18)
        val tomorrow = today.plusDays(1)
        // No data for tomorrow, only for the day after.
        val after = tomorrow.plusDays(1)
        val days = listOf(day(today), day(after))
        val now = zoned(today, "23:00")

        val next = PrayerTimeCalculator.nextPrayer(days, zone, now)

        assertEquals(zoned(after, "05:00"), next?.at)
    }
}