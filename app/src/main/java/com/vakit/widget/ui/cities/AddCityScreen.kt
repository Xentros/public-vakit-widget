package com.vakit.widget.ui.cities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xentros.vakitwidget.R
import com.vakit.widget.domain.model.City
import com.vakit.widget.domain.model.MAX_SAVED_CITIES
import com.vakit.widget.ui.appViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCityScreen(
    onClose: () -> Unit,
    viewModel: AddCityViewModel = viewModel(factory = appViewModelFactory { AddCityViewModel(it) }),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var saveFor by remember { mutableStateOf<City?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cities_add_title)) },
                navigationIcon = {
                    IconButton(onClick = { if (!viewModel.back()) onClose() }) {
                        Icon(
                            painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.action_cancel),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text(stringResource(R.string.cities_search_hint)) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = true,
                trailingIcon = {
                    if (state.query.isNotBlank()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(painterResource(R.drawable.ic_close), contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                },
            )

            when {
                state.error != null -> ErrorView(message = state.error.orEmpty())
                state.loading || state.searching -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.inSearch -> SearchResults(
                    results = state.results,
                    canAdd = state.canAdd,
                    onPick = { saveFor = it },
                )
                else -> BrowseView(
                    state = state,
                    onSelectCountry = viewModel::selectCountry,
                    onSelectCity = viewModel::selectCity,
                    onPick = { saveFor = it },
                )
            }
        }
    }

    saveFor?.let { city ->
        SaveCityDialog(
            city = city,
            canAdd = state.canAdd,
            onConfirm = { label -> viewModel.save(city, label) { saveFor = null; onClose() } },
            onDismiss = { saveFor = null },
        )
    }
}

@Composable
private fun BrowseView(
    state: AddCityUiState,
    onSelectCountry: (String) -> Unit,
    onSelectCity: (String, String) -> Unit,
    onPick: (City) -> Unit,
) {
    when (val step = state.step) {
        is BrowseStep.Locations -> {
            Column(modifier = Modifier.fillMaxSize()) {
                Header(title = "${step.country} · ${step.city}")
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (state.locations.isEmpty()) {
                        item { EmptyText(stringResource(R.string.cities_empty_region)) }
                    } else {
                        items(state.locations) { location ->
                            CityItem(
                                title = location.region ?: location.city,
                                subtitle = step.city,
                                onClick = { onPick(location) },
                            )
                        }
                    }
                }
            }
        }
        is BrowseStep.Cities -> {
            Column(modifier = Modifier.fillMaxSize()) {
                Header(title = step.country)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (state.cities.isEmpty()) {
                        item { EmptyText(stringResource(R.string.cities_empty_city)) }
                    } else {
                        items(state.cities) { city ->
                            CityItem(title = city, subtitle = null) { onSelectCity(step.country, city) }
                        }
                    }
                }
            }
        }
        BrowseStep.Countries -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (state.countries.isEmpty()) {
                    item { EmptyText(stringResource(R.string.cities_empty_country)) }
                } else {
                    items(state.countries) { country ->
                        CityItem(title = country, subtitle = null) { onSelectCountry(country) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun EmptyText(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SearchResults(results: List<City>, canAdd: Boolean, onPick: (City) -> Unit) {
    if (results.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.cities_search_no_results))
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(results) { city ->
            CityItem(
                title = city.city,
                subtitle = listOfNotNull(city.country, city.region).joinToString(" · "),
                onClick = { if (canAdd) onPick(city) },
            )
        }
    }
}

@Composable
private fun CityItem(title: String, subtitle: String?, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it) } },
    )
}

@Composable
private fun ErrorView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
private fun SaveCityDialog(
    city: City,
    canAdd: Boolean,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember(city) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(city.city) },
        text = {
            Column {
                Text(
                    text = listOfNotNull(city.country, city.region).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.cities_label_dialog_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!canAdd) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.cities_max_reached, MAX_SAVED_CITIES),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(label) }, enabled = canAdd) {
                Text(stringResource(R.string.cities_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}