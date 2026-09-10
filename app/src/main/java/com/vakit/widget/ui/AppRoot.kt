package com.vakit.widget.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.xentros.vakitwidget.R
import com.vakit.widget.ui.cities.AddCityScreen
import com.vakit.widget.ui.home.HomeScreen
import com.vakit.widget.ui.qibla.QiblaScreen
import com.vakit.widget.ui.settings.SettingsScreen

@Composable
fun AppRoot() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showAddCity by rememberSaveable { mutableStateOf(false) }

    if (showAddCity) {
        AddCityScreen(onClose = { showAddCity = false })
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.android_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_clock),
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(R.string.tab_prayer_times)) },
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_qibla),
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(R.string.tab_qibla)) },
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_settings),
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(R.string.tab_settings)) },
                    )
                }
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    0 -> HomeScreen(onAddCity = { showAddCity = true })
                    1 -> QiblaScreen()
                    else -> SettingsScreen(onAddCity = { showAddCity = true })
                }
            }
        }
    }
}