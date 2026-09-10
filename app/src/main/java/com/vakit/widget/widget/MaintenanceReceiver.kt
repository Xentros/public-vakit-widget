package com.vakit.widget.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vakit.widget.VakitApplication
import kotlinx.coroutines.launch

/**
 * Receives scheduled maintenance broadcasts:
 *  - per-prayer widget updates (keeps the next-prayer highlight fresh)
 *  - the daily midnight maintenance (refresh, reschedule alarms, update widget)
 */
class MaintenanceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        val scope = (context.applicationContext as VakitApplication).applicationScope
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_MAINTENANCE -> com.vakit.widget.di.MaintenanceRunner.run(context)
                    else -> PrayerTimesWidget.updateAllReal(context)
                }
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_UPDATE = "com.vakit.widget.ACTION_WIDGET_UPDATE"
        const val ACTION_MAINTENANCE = "com.vakit.widget.ACTION_MAINTENANCE"
    }
}