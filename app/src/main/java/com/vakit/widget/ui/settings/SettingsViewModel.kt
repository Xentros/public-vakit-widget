package com.vakit.widget.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakit.widget.VakitApplication
import com.vakit.widget.data.repository.RefreshResult
import com.vakit.widget.domain.model.AppSettings
import com.vakit.widget.domain.model.PrayerType
import com.vakit.widget.widget.PrayerTimesWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val refreshing: Boolean = false,
)

class SettingsViewModel(app: VakitApplication) : ViewModel() {

    private val context: Context = app.applicationContext
    private val container = app.container
    private val settingsRepository = container.settingsRepository
    private val prayerTimeRepository = container.prayerTimeRepository
    private val alarmScheduler = container.alarmScheduler
    private val maintenanceRunner = com.vakit.widget.di.MaintenanceRunner

    private val refreshing = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> =
        combine(settingsRepository.settingsFlow, refreshing) { settings, r ->
            SettingsUiState(settings = settings, refreshing = r)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun exactAlarmsAllowed(): Boolean = alarmScheduler.canScheduleExactAlarms()

    fun notificationsAllowed(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun setAlarmsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAlarmsEnabled(enabled)
            maintenanceRunner.run(context, refresh = false)
        }
    }

    fun setEnabledAlarmPrayers(prayers: Set<PrayerType>) {
        viewModelScope.launch {
            settingsRepository.setEnabledAlarmPrayers(prayers)
            maintenanceRunner.run(context, refresh = false)
        }
    }

    fun setAlarmSound(uri: String?) {
        viewModelScope.launch {
            settingsRepository.setAlarmSoundUri(uri)
            maintenanceRunner.run(context, refresh = false)
        }
    }

    fun setLocale(locale: String) {
        viewModelScope.launch {
            settingsRepository.setLocale(locale)
        }
    }

    fun setWidgetBackgroundColor(color: Long) {
        viewModelScope.launch {
            settingsRepository.setWidgetBackgroundColor(color)
            maintenanceRunner.run(context, refresh = false)
        }
    }

    fun setWidgetBackgroundOpacity(opacity: Int) {
        viewModelScope.launch {
            settingsRepository.setWidgetBackgroundOpacity(opacity)
            maintenanceRunner.run(context, refresh = false)
        }
    }

    fun setWidgetForegroundColor(color: Long) {
        viewModelScope.launch {
            settingsRepository.setWidgetForegroundColor(color)
            maintenanceRunner.run(context, refresh = false)
        }
    }

    fun setActiveCity(id: String) {
        viewModelScope.launch {
            container.cityRepository.setActiveCity(id)
            maintenanceRunner.run(context, refresh = false)
        }
    }

    fun removeCity(id: String) {
        viewModelScope.launch {
            container.cityRepository.removeCity(id)
            maintenanceRunner.run(context, refresh = false)
        }
    }

    fun renameCity(id: String, label: String) {
        viewModelScope.launch {
            container.cityRepository.renameCity(id, label)
            maintenanceRunner.run(context, refresh = false)
        }
    }

    fun moveCity(id: String, direction: Int) {
        viewModelScope.launch {
            val cities = container.cityRepository.getCities().toMutableList()
            val index = cities.indexOfFirst { it.id == id }
            val target = index + direction
            if (index < 0 || target < 0 || target >= cities.size) return@launch
            val item = cities.removeAt(index)
            cities.add(target, item)
            container.cityRepository.reorderCities(cities.map { it.id })
            maintenanceRunner.run(context, refresh = false)
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            val city = settingsRepository.getSettings().activeCity ?: return@launch
            refreshing.value = true
            val result = prayerTimeRepository.ensureFresh(city.locationId)
            if (result is RefreshResult.Success) {
                maintenanceRunner.run(context, refresh = false)
            }
            refreshing.value = false
        }
    }

    fun forceWidgetUpdate() {
        viewModelScope.launch {
            PrayerTimesWidget.updateAllReal(context)
        }
    }
}