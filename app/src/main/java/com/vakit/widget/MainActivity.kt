package com.vakit.widget

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.vakit.widget.ui.AppRoot
import com.vakit.widget.ui.theme.VakitTheme
import com.vakit.widget.util.LocaleHelper
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // getApplication() is null during attachBaseContext, so read it from the base context.
        val locale = (newBase.applicationContext as? VakitApplication)?.currentLocaleTag.orEmpty()
        super.attachBaseContext(LocaleHelper.apply(newBase, locale))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        observeLocaleChanges()
        setContent {
            VakitTheme {
                AppRoot()
            }
        }
    }

    private fun observeLocaleChanges() {
        val app = application as VakitApplication
        lifecycleScope.launch {
            app.container.settingsRepository.settingsFlow
                .map { it.locale }
                .distinctUntilChanged()
                .collect { tag ->
                    val previous = app.currentLocaleTag
                    app.currentLocaleTag = tag
                    if (previous != tag) {
                        recreate()
                    }
                }
        }
    }
}