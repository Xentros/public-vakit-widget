package com.vakit.widget.data.repository

import com.vakit.widget.domain.location.CountryTimeZones
import com.vakit.widget.domain.model.City
import com.vakit.widget.domain.model.MAX_SAVED_CITIES
import com.vakit.widget.domain.model.SavedCity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CityRepository(
    private val settingsRepository: SettingsRepository,
) {

    val citiesFlow: Flow<List<SavedCity>> = settingsRepository.settingsFlow.map { it.savedCities }

    val activeCityFlow: Flow<SavedCity?> = settingsRepository.settingsFlow.map { it.activeCity }

    suspend fun getCities(): List<SavedCity> = settingsRepository.getSettings().savedCities

    suspend fun getActiveCity(): SavedCity? = settingsRepository.getSettings().activeCity

    suspend fun addCity(city: City, label: String? = null): SavedCity {
        val cities = getCities()
        val existing = cities.firstOrNull { it.locationId == city.locationId }
        if (existing != null) return existing

        val saved = SavedCity(
            id = "city_${city.locationId}",
            country = city.country,
            city = city.city,
            region = city.region,
            locationId = city.locationId,
            label = label?.takeIf { it.isNotBlank() },
            timezoneId = CountryTimeZones.resolveTimezone(city.country),
        )
        val newList = cities + saved
        settingsRepository.updateCities(newList.take(MAX_SAVED_CITIES))
        if (settingsRepository.getSettings().activeCityId == null) {
            settingsRepository.setActiveCityId(saved.id)
        }
        return saved
    }

    suspend fun removeCity(id: String) {
        val cities = getCities().filterNot { it.id == id }
        settingsRepository.updateCities(cities)
        val settings = settingsRepository.getSettings()
        if (settings.activeCityId == id || settings.activeCityId == null) {
            settingsRepository.setActiveCityId(cities.firstOrNull()?.id)
        }
    }

    suspend fun renameCity(id: String, label: String) {
        val cities = getCities().map {
            if (it.id == id) it.copy(label = label.takeIf { l -> l.isNotBlank() }) else it
        }
        settingsRepository.updateCities(cities)
    }

    /** Reorders the saved cities. [ids] must contain every city id exactly once. */
    suspend fun reorderCities(ids: List<String>) {
        val byId = getCities().associateBy { it.id }
        val ordered = ids.mapNotNull { byId[it] }
        settingsRepository.updateCities(ordered)
    }

    suspend fun setActiveCity(id: String) {
        if (getCities().any { it.id == id }) {
            settingsRepository.setActiveCityId(id)
        }
    }
}