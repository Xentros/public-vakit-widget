package com.vakit.widget.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xentros.vakitwidget.R
import com.vakit.widget.domain.model.PrayerType
import com.vakit.widget.domain.prayer.PrayerTimeCalculator
import com.vakit.widget.domain.prayer.NextPrayer
import com.vakit.widget.ui.appViewModelFactory
import com.vakit.widget.ui.theme.VakitTheme
import com.vakit.widget.util.Format
import com.vakit.widget.util.PrayerIcons
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onAddCity: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = appViewModelFactory { HomeViewModel(it.container) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state = state, onAddCity = onAddCity)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    onAddCity: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.city == null -> WelcomeContent(onAddCity)
                else -> TimesContent(state)
            }
        }
    }
}

@Composable
private fun WelcomeContent(onAddCity: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.home_add_city_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_add_city_message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAddCity) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.home_add_city_button))
        }
    }
}

@Composable
private fun TimesContent(state: HomeUiState) {
    val now = ZonedDateTime.ofInstant(state.now, state.city?.timezone ?: ZonedDateTime.now().zone)
    val next = state.city?.let {
        PrayerTimeCalculator.nextPrayer(state.days, it.timezone, now)
    }
    val today = state.days.firstOrNull { it.date == now.toLocalDate() }
    val dateLabel = now.format(DateTimeFormatter.ofPattern("EEE, d MMM"))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        item {
            CityHeader(state = state, dateLabel = dateLabel)
        }

        if (state.refreshError) {
            item {
                Text(
                    text = stringResource(R.string.home_stale_data),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (state.days.isEmpty()) {
            item {
                NoDataCard()
            }
        } else {
            item {
                NextPrayerCard(next = next, cityName = state.city?.displayName.orEmpty())
            }

            // Extra space before first prayer row
            item {
                Spacer(Modifier.height(16.dp))
            }

            today?.let { day ->
                items(PrayerType.entries) { prayer ->
                    PrayerRow(
                        prayer = prayer,
                        time = day.times[prayer],
                        isNext = prayer == next?.prayer,
                    )
                }
            }
        }
    }
}

@Composable
private fun CityHeader(state: HomeUiState, dateLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = state.city?.displayName.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.lastFetchedAt?.let { fetched ->
            Text(
                text = stringResource(R.string.data_last_updated, Format.time(fetched)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NextPrayerCard(next: NextPrayer?, cityName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                text = stringResource(R.string.home_upcoming_prayer).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            if (next != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = Format.prayerName(LocalContext.current, next.prayer),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = cityName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = Format.prayerTime(next.at.toLocalTime()),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = stringResource(
                                R.string.home_countdown_to_go_value,
                                Format.countdownCompact(LocalContext.current, next.countdown),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.home_no_data_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun PrayerRow(
    prayer: PrayerType,
    time: java.time.LocalTime?,
    isNext: Boolean,
) {
    val container = if (isNext) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val fg = if (isNext) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(container)
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .alpha(if (isNext) 1f else 0.8f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(PrayerIcons.res(prayer)),
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = Format.prayerName(LocalContext.current, prayer),
            color = fg,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Text(
            text = Format.prayerTime(time),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
            color = fg,
        )
    }
}

@Composable
private fun NoDataCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_no_data_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_no_data_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun HomePreview() {
    VakitTheme {
        HomeContent(state = HomeUiState(), onAddCity = {})
    }
}