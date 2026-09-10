package com.vakit.widget.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vakit.widget.MainActivity
import com.xentros.vakitwidget.R
import com.vakit.widget.alarms.PrayerAlarmStopReceiver

object NotificationHelper {

    const val CHANNEL_ID = "prayer_alarms"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        )
        channel.description = context.getString(R.string.alarm_channel_name)
        manager.createNotificationChannel(channel)
    }

    fun notificationsAllowed(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasPermission(context: Context): Boolean = notificationsAllowed(context)

    fun buildNotification(
        context: Context,
        notificationId: Int,
        prayerName: String,
        time: String,
        cityName: String,
        stopPendingIntent: PendingIntent,
    ): android.app.Notification {
        createChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_clock)
            .setContentTitle(context.getString(R.string.alarm_notification_title, prayerName))
            .setContentText(context.getString(R.string.alarm_notification_text, prayerName, time, cityName))
            .setContentIntent(contentIntent)
            .setOngoing(false)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_close,
                    context.getString(R.string.alarm_notification_stop),
                    stopPendingIntent,
                )
            )
            .build()
    }

    fun showPrayerAlarm(
        context: Context,
        notificationId: Int,
        prayerName: String,
        time: String,
        cityName: String,
        stopPendingIntent: PendingIntent,
    ) {
        val notification = buildNotification(
            context = context,
            notificationId = notificationId,
            prayerName = prayerName,
            time = time,
            cityName = cityName,
            stopPendingIntent = stopPendingIntent,
        )

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS denied - nothing we can do about the banner.
        }
    }

    fun cancel(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    /**
     * Fallback used when a foreground service cannot be started (Android 12/13
     * restrict this from background alarm receivers). Plays the selected sound
     * through the notification itself and offers a Stop action.
     */
    fun showFallbackAlarm(
        context: Context,
        notificationId: Int,
        prayerName: String,
        time: String,
        cityName: String,
        soundUri: String?,
    ) {
        createChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = Intent(context, PrayerAlarmStopReceiver::class.java)
            .putExtra(PrayerAlarmStopReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val sound = if (soundUri.isNullOrBlank()) {
            Uri.parse("android.resource://${context.packageName}/${R.raw.azan_v1}")
        } else {
            Uri.parse(soundUri)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_clock)
            .setContentTitle(context.getString(R.string.alarm_notification_title, prayerName))
            .setContentText(context.getString(R.string.alarm_notification_text, prayerName, time, cityName))
            .setContentIntent(contentIntent)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(sound)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_close,
                    context.getString(R.string.alarm_notification_stop),
                    stopPendingIntent,
                )
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS denied - nothing we can do.
        }
    }
}