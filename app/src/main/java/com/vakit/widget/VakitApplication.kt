package com.vakit.widget

import android.app.Application
import com.vakit.widget.di.AppContainer
import com.vakit.widget.di.MaintenanceRunner
import com.vakit.widget.util.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

class VakitApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val localeRef = AtomicReference<String?>(null)

    /** The currently selected app locale ("" = system). Read synchronously at startup. */
    var currentLocaleTag: String?
        get() = localeRef.get()
        set(value) = localeRef.set(value)

    override fun onCreate() {
        super.onCreate()
        // Load locale off the main thread. The first Activity may start in
        // system locale; MainActivity.observeLocaleChanges() will recreate()
        // once the DataStore value arrives. This avoids runBlocking on every
        // process start (widget/broadcast) which is an ANR/vitals risk.
        applicationScope.launch {
            try {
                val locale = container.settingsRepository.getSettings().locale
                localeRef.set(locale)
                if (locale.isNotEmpty()) LocaleHelper.applyToResources(this@VakitApplication, locale)
            } catch (_: Exception) {
                // keep system locale on error
            }
        }

        applicationScope.launch {
            MaintenanceRunner.run(this@VakitApplication, refresh = false)
        }
    }
}