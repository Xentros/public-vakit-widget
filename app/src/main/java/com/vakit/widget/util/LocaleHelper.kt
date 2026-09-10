package com.vakit.widget.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    /**
     * Returns a context whose resources use [localeTag] ("" = follow the system).
     * Never mutates the caller; safe to use in Activity#attachBaseContext.
     */
    fun apply(context: Context, localeTag: String): Context {
        if (localeTag.isBlank()) return context
        val locale = Locale.forLanguageTag(localeTag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    /** Updates the global resources configuration (used for widget/background contexts). */
    @Suppress("DEPRECATION")
    fun applyToResources(context: Context, localeTag: String) {
        if (localeTag.isBlank()) return
        val locale = Locale.forLanguageTag(localeTag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}