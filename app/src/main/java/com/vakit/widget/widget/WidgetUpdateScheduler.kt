package com.vakit.widget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.vakit.widget.data.repository.PrayerTimeRepository
import com.vakit.widget.data.repository.SettingsRepository
import com.vakit.widget.domain.model.PrayerType
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Schedules light maintenance broadcasts with AlarmManager:
 *  - a widget update at each prayer time, so the subtle next-prayer highlight
 *    stays correct during the day,
 *  - one daily maintenance shortly after midnight that refreshes data,
 *    reschedules alarms/widget updates and updates the widget.
 *
 * These use inexact [AlarmManager.setWindow], so no special exact-alarm
 * permission is required. The widget itself never performs network requests.
 */
class WidgetUpdateScheduler(
    context: Context,
    private val settingsRepository: SettingsRepository,
    private val prayerTimeRepository: PrayerTimeRepository,
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    suspend fun schedule() {
        cancelScheduled()

        val settings = settingsRepository.getSettings()
        val city = settings.activeCity ?: return
        val cache = prayerTimeRepository.cached(city.locationId) ?: return
        val zone = city.timezone
        val now = ZonedDateTime.now(zone)

        val codes = mutableSetOf<Int>()
        val today = now.toLocalDate()

        cache.days.firstOrNull { it.date == today }?.let { day ->
            PrayerType.entries.forEach { prayer ->
                val time = day.times[prayer] ?: return@forEach
                val at = ZonedDateTime.of(today, time, zone)
                if (at.isAfter(now)) {
                    val code = widgetCode(prayer, today)
                    scheduleAt(at, code, MaintenanceReceiver.ACTION_WIDGET_UPDATE)
                    codes += code
                }
            }
        }

        val nextMaintenance = today.plusDays(1).atTime(0, 5).atZone(zone)
        scheduleAt(nextMaintenance, MAINTENANCE_CODE, MaintenanceReceiver.ACTION_MAINTENANCE)
        codes += MAINTENANCE_CODE

        settingsRepository.setPendingWidgetCodes(codes)
    }

    suspend fun cancelScheduled() {
        val codes = settingsRepository.getPendingWidgetCodes()
        codes.forEach { code ->
            val pi = PendingIntent.getBroadcast(
                appContext,
                code,
                Intent(appContext, MaintenanceReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pi != null) {
                alarmManager.cancel(pi)
                pi.cancel()
            }
        }
        settingsRepository.setPendingWidgetCodes(emptySet())
    }

    private fun scheduleAt(at: ZonedDateTime, code: Int, action: String) {
        val intent = Intent(appContext, MaintenanceReceiver::class.java).setAction(action)
        val pi = PendingIntent.getBroadcast(
            appContext,
            code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            at.toInstant().toEpochMilli(),
            WINDOW_MILLIS,
            pi,
        )
    }

    private fun widgetCode(prayer: PrayerType, date: LocalDate): Int =
        10000 + (date.dayOfYear * 10) + prayer.ordinal

    private companion object {
        const val MAINTENANCE_CODE = 900000
        const val WINDOW_MILLIS = 15 * 60_000L
    }
}