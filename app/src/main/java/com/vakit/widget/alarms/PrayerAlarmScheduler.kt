package com.vakit.widget.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.vakit.widget.data.repository.PrayerTimeRepository
import com.vakit.widget.data.repository.SettingsRepository
import com.vakit.widget.domain.model.PrayerType
import com.vakit.widget.domain.model.SavedCity
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime

/**
 * Schedules exact prayer alarms using AlarmManager.
 *
 * Alarms are scheduled for today and tomorrow, cancelled before re-scheduling
 * (no duplicates), recreated on reboot via BootReceiver and recreated whenever
 * the city or cached prayer data changes.
 */
class PrayerAlarmScheduler(
    context: Context,
    private val settingsRepository: SettingsRepository,
    private val prayerTimeRepository: PrayerTimeRepository,
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    suspend fun reschedule() {
        cancelScheduled()

        val settings = settingsRepository.getSettings()
        if (!settings.alarmsEnabled) return
        val city = settings.activeCity ?: return
        val cache = prayerTimeRepository.cached(city.locationId) ?: return
        val zone = city.timezone
        val now = ZonedDateTime.now(zone)
        val soundUri = settings.alarmSoundUri

        val scheduledCodes = mutableSetOf<Int>()

        for (dayOffset in 0..1) {
            val date = now.toLocalDate().plusDays(dayOffset.toLong())
            val day = cache.days.firstOrNull { it.date == date } ?: continue
            for (prayer in PrayerType.entries) {
                if (prayer !in settings.enabledAlarmPrayers) continue
                val time = day.times[prayer] ?: continue
                val at = ZonedDateTime.of(date, time, zone)
                if (!at.isAfter(now)) continue

                val code = alarmCode(at, prayer)
                val pendingIntent = alarmPendingIntent(city, prayer, at, code, soundUri)
                val triggerAt = at.toInstant().toEpochMilli()
                if (canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                } else {
                    // Fallback: approximate time, no special permission needed.
                    alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 15 * 60_000L, pendingIntent)
                }
                scheduledCodes += code
            }
        }

        settingsRepository.setPendingAlarmCodes(scheduledCodes)
    }

    suspend fun cancelScheduled() {
        val codes = settingsRepository.getPendingAlarmCodes()
        codes.forEach { code ->
            val pi = PendingIntent.getBroadcast(
                appContext,
                code,
                Intent(appContext, PrayerAlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pi != null) {
                alarmManager.cancel(pi)
                pi.cancel()
            }
        }
        settingsRepository.setPendingAlarmCodes(emptySet())
    }

    private fun alarmPendingIntent(
        city: SavedCity,
        prayer: PrayerType,
        at: ZonedDateTime,
        code: Int,
        soundUri: String?,
    ): PendingIntent {
        val intent = Intent(appContext, PrayerAlarmReceiver::class.java).apply {
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayer.name)
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, at.toLocalTime().toString())
            putExtra(PrayerAlarmReceiver.EXTRA_CITY_NAME, city.displayName)
            putExtra(PrayerAlarmReceiver.EXTRA_SOUND_URI, soundUri)
            putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, code)
        }
        return PendingIntent.getBroadcast(
            appContext,
            code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun alarmCode(at: ZonedDateTime, prayer: PrayerType): Int =
        (at.dayOfYear * 10) + prayer.ordinal
}