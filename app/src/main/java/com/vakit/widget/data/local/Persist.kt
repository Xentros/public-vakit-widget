package com.vakit.widget.data.local

import com.vakit.widget.domain.model.DailyPrayerTimes
import com.vakit.widget.domain.model.PrayerCache
import com.vakit.widget.domain.model.PrayerType
import com.vakit.widget.domain.model.SavedCity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
internal data class SavedCityDto(
    val id: String,
    val country: String,
    val city: String,
    val region: String? = null,
    val locationId: Long,
    val label: String? = null,
    val timezoneId: String,
)

@Serializable
internal data class CachedDayDto(
    val date: String,
    val times: Map<String, String>,
)

@Serializable
internal data class PrayerCacheDto(
    val locationId: Long,
    val days: List<CachedDayDto>,
    val lastFetchedAt: Long,
)

object Persist {

    // --- SavedCity ---

    fun citiesToJson(cities: List<SavedCity>): String =
        json.encodeToString(cities.map { it.toDto() })

    fun citiesFromJson(raw: String?): List<SavedCity> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<SavedCityDto>>(raw).map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    private fun SavedCity.toDto() = SavedCityDto(
        id = id,
        country = country,
        city = city,
        region = region,
        locationId = locationId,
        label = label,
        timezoneId = timezoneId,
    )

    private fun SavedCityDto.toDomain() = SavedCity(
        id = id,
        country = country,
        city = city,
        region = region,
        locationId = locationId,
        label = label,
        timezoneId = timezoneId,
    )

    // --- Prayer cache ---

    fun cacheToJson(cache: PrayerCache): String =
        json.encodeToString(
            PrayerCacheDto(
                locationId = cache.locationId,
                days = cache.days.map { day ->
                    CachedDayDto(
                        date = day.date.toString(),
                        times = day.times.entries.associate { (p, t) -> p.apiField to t.toString() },
                    )
                },
                lastFetchedAt = cache.lastFetchedAt.toEpochMilli(),
            )
        )

    fun cacheFromJson(raw: String?): PrayerCache? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val dto = json.decodeFromString<PrayerCacheDto>(raw)
            val days = dto.days.mapNotNull { day ->
                val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@mapNotNull null
                val times = mutableMapOf<PrayerType, LocalTime>()
                day.times.forEach { (field, value) ->
                    val prayer = PrayerType.fromApiField(field) ?: return@forEach
                    val time = runCatching { LocalTime.parse(value) }.getOrNull() ?: return@forEach
                    times[prayer] = time
                }
                if (times.isEmpty()) null else DailyPrayerTimes(date, times)
            }
            PrayerCache(
                locationId = dto.locationId,
                days = days,
                lastFetchedAt = Instant.ofEpochMilli(dto.lastFetchedAt),
            )
        }.getOrNull()
    }
}