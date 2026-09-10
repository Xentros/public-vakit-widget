package com.vakit.widget.data.provider

import com.vakit.widget.data.api.DiyanetApi
import com.vakit.widget.data.api.LocationDto
import com.vakit.widget.data.api.PrayerTimesDto
import com.vakit.widget.domain.model.City
import com.vakit.widget.domain.model.DailyPrayerTimes
import com.vakit.widget.domain.model.PrayerType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DiyanetPrayerTimeProvider(
    private val api: DiyanetApi,
) : PrayerTimeProvider {

    override val id: String = "diyanet"

    override suspend fun getCountries(): List<String> = api.getCountries()

    override suspend fun getCities(country: String): List<String> = api.getCities(country)

    override suspend fun getLocations(country: String, city: String): List<City> =
        api.getLocations(country, city).map { it.toDomain() }

    override suspend fun search(query: String): List<City> =
        api.search(query).map { it.toDomain() }

    override suspend fun getPrayerTimes(locationId: Long): List<DailyPrayerTimes> {
        val raw = api.getPrayerTimes(locationId)
        return raw.mapNotNull { it.toDomain() }
    }

    private fun LocationDto.toDomain(): City = City(
        locationId = id,
        country = country,
        city = city,
        region = region,
    )

    /**
     * Converts an API day into domain prayer times. Returns null for malformed
     * entries (unparseable date or time) so that only valid data is cached.
     */
    private fun PrayerTimesDto.toDomain(): DailyPrayerTimes? {
        val date = runCatching { LocalDateTime.parse(date).toLocalDate() }
            .getOrElse { runCatching { LocalDate.parse(date) }.getOrNull() }
            ?: return null

        val times = mutableMapOf<PrayerType, LocalTime>()
        for (prayer in PrayerType.entries) {
            val rawTime = when (prayer) {
                PrayerType.IMSAK -> fajr
                PrayerType.SUNRISE -> sun
                PrayerType.DHUHR -> dhuhr
                PrayerType.ASR -> asr
                PrayerType.MAGHRIB -> maghrib
                PrayerType.ISHA -> isha
            }
            if (rawTime.isBlank()) continue
            val parsed = runCatching { LocalTime.parse(rawTime) }.getOrNull() ?: continue
            times[prayer] = parsed
        }
        if (times.isEmpty()) return null
        return DailyPrayerTimes(date = date, times = times)
    }
}