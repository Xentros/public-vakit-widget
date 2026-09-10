package com.vakit.widget.di

import android.content.Context
import com.vakit.widget.alarms.PrayerAlarmScheduler
import com.vakit.widget.data.api.ApiClient
import com.vakit.widget.data.api.DiyanetApi
import com.vakit.widget.data.provider.DiyanetPrayerTimeProvider
import com.vakit.widget.data.provider.PrayerTimeProvider
import com.vakit.widget.data.repository.CityRepository
import com.vakit.widget.data.repository.PrayerTimeRepository
import com.vakit.widget.data.repository.SettingsRepository
import com.vakit.widget.widget.WidgetUpdateScheduler

/**
 * Minimal manual dependency container. Kept deliberately simple - the project
 * is small enough that a DI framework would be over-engineering.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val diyanetApi: DiyanetApi by lazy { ApiClient.diyanetApi() }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val cityRepository: CityRepository by lazy { CityRepository(settingsRepository) }

    val prayerTimeProvider: PrayerTimeProvider by lazy { DiyanetPrayerTimeProvider(diyanetApi) }

    val prayerTimeRepository: PrayerTimeRepository by lazy {
        PrayerTimeRepository(prayerTimeProvider, settingsRepository)
    }

    val alarmScheduler: PrayerAlarmScheduler by lazy {
        PrayerAlarmScheduler(appContext, settingsRepository, prayerTimeRepository)
    }

    val widgetUpdateScheduler: WidgetUpdateScheduler by lazy {
        WidgetUpdateScheduler(appContext, settingsRepository, prayerTimeRepository)
    }
}