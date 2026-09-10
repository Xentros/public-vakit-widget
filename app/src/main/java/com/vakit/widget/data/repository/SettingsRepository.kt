package com.vakit.widget.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.vakit.widget.data.local.AppDataStore.settings
import com.vakit.widget.data.local.Persist
import com.vakit.widget.domain.model.AppSettings
import com.vakit.widget.domain.model.DEFAULT_WIDGET_BG_COLOR
import com.vakit.widget.domain.model.DEFAULT_WIDGET_BG_OPACITY
import com.vakit.widget.domain.model.DEFAULT_WIDGET_FG_COLOR
import com.vakit.widget.domain.model.PrayerCache
import com.vakit.widget.domain.model.PrayerType
import com.vakit.widget.domain.model.SavedCity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

class SettingsRepository(context: Context) {

    private val dataStore: DataStore<Preferences> = context.settings

    val settingsFlow: Flow<AppSettings> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { it.toSettings() }

    suspend fun getSettings(): AppSettings = settingsFlow.first()

    suspend fun updateCities(cities: List<SavedCity>) {
        dataStore.edit { it[KEY_CITIES] = Persist.citiesToJson(cities) }
    }

    suspend fun setActiveCityId(id: String?) {
        dataStore.edit {
            if (id == null) it.remove(KEY_ACTIVE_CITY) else it[KEY_ACTIVE_CITY] = id
        }
    }

    suspend fun setProvider(provider: String) {
        dataStore.edit { it[KEY_PROVIDER] = provider }
    }

    suspend fun setLocale(locale: String) {
        dataStore.edit { it[KEY_LOCALE] = locale }
    }

    suspend fun setAlarmsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ALARMS_ENABLED] = enabled }
    }

    suspend fun setEnabledAlarmPrayers(prayers: Set<PrayerType>) {
        dataStore.edit { it[KEY_ENABLED_PRAYERS] = prayers.map { p -> p.apiField }.toSet() }
    }

    suspend fun setAlarmSoundUri(uri: String?) {
        dataStore.edit {
            if (uri == null) it.remove(KEY_ALARM_SOUND) else it[KEY_ALARM_SOUND] = uri
        }
    }

    suspend fun setPendingAlarmCodes(codes: Set<Int>) {
        dataStore.edit { it[KEY_PENDING_ALARM_CODES] = codes.map(Int::toString).toSet() }
    }

    suspend fun getPendingAlarmCodes(): Set<Int> =
        dataStore.data.first()[KEY_PENDING_ALARM_CODES].orEmpty().mapNotNull { it.toIntOrNull() }.toSet()

    suspend fun setPendingWidgetCodes(codes: Set<Int>) {
        dataStore.edit { it[KEY_PENDING_WIDGET_CODES] = codes.map(Int::toString).toSet() }
    }

    suspend fun getPendingWidgetCodes(): Set<Int> =
        dataStore.data.first()[KEY_PENDING_WIDGET_CODES].orEmpty().mapNotNull { it.toIntOrNull() }.toSet()

    suspend fun setWidgetBackgroundColor(argb: Long) {
        dataStore.edit { it[KEY_WIDGET_BG_COLOR] = argb }
    }

    suspend fun setWidgetBackgroundOpacity(percent: Int) {
        dataStore.edit { it[KEY_WIDGET_BG_OPACITY] = percent.coerceIn(0, 100) }
    }

    suspend fun setWidgetForegroundColor(argb: Long) {
        dataStore.edit { it[KEY_WIDGET_FG_COLOR] = argb }
    }

    // --- Cached prayer times ---

    suspend fun savePrayerCache(cache: PrayerCache) {
        dataStore.edit {
            it[longPreferencesKey(cacheKey(cache.locationId))] = cache.lastFetchedAt.toEpochMilli()
            it[stringPreferencesKey(cacheKey(cache.locationId))] = Persist.cacheToJson(cache)
        }
    }

    suspend fun readPrayerCache(locationId: Long): PrayerCache? {
        val prefs = dataStore.data.first()
        return Persist.cacheFromJson(prefs[stringPreferencesKey(cacheKey(locationId))])
    }

    fun prayerCacheFlow(locationId: Long): Flow<PrayerCache?> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { Persist.cacheFromJson(it[stringPreferencesKey(cacheKey(locationId))]) }

    private fun cacheKey(locationId: Long) = "prayer_cache_$locationId"

    private fun Preferences.toSettings(): AppSettings {
        val cities = Persist.citiesFromJson(this[KEY_CITIES])
        val enabledFields = this[KEY_ENABLED_PRAYERS].orEmpty()
        return AppSettings(
            savedCities = cities,
            activeCityId = this[KEY_ACTIVE_CITY],
            provider = this[KEY_PROVIDER] ?: "diyanet",
            locale = this[KEY_LOCALE] ?: "",
            alarmsEnabled = this[KEY_ALARMS_ENABLED] ?: false,
            enabledAlarmPrayers = PrayerType.entries.filterTo(mutableSetOf()) { it.apiField in enabledFields }
                .ifEmpty { PrayerType.entries.toSet() },
            alarmSoundUri = this[KEY_ALARM_SOUND],
            widgetBackgroundColor = this[KEY_WIDGET_BG_COLOR] ?: DEFAULT_WIDGET_BG_COLOR,
            widgetBackgroundOpacity = this[KEY_WIDGET_BG_OPACITY] ?: DEFAULT_WIDGET_BG_OPACITY,
            widgetForegroundColor = this[KEY_WIDGET_FG_COLOR] ?: DEFAULT_WIDGET_FG_COLOR,
        )
    }

    private companion object {
        val KEY_CITIES = stringPreferencesKey("cities_json")
        val KEY_ACTIVE_CITY = stringPreferencesKey("active_city_id")
        val KEY_PROVIDER = stringPreferencesKey("provider")
        val KEY_LOCALE = stringPreferencesKey("locale")
        val KEY_ALARMS_ENABLED = booleanPreferencesKey("alarms_enabled")
        val KEY_ENABLED_PRAYERS = stringSetPreferencesKey("enabled_prayers")
        val KEY_ALARM_SOUND = stringPreferencesKey("alarm_sound")
        val KEY_PENDING_ALARM_CODES = stringSetPreferencesKey("pending_alarm_codes")
        val KEY_PENDING_WIDGET_CODES = stringSetPreferencesKey("pending_widget_codes")
        val KEY_WIDGET_BG_COLOR = longPreferencesKey("widget_bg_color")
        val KEY_WIDGET_BG_OPACITY = intPreferencesKey("widget_bg_opacity")
        val KEY_WIDGET_FG_COLOR = longPreferencesKey("widget_fg_color")
    }
}