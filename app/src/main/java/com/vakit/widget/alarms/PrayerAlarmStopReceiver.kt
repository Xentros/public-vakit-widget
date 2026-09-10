package com.vakit.widget.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vakit.widget.notif.NotificationHelper

/** Cancels a fallback alarm notification when the user taps Stop. */
class PrayerAlarmStopReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        if (notificationId != 0) {
            NotificationHelper.cancel(context, notificationId)
        }
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}