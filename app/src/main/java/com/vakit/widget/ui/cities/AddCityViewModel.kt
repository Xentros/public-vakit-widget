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
        // Prefetch all cities/locations in background for fast fuzzy search (handles Pa->Paris, Fank->Frankfurt)
        viewModelScope.launch { prefetchAllForSearch() }
    }

    private var lastQuery: String = ""
    private val citiesCache = mutableMapOf<String, List<String>>()
    private var allLocationsCache: List<City> = emptyList()
    private var prefetchDone = false

    private suspend fun prefetchAllForSearch() {
        try {
            val countries = runCatching { api.getCountries() }.getOrNull() ?: return
            val all = mutableListOf<City>()
            for (country in countries) {
                val cities = runCatching { api.getCities(country) }.getOrNull() ?: continue
                citiesCache[country] = cities
                for (cityName in cities) {
                    val locs = runCatching { api.getLocations(country, cityName) }.getOrNull() ?: continue
                    for (dto in locs) {
                        all.add(dto.toCity())
                    }
                    if (all.size > 5000) break
                }
                if (all.size > 5000) break
            }
            allLocationsCache = all
            prefetchDone = true
        } catch (_: Exception) { }
    }

    fun setQuery(q: String) {
        _uiState.update { it.copy(query = q) }
        lastQuery = q
        if (q.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), searching = false) }
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            _uiState.update { it.copy(searching = true, error = null) }
            // First try exact API search
            val apiResult = runCatching { api.search(q.trim()) }.getOrNull()?.map { it.toCity() }
            if (!apiResult.isNullOrEmpty()) {
                // Rank API results by closeness to query (so Pa->Paris ranks high)
                val ranked = apiResult.sortedBy { city -> bestMatchDistance(q, city) }
                _uiState.update { it.copy(results = ranked, searching = false) }
                return@launch
            }
            // Fallback: client-side substring + fuzzy over cached browse data
            val fallback = runCatching { fuzzySearchFallback(q.trim()) }.getOrNull() ?: emptyList()
            if (fallback.isNotEmpty()) {
                _uiState.update { it.copy(results = fallback, searching = false) }
            } else {
                _uiState.update {
                    it.copy(results = emptyList(), searching = false, error = context.getString(R.string.cities_search_error))
                }
            }
        }
    }

    private suspend fun fuzzySearchFallback(query: String): List<City> {
        val q = query.lowercase().trim()
        if (q.length < 2) return emptyList()
        // If prefetch cache is ready, filter locally (instant, handles Pa->Paris, Fank->Frankfurt)
        if (prefetchDone && allLocationsCache.isNotEmpty()) {
            return allLocationsCache.filter { isCityFuzzyMatch(q, it) }
                .sortedBy { bestMatchDistance(q, it) }
                .distinctBy { it.locationId }
                .take(20)
        }
        // Otherwise on-demand fallback (limited, cached)
        if (_uiState.value.countries.isEmpty()) {
            runCatching { api.getCountries() }.onSuccess { list ->
                _uiState.update { it.copy(countries = list) }
            }
        }
        val countries = _uiState.value.countries
        val results = mutableListOf<City>()
        for (country in countries) {
            if (results.size >= 20) break
            val cities = citiesCache[country] ?: runCatching { api.getCities(country) }.getOrNull()?.also { citiesCache[country] = it } ?: continue
            for (cityName in cities) {
                if (results.size >= 20) break
                val locations = runCatching { api.getLocations(country, cityName) }.getOrNull() ?: continue
                for (dto in locations) {
                    val city = dto.toCity()
                    if (isCityFuzzyMatch(q, city)) {
                        results.add(city)
                        if (results.size >= 20) break
                    }
                }
            }
        }
        return results.sortedBy { city -> bestMatchDistance(q, city) }.distinctBy { it.locationId }.take(20)
    }

    private fun isCityFuzzyMatch(q: String, city: City): Boolean {
        val ql = q.lowercase()
        return isFuzzyMatch(ql, city.city) || isFuzzyMatch(ql, city.country) || (city.region?.let { isFuzzyMatch(ql, it) } ?: false)
    }

    private fun bestMatchDistance(q: String, city: City): Int {
        val ql = q.lowercase()
        val candidates = mutableListOf<String>()
        candidates.add(city.city)
        city.region?.split(" /", "/", " ")?.map { it.trim() }?.filter { it.isNotEmpty() }?.let { candidates.addAll(it) }
        candidates.add(city.country)
        // Also split region by " / "
        city.region?.split(" / ")?.let { candidates.addAll(it) }
        return candidates.minOfOrNull { c -> levenshtein(ql, c.lowercase()) } ?: Int.MAX_VALUE
    }

    private fun isFuzzyMatch(query: String, target: String): Boolean {
        val q = query.lowercase()
        val t = target.lowercase()
        if (t.contains(q)) return true
        if (levenshtein(q, t) <= 2) return true
        // Check any substring of t of length q.length (+1) within distance 1-2
        if (q.length >= 3 && t.length >= q.length) {
            val window = q.length
            for (i in 0..t.length - window) {
                val sub = t.substring(i, i + window)
                if (levenshtein(q, sub) <= 1) return true
            }
            if (q.length >= 4) {
                for (i in 0..t.length - window - 1) {
                    if (i + window + 1 > t.length) break
                    val sub = t.substring(i, i + window + 1)
                    if (levenshtein(q, sub) <= 2) return true
                }
            }
        }
        return false
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[a.length][b.length]
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
            val finalLabel = if (label.isNullOrBlank()) {
                pickBestLabel(city, lastQuery)
            } else label
            cityRepository.addCity(city, finalLabel)
            maintenanceRunner.run(context, refresh = true)
            onDone()
        }
    }

    private fun pickBestLabel(city: City, query: String): String? {
        if (query.isBlank()) return null
        val q = query.lowercase().trim()
        val candidates = mutableListOf<String>()
        candidates.add(city.city)
        city.region?.let { r ->
            // Region may be "FRANKFURT / ODER" -> split
            r.split("/", " / ", ",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { candidates.add(it) }
            candidates.add(r)
        }
        candidates.add(city.country)
        // Also split country if needed
        var best: String? = null
        var bestScore = Int.MAX_VALUE
        var bestContains = false
        for (c in candidates) {
            val cl = c.lowercase()
            val contains = cl.contains(q)
            val dist = levenshtein(q, cl)
            // Prefer substring matches, then smallest distance
            val score = if (contains) dist - 10 else dist
            if (score < bestScore) {
                bestScore = score
                best = c
                bestContains = contains
            }
        }
        // If no candidate is close (distance >3 and no substring), fallback to city
        return if (best != null && (bestContains || bestScore <= 3)) best else null
    }

    fun getSuggestedLabel(city: City): String = pickBestLabel(city, lastQuery) ?: ""

    private fun LocationDto.toCity(): City =
        City(locationId = id, country = country, city = city, region = region)
}