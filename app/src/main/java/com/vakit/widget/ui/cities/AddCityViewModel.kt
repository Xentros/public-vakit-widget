package com.vakit.widget.ui.cities

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentros.vakitwidget.R
import com.vakit.widget.VakitApplication
import com.vakit.widget.data.api.LocationDto
import com.vakit.widget.domain.model.City
import com.vakit.widget.domain.model.MAX_SAVED_CITIES
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface BrowseStep {
    data object Countries : BrowseStep
    data class Cities(val country: String) : BrowseStep
    data class Locations(val country: String, val city: String) : BrowseStep
}

data class AddCityUiState(
    val query: String = "",
    val results: List<City> = emptyList(),
    val searching: Boolean = false,
    val step: BrowseStep = BrowseStep.Countries,
    val countries: List<String> = emptyList(),
    val cities: List<String> = emptyList(),
    val locations: List<City> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val savedCount: Int = 0,
) {
    val canAdd: Boolean get() = savedCount < MAX_SAVED_CITIES
    val inSearch: Boolean get() = query.isNotBlank()
}

class AddCityViewModel(app: VakitApplication) : ViewModel() {

    private val context: Context = app.applicationContext
    private val container = app.container
    private val api = container.diyanetApi
    private val cityRepository = container.cityRepository
    private val maintenanceRunner = com.vakit.widget.di.MaintenanceRunner

    private val _uiState = MutableStateFlow(AddCityUiState())
    val uiState: StateFlow<AddCityUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadCountries()
        viewModelScope.launch {
            _uiState.update { it.copy(savedCount = cityRepository.getCities().size) }
        }
    }

    fun setQuery(q: String) {
        _uiState.update { it.copy(query = q) }
        if (q.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            _uiState.update { it.copy(searching = true, error = null) }
            runCatching { api.search(q.trim()) }
                .onSuccess { dtos ->
                    _uiState.update {
                        it.copy(results = dtos.map { it.toCity() }, searching = false)
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(results = emptyList(), searching = false, error = context.getString(R.string.cities_search_error))
                    }
                }
        }
    }

    fun loadCountries() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { api.getCountries() }
                .onSuccess { list ->
                    _uiState.update { it.copy(countries = list, loading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(loading = false, error = context.getString(R.string.cities_loading_error)) }
                }
        }
    }

    fun selectCountry(country: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(step = BrowseStep.Cities(country), cities = emptyList(), loading = true, error = null) }
            runCatching { api.getCities(country) }
                .onSuccess { list -> _uiState.update { it.copy(cities = list, loading = false) } }
                .onFailure { _uiState.update { it.copy(loading = false, error = context.getString(R.string.cities_loading_error)) } }
        }
    }

    fun selectCity(country: String, city: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(step = BrowseStep.Locations(country, city), locations = emptyList(), loading = true, error = null) }
            runCatching { api.getLocations(country, city) }
                .onSuccess { list ->
                    _uiState.update { it.copy(locations = list.map { it.toCity() }, loading = false) }
                }
                .onFailure { _uiState.update { it.copy(loading = false, error = context.getString(R.string.cities_loading_error)) } }
        }
    }

    /** Steps one level back in the browse hierarchy. */
    fun back(): Boolean {
        when (val step = _uiState.value.step) {
            is BrowseStep.Locations -> _uiState.update { it.copy(step = BrowseStep.Cities(step.country), error = null) }
            is BrowseStep.Cities -> _uiState.update { it.copy(step = BrowseStep.Countries, error = null) }
            BrowseStep.Countries -> return false
        }
        return true
    }

    fun save(city: City, label: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            cityRepository.addCity(city, label)
            maintenanceRunner.run(context, refresh = true)
            onDone()
        }
    }

    private fun LocationDto.toCity(): City =
        City(locationId = id, country = country, city = city, region = region)
}