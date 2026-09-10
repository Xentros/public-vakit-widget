package com.vakit.widget.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xentros.vakitwidget.R
import com.vakit.widget.domain.model.PrayerType
import com.vakit.widget.domain.model.SavedCity
import com.vakit.widget.sound.SoundManager
import com.vakit.widget.ui.appViewModelFactory
import com.vakit.widget.ui.components.ColorPickerDialog
import com.vakit.widget.util.Format
import com.vakit.widget.util.PrayerIcons

@Composable
fun SettingsScreen(
    onAddCity: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = appViewModelFactory { SettingsViewModel(it) }),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Stop any playing sound when leaving the Settings screen
    DisposableEffect(Unit) {
        onDispose { SoundManager.stop() }
    }

    // Refresh permission-derived flags whenever the screen regains focus.
    var permissionTick by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        permissionTick = !permissionTick
        onPauseOrDispose { }
    }
    val exactAllowed = remember(permissionTick) { viewModel.exactAlarmsAllowed() }
    val notificationsAllowed = remember(permissionTick) { viewModel.notificationsAllowed() }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionTick = !permissionTick }
    val soundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.setAlarmSound(uri.toString())
        }
    }

    var pickerFor by remember { mutableStateOf<String?>(null) }
    var renameFor by remember { mutableStateOf<SavedCity?>(null) }
    var removeFor by remember { mutableStateOf<SavedCity?>(null) }

    SettingsContent(
        state = state,
        exactAllowed = exactAllowed,
        notificationsAllowed = notificationsAllowed,
        onAddCity = onAddCity,
        onSetAlarmsEnabled = viewModel::setAlarmsEnabled,
        onSetEnabledPrayers = viewModel::setEnabledAlarmPrayers,
        onRequestNotifications = {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        onOpenExactAlarmSettings = {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:${context.packageName}"),
                    )
                )
            }
        },
        onSetAlarmSound = viewModel::setAlarmSound,
        onPickCustomSound = { soundLauncher.launch(arrayOf("audio/*")) },
        onSetLocale = viewModel::setLocale,
        onSetBackgroundColor = viewModel::setWidgetBackgroundColor,
        onSetBackgroundOpacity = viewModel::setWidgetBackgroundOpacity,
        onSetForegroundColor = viewModel::setWidgetForegroundColor,
        onRefresh = viewModel::refreshData,
        onForceWidgetUpdate = viewModel::forceWidgetUpdate,
        onSetActiveCity = viewModel::setActiveCity,
        onRemoveCity = { removeFor = it },
        onRenameCity = { renameFor = it },
        onMoveCity = viewModel::moveCity,
        onPreviewSound = { key ->
            if (key == null) {
                SoundManager.play(context, null)
            } else {
                SoundManager.play(context, key)
            }
        },
        onStopSound = { SoundManager.stop() },
        onPickColor = { pickerFor = it },
    )

    pickerFor?.let { kind ->
        val current = when (kind) {
            PICKER_FOREGROUND -> state.settings.widgetForegroundColor
            else -> state.settings.widgetBackgroundColor
        }
        ColorPickerDialog(
            title = stringResource(
                if (kind == PICKER_FOREGROUND) R.string.widget_appearance_foreground_color
                else R.string.widget_appearance_background_color
            ),
            currentColor = current,
            onColorSelected = {
                if (kind == PICKER_FOREGROUND) viewModel.setWidgetForegroundColor(it)
                else viewModel.setWidgetBackgroundColor(it)
            },
            onDismiss = { pickerFor = null },
        )
    }

    renameFor?.let { city ->
        RenameDialog(city = city, onConfirm = { viewModel.renameCity(city.id, it); renameFor = null }, onDismiss = { renameFor = null })
    }

    removeFor?.let { city ->
        ConfirmRemoveDialog(city = city, onConfirm = { viewModel.removeCity(city.id); removeFor = null }, onDismiss = { removeFor = null })
    }
}

private const val PICKER_FOREGROUND = "foreground"
private const val PICKER_BACKGROUND = "background"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    state: SettingsUiState,
    exactAllowed: Boolean,
    notificationsAllowed: Boolean,
    onAddCity: () -> Unit,
    onSetAlarmsEnabled: (Boolean) -> Unit,
    onSetEnabledPrayers: (Set<PrayerType>) -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onSetAlarmSound: (String?) -> Unit,
    onPickCustomSound: () -> Unit,
    onSetLocale: (String) -> Unit,
    onSetBackgroundColor: (Long) -> Unit,
    onSetBackgroundOpacity: (Int) -> Unit,
    onSetForegroundColor: (Long) -> Unit,
    onRefresh: () -> Unit,
    onForceWidgetUpdate: () -> Unit,
    onSetActiveCity: (String) -> Unit,
    onRemoveCity: (SavedCity) -> Unit,
    onRenameCity: (SavedCity) -> Unit,
    onMoveCity: (String, Int) -> Unit,
    onPreviewSound: (String?) -> Unit,
    onStopSound: () -> Unit,
    onPickColor: (String) -> Unit,
) {
    val settings = state.settings
    var playingSoundKey by remember { mutableStateOf<String?>(null) }

    val handleSoundClick = { key: String? ->
        val effectiveKey = key ?: "builtin_azan_v1"
        if (playingSoundKey == effectiveKey) {
            // Same sound playing -> stop
            onStopSound()
            playingSoundKey = null
        } else {
            // Different sound -> stop current and play new
            onStopSound()
            onPreviewSound(key)
            playingSoundKey = effectiveKey
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_settings)) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        ) {
            item { SectionTitle(stringResource(R.string.settings_location)) }
            items(settings.savedCities.size) { index ->
                val city = settings.savedCities[index]
                CityRow(
                    city = city,
                    isActive = city.id == settings.activeCity?.id,
                    isFirst = index == 0,
                    isLast = index == settings.savedCities.size - 1,
                    maxCitiesReached = settings.savedCities.size >= MAX_CITIES,
                    onSetActive = { onSetActiveCity(city.id) },
                    onRename = { onRenameCity(city) },
                    onRemove = { onRemoveCity(city) },
                    onMoveUp = { onMoveCity(city.id, -1) },
                    onMoveDown = { onMoveCity(city.id, 1) },
                )
            }
            item {
                AddCityRow(
                    canAdd = settings.savedCities.size < MAX_CITIES,
                    onAddCity = onAddCity,
                )
            }

            item { SectionTitle(stringResource(R.string.settings_provider)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.provider_diyanet)) },
                    supportingContent = { Text(stringResource(R.string.provider_desc)) },
                    trailingContent = { Icon(painterResource(R.drawable.ic_check), contentDescription = null) },
                )
            }

            item { SectionTitle(stringResource(R.string.settings_language)) }
            item { LanguageRow(locale = settings.locale, onSelect = onSetLocale) }

            item { SectionTitle(stringResource(R.string.settings_alarms)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.alarm_master)) },
                    supportingContent = { Text(stringResource(R.string.alarm_master_desc)) },
                    trailingContent = {
                        Switch(
                            checked = settings.alarmsEnabled,
                            onCheckedChange = onSetAlarmsEnabled,
                        )
                    },
                )
            }
            if (settings.alarmsEnabled) {
                if (!notificationsAllowed) {
                    item {
                        PermissionCard(
                            title = stringResource(R.string.alarm_notification_permission_desc),
                            buttonText = stringResource(R.string.alarm_notification_permission_button),
                            onClick = onRequestNotifications,
                        )
                    }
                }
                if (!exactAllowed) {
                    item {
                        PermissionCard(
                            title = stringResource(R.string.alarm_exact_desc),
                            buttonText = stringResource(R.string.alarm_exact_button),
                            onClick = onOpenExactAlarmSettings,
                        )
                    }
                }
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        PrayerType.entries.forEach { prayer ->
                            ListItem(
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(PrayerIcons.res(prayer)),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                    )
                                },
                                headlineContent = { Text(Format.prayerName(LocalContext.current, prayer)) },
                                trailingContent = {
                                    Switch(
                                        checked = prayer in settings.enabledAlarmPrayers,
                                        onCheckedChange = { checked ->
                                            val newSet = if (checked) {
                                                settings.enabledAlarmPrayers + prayer
                                            } else {
                                                settings.enabledAlarmPrayers - prayer
                                            }
                                            onSetEnabledPrayers(newSet)
                                        },
                                    )
                                },
                            )
                        }
                        Text(
                            text = stringResource(R.string.alarm_enabled_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.settings_sounds)) }
            items(SoundManager.builtInSounds().size + 1) { index ->
                if (index == 0) {
                    SoundRow(
                        label = stringResource(R.string.alarm_sound_default),
                        selected = settings.alarmSoundUri == null,
                        isPlaying = playingSoundKey == "builtin_azan_v1",
                        onSelect = { onSetAlarmSound(null) },
                        onClick = { handleSoundClick(null) },
                    )
                } else {
                    val sound = SoundManager.builtInSounds()[index - 1]
                    SoundRow(
                        label = stringResource(sound.labelRes),
                        selected = settings.alarmSoundUri == sound.key,
                        isPlaying = playingSoundKey == sound.key,
                        onSelect = { onSetAlarmSound(sound.key) },
                        onClick = { handleSoundClick(sound.key) },
                    )
                }
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.alarm_sound_custom)) },
                    supportingContent = {
                        Text(
                            if (settings.alarmSoundUri != null &&
                                !settings.alarmSoundUri!!.startsWith(SoundManager.BUILTIN_PREFIX)
                            ) {
                                settings.alarmSoundUri!!
                            } else {
                                stringResource(R.string.alarm_sound_custom_pick)
                            }
                        )
                    },
                    trailingContent = {
                        if (settings.alarmSoundUri != null &&
                            !settings.alarmSoundUri!!.startsWith(SoundManager.BUILTIN_PREFIX)
                        ) {
                            IconButton(onClick = { onSetAlarmSound(null) }) {
                                Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.alarm_sound_custom_clear))
                            }
                        } else {
                            IconButton(onClick = onPickCustomSound) {
                                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.alarm_sound_custom_pick))
                            }
                        }
                    },
                )
            }

            item { SectionTitle(stringResource(R.string.settings_widget_appearance)) }
            item {
                ColorRow(
                    label = stringResource(R.string.widget_appearance_foreground_color),
                    color = settings.widgetForegroundColor,
                    onClick = { onPickColor(PICKER_FOREGROUND) },
                )
                ColorRow(
                    label = stringResource(R.string.widget_appearance_background_color),
                    color = settings.widgetBackgroundColor,
                    onClick = { onPickColor(PICKER_BACKGROUND) },
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = stringResource(R.string.widget_appearance_background_opacity),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val sliderValue = remember(settings.widgetBackgroundOpacity) { mutableStateOf(settings.widgetBackgroundOpacity.toFloat()) }
                        Slider(
                            value = sliderValue.value,
                            onValueChange = { sliderValue.value = it },
                            onValueChangeFinished = { onSetBackgroundOpacity(sliderValue.value.toInt()) },
                            valueRange = 0f..100f,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(R.string.widget_appearance_percent, sliderValue.value.toInt()),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(56.dp),
                        )
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.settings_data)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.data_refresh)) },
                    supportingContent = { Text(stringResource(R.string.data_refresh_desc)) },
                    trailingContent = {
                        if (state.refreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = onRefresh) {
                                Icon(painterResource(R.drawable.ic_refresh), contentDescription = stringResource(R.string.data_refresh))
                            }
                        }
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.data_force_widget_update)) },
                    supportingContent = { Text(stringResource(R.string.data_force_widget_update_desc)) },
                    trailingContent = {
                        IconButton(onClick = onForceWidgetUpdate) {
                            Icon(painterResource(R.drawable.ic_refresh), contentDescription = stringResource(R.string.data_force_widget_update))
                        }
                    },
                )
            }

            item { SectionTitle(stringResource(R.string.settings_about)) }
            item {
                ListItem(
                    headlineContent = {
                        val ctx = LocalContext.current
                        val version = remember {
                            try {
                                ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0.2"
                            } catch (_: Exception) { "1.0.2" }
                        }
                        Text(stringResource(R.string.about_version, version))
                    },
                    supportingContent = { Text(stringResource(R.string.about_desc)) },
                )
            }
            item {
                val ctx = LocalContext.current
                ListItem(
                    headlineContent = { Text(stringResource(R.string.about_privacy_policy)) },
                    supportingContent = { Text(stringResource(R.string.about_privacy_policy_desc)) },
                    trailingContent = { Icon(painterResource(R.drawable.ic_open), contentDescription = null) },
                    modifier = Modifier.clickable {
                        runCatching {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://xentros.github.io/vakit-widget/privacy.html"))
                            )
                        }
                    },
                )
            }
        }
    }
}

private const val MAX_CITIES = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageRow(locale: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "" to stringResource(R.string.language_system),
        "en" to stringResource(R.string.language_en),
        "de" to stringResource(R.string.language_de),
        "tr" to stringResource(R.string.language_tr),
    )
    val current = options.firstOrNull { it.first == locale }?.second ?: options.first().second

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = current,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.settings_language)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (tag, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSelect(tag)
                            expanded = false
                        },
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.language_system_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun CityRow(
    city: SavedCity,
    isActive: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    maxCitiesReached: Boolean,
    onSetActive: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    ListItem(
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_location),
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        headlineContent = {
            Text(
                text = city.displayName,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            )
        },
        supportingContent = {
            Text(
                text = listOfNotNull(city.country, city.region).joinToString(" · "),
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    Text(
                        text = stringResource(R.string.cities_active),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    IconButton(onClick = onSetActive, modifier = Modifier.size(32.dp)) {
                        Icon(painterResource(R.drawable.ic_check), contentDescription = stringResource(R.string.cities_make_active), modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(32.dp)) {
                    Text("▲", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                }
                IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(32.dp)) {
                    Text("▼", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                }
                IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                    Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.cities_edit_label), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.cities_remove), modifier = Modifier.size(18.dp))
                }
            }
        },
    )
}

@Composable
private fun AddCityRow(canAdd: Boolean, onAddCity: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onAddCity, enabled = canAdd) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.cities_add))
        }
        if (!canAdd) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.cities_max_reached, MAX_CITIES),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionCard(title: String, buttonText: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onClick) { Text(buttonText) }
        }
    }
}

@Composable
private fun SoundRow(
    label: String,
    selected: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        IconButton(onClick = onClick) {
            if (isPlaying) {
                Text(
                    text = "❚❚",
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { contentDescription = "Stop preview" },
                )
            } else {
                Icon(
                    painterResource(R.drawable.ic_play),
                    contentDescription = stringResource(R.string.alarm_preview_play),
                )
            }
        }
    }
}

@Composable
private fun ColorRow(label: String, color: Long, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        modifier = Modifier.clickable(onClick = onClick),
        trailingContent = {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(androidx.compose.ui.graphics.Color(color)),
            )
        },
    )
}

@Composable
private fun RenameDialog(city: SavedCity, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember(city) { mutableStateOf(city.label.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cities_label_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.cities_label_dialog_hint)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.cities_label_dialog_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cities_label_dialog_cancel)) }
        },
    )
}

@Composable
private fun ConfirmRemoveDialog(city: SavedCity, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cities_remove_confirm_title)) },
        text = { Text(stringResource(R.string.cities_remove_confirm_message, city.displayName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.cities_remove_confirm_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cities_remove_confirm_cancel)) }
        },
    )
}