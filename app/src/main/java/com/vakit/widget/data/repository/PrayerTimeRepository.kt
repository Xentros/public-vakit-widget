package com.vakit.widget.data.repository

import com.vakit.widget.data.provider.PrayerTimeProvider
import com.vakit.widget.domain.model.PrayerCache
import com.vakit.widget.domain.model.SavedCity
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

sealed interface RefreshResult {
    data class Success(val cache: PrayerCache) : RefreshResult
    data class Failure(val cause: Throwable) : RefreshResult
}

class PrayerTimeRepository(
    private val provider: PrayerTimeProvider,
    private val settingsRepository: SettingsRepository,
) {

    fun cacheFlow(locationId: Long): Flow<PrayerCache?> =
        settingsRepository.prayerCacheFlow(locationId)

    suspend fun cached(locationId: Long): PrayerCache? =
        settingsRepository.readPrayerCache(locationId)

    suspend fun refresh(locationId: Long): RefreshResult {
        return try {
            val days = provider.getPrayerTimes(locationId)
            if (days.isEmpty()) {
                return RefreshResult.Failure(IllegalStateException("No prayer times returned"))
            }
            val cache = PrayerCache(
                locationId = locationId,
                days = days,
                lastFetchedAt = Instant.now(),
            )
            settingsRepository.savePrayerCache(cache)
            RefreshResult.Success(cache)
        } catch (t: Throwable) {
            RefreshResult.Failure(t)
        }
    }

    /**
     * Refreshes only when the cached data is stale or insufficient.
     * Cached data is considered fresh when it was fetched recently and still
     * covers today plus several following days.
     */
    suspend fun ensureFresh(locationId: Long): RefreshResult {
        val cache = cached(locationId)
        val today = LocalDate.now()
        val freshEnough = cache != null &&
            Duration.between(cache.lastFetchedAt, Instant.now()).abs() < REFRESH_INTERVAL &&
            cache.days.any { it.date == today } &&
            cache.days.count { !it.date.isBefore(today) } >= MIN_FUTURE_DAYS
        return if (freshEnough) {
            RefreshResult.Success(cache!!)
        } else {
            refresh(locationId)
        }
    }

    /** Used by widgets/services which only have a [SavedCity]. */
    suspend fun cachedForCity(city: SavedCity): PrayerCache? = cached(city.locationId)

    private companion object {
        val REFRESH_INTERVAL: Duration = Duration.ofHours(12)
        const val MIN_FUTURE_DAYS = 7
    }
}