package com.vakit.widget.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "vakit_settings")
private val Context.widgetStateDataStore: DataStore<Preferences> by preferencesDataStore(name = "vakit_widget_state")

object AppDataStore {
    val Context.settings: DataStore<Preferences> get() = settingsDataStore
    val Context.widgetState: DataStore<Preferences> get() = widgetStateDataStore
}