package com.vakit.widget.data.provider

import com.vakit.widget.domain.model.City
import com.vakit.widget.domain.model.DailyPrayerTimes

/**
 * Abstraction over a prayer-time provider.
 *
 * The rest of the application only depends on this interface, so additional
 * providers (e.g. Muslim Pro, Aladhan) can be added without touching the UI,
 * widget or alarm code.
 */
interface PrayerTimeProvider {

    /** Stable provider id, e.g. "diyanet". */
    val id: String

    suspend fun getCountries(): List<String>

    suspend fun getCities(country: String): List<String>

    suspend fun getLocations(country: String, city: String): List<City>

    suspend fun search(query: String): List<City>

    suspend fun getPrayerTimes(locationId: Long): List<DailyPrayerTimes>
}