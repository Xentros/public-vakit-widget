package com.vakit.widget.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vakit.widget.VakitApplication
import com.vakit.widget.di.MaintenanceRunner
import kotlinx.coroutines.launch

/**
 * Recreates alarms, widget updates and maintenance schedules after device boot,
 * app updates and time/timezone changes.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            -> {
                val result = goAsync()
                (context.applicationContext as VakitApplication).applicationScope.launch {
                    try {
                        MaintenanceRunner.run(context)
                    } finally {
                        result.finish()
                    }
                }
            }
        }
    }
}