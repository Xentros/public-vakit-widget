package com.vakit.widget.domain.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The six prayer entries supported by version 1.
 * The [apiField] values map to the Diyanet API response fields.
 * The order of the enum is the display order.
 */
enum class PrayerType(val apiField: String) {
    IMSAK("fajr"),
    SUNRISE("sun"),
    DHUHR("dhuhr"),
    ASR("asr"),
    MAGHRIB("maghrib"),
    ISHA("isha");

    companion object {
        fun fromApiField(field: String): PrayerType? = entries.firstOrNull { it.apiField == field }
    }
}

/** A location as returned by the prayer-time provider. */
data class City(
    val locationId: Long,
    val country: String,
    val city: String,
    val region: String? = null,
)

/** A user-saved city. */
data class SavedCity(
    val id: String,
    val country: String,
    val city: String,
    val region: String? = null,
    val locationId: Long,
    val label: String? = null,
    val timezoneId: String,
) {
    val displayName: String get() = label ?: city

    val timezone: ZoneId
        get() = runCatching { ZoneId.of(timezoneId) }.getOrDefault(ZoneId.systemDefault())
}

/** Prayer times for a single date in the location's local time. */
data class DailyPrayerTimes(
    val date: LocalDate,
    val times: Map<PrayerType, LocalTime>,
)

/** Cached prayer data for a single location. */
data class PrayerCache(
    val locationId: Long,
    val days: List<DailyPrayerTimes>,
    val lastFetchedAt: java.time.Instant,
)

const val PROVIDER_DIYANET = "diyanet"

const val MAX_SAVED_CITIES = 5

data class AppSettings(
    val savedCities: List<SavedCity> = emptyList(),
    val activeCityId: String? = null,
    val provider: String = PROVIDER_DIYANET,
    val locale: String = "",
    val alarmsEnabled: Boolean = false,
    val enabledAlarmPrayers: Set<PrayerType> = PrayerType.entries.toSet(),
    val alarmSoundUri: String? = null,
    val widgetBackgroundColor: Long = DEFAULT_WIDGET_BG_COLOR,
    val widgetBackgroundOpacity: Int = DEFAULT_WIDGET_BG_OPACITY,
    val widgetForegroundColor: Long = DEFAULT_WIDGET_FG_COLOR,
) {
    val activeCity: SavedCity?
        get() = savedCities.firstOrNull { it.id == activeCityId } ?: savedCities.firstOrNull()
}

const val DEFAULT_WIDGET_BG_COLOR: Long = 0xFF1A1A1A // near-black, not hard-coded black
const val DEFAULT_WIDGET_BG_OPACITY: Int = 55 // percent
const val DEFAULT_WIDGET_FG_COLOR: Long = 0xFFFFFFFF
