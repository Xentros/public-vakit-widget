package com.vakit.widget.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakit.widget.data.repository.PrayerTimeRepository
import com.vakit.widget.data.repository.RefreshResult
import com.vakit.widget.data.repository.SettingsRepository
import com.vakit.widget.di.AppContainer
import com.vakit.widget.domain.model.DailyPrayerTimes
import com.vakit.widget.domain.model.SavedCity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

data class HomeUiState(
    val now: Instant = Instant.now(),
    val city: SavedCity? = null,
    val days: List<DailyPrayerTimes> = emptyList(),
    val lastFetchedAt: Instant? = null,
    val refreshing: Boolean = false,
    val refreshError: Boolean = false,
)

class HomeViewModel(container: AppContainer) : ViewModel() {

    private val settingsRepository: SettingsRepository = container.settingsRepository
    private val prayerTimeRepository: PrayerTimeRepository = container.prayerTimeRepository

    private val refreshing = MutableStateFlow(false)
    private val refreshError = MutableStateFlow(false)

    private val ticker: Flow<Instant> = flow {
        while (true) {
            emit(Instant.now())
            delay(1_000)
        }
    }

    /** Data flow: follows the active city and its cached prayer times. */
    private val dataFlow: Flow<Triple<SavedCity?, List<DailyPrayerTimes>, Instant?>> =
        settingsRepository.settingsFlow
            .map { it.activeCity }
            .distinctUntilChanged()
            .flatMapLatest { city ->
                if (city == null) flow {
                    emit(Triple<SavedCity?, List<DailyPrayerTimes>, Instant?>(null, emptyList(), null))
                } else {
                    prayerTimeRepository.cacheFlow(city.locationId).map { cache ->
                        Triple<SavedCity?, List<DailyPrayerTimes>, Instant?>(
                            city, cache?.days.orEmpty(), cache?.lastFetchedAt,
                        )
                    }
                }
            }

    val uiState: StateFlow<HomeUiState> = combine(dataFlow, ticker, refreshing, refreshError) { data, now, r, err ->
        HomeUiState(
            now = now,
            city = data.first,
            days = data.second,
            lastFetchedAt = data.third,
            refreshing = r,
            refreshError = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refreshIfMissing()
    }

    fun refresh() {
        viewModelScope.launch {
            val city = settingsRepository.getSettings().activeCity ?: return@launch
            refreshing.value = true
            refreshError.value = false
            val result = prayerTimeRepository.ensureFresh(city.locationId)
            refreshError.value = result is RefreshResult.Failure
            refreshing.value = false
        }
    }

    /** When a city exists but there is no cache yet, fetch silently on open. */
    private fun refreshIfMissing() {
        viewModelScope.launch {
            val city = settingsRepository.getSettings().activeCity ?: return@launch
            val cache = prayerTimeRepository.cached(city.locationId)
            if (cache == null) {
                refresh()
            }
        }
    }
}