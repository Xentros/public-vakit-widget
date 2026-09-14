package com.vakit.widget.alarms

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.session.MediaSession
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.xentros.vakitwidget.R
import com.vakit.widget.notif.NotificationHelper
import com.vakit.widget.sound.SoundManager

/**
 * Foreground service that plays the prayer alarm sound and shows a notification.
 * Stopping via the notification (or the automatic timeout) stops the sound.
 */
class PrayerAlarmService : Service() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var notificationId = 0
    private var mediaSession: MediaSession? = null

    private val autoStop = Runnable { stopSelf() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val prayerName = intent?.getStringExtra(EXTRA_PRAYER_NAME)
            ?: getString(R.string.prayer_isha)
        val time = intent?.getStringExtra(EXTRA_TIME) ?: ""
        val cityName = intent?.getStringExtra(EXTRA_CITY) ?: ""
        val soundUri = intent?.getStringExtra(EXTRA_SOUND)
        notificationId = intent?.getIntExtra(EXTRA_NOTIFICATION_ID, 0) ?: 0
        if (notificationId == 0) notificationId = 1

        NotificationHelper.createChannel(this)

        val stopIntent = Intent(this, PrayerAlarmService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            notificationId,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Android 14+ expects an active media session for mediaPlayback services.
        ensureMediaSession()

        val notification = NotificationHelper.buildNotification(
            context = this,
            notificationId = notificationId,
            prayerName = prayerName,
            time = time,
            cityName = cityName,
            stopPendingIntent = stopPendingIntent,
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(notificationId, notification)
            }
        } catch (e: SecurityException) {
            // Some devices reject the mediaPlayback type; start without it.
            runCatching { startForeground(notificationId, notification) }
        }

        SoundManager.play(this, soundUri) { stopSelf() }

        // Fallback stop in case completion doesn't fire
        mainHandler.removeCallbacks(autoStop)
        mainHandler.postDelayed(autoStop, AUTO_STOP_MILLIS)
        return START_NOT_STICKY
    }

    private fun ensureMediaSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            runCatching {
                mediaSession = MediaSession(this, "VakitAlarm").apply {
                    setActive(true)
                    setCallback(object : MediaSession.Callback() {})
                }
            }
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(autoStop)
        SoundManager.stop()
        mediaSession?.release()
        mediaSession = null
        if (notificationId != 0) {
            NotificationHelper.cancel(this, notificationId)
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.vakit.widget.ACTION_STOP_ALARM"
        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_TIME = "time"
        const val EXTRA_CITY = "city"
        const val EXTRA_SOUND = "sound"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val AUTO_STOP_MILLIS = 3 * 60 * 1000L
    }
}