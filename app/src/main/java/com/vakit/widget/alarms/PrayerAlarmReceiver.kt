package com.vakit.widget.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.vakit.widget.notif.NotificationHelper

/** Fires when a scheduled prayer alarm goes off and starts the alarm service. */
class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: return
        val time = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: return
        val city = intent.getStringExtra(EXTRA_CITY_NAME) ?: return
        val soundUri = intent.getStringExtra(EXTRA_SOUND_URI)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        val serviceIntent = Intent(context, PrayerAlarmService::class.java).apply {
            putExtra(PrayerAlarmService.EXTRA_PRAYER_NAME, prayerName)
            putExtra(PrayerAlarmService.EXTRA_TIME, time)
            putExtra(PrayerAlarmService.EXTRA_CITY, city)
            putExtra(PrayerAlarmService.EXTRA_SOUND, soundUri)
            putExtra(PrayerAlarmService.EXTRA_NOTIFICATION_ID, notificationId)
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (t: Throwable) {
            // Android 12/13 forbid starting a foreground service from a
            // background alarm receiver. Fall back to a high-priority
            // notification that plays the sound itself.
            NotificationHelper.showFallbackAlarm(
                context = context,
                notificationId = notificationId,
                prayerName = prayerName,
                time = time,
                cityName = city,
                soundUri = soundUri,
            )
        }
    }

    companion object {
        const val EXTRA_PRAYER_NAME = "prayer"
        const val EXTRA_PRAYER_TIME = "time"
        const val EXTRA_CITY_NAME = "city"
        const val EXTRA_SOUND_URI = "sound_uri"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}