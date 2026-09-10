package com.vakit.widget.di

import android.content.Context
import com.vakit.widget.VakitApplication
import com.vakit.widget.widget.PrayerTimesWidget

/**
 * Central "keep everything in sync" routine. Used after data refreshes, city
 * changes, app start and device boot.
 */
object MaintenanceRunner {

    /**
     * @param refresh when true, refreshes cached prayer data if it is stale.
     */
    suspend fun run(context: Context, refresh: Boolean = true) {
        val container = (context.applicationContext as VakitApplication).container
        val settings = container.settingsRepository.getSettings()
        val city = settings.activeCity
        if (refresh && city != null) {
            // Never replaces valid cached data with nothing - failures keep cache.
            container.prayerTimeRepository.ensureFresh(city.locationId)
        }
        container.alarmScheduler.reschedule()
        container.widgetUpdateScheduler.schedule()
        PrayerTimesWidget.updateAllReal(context)
    }
}