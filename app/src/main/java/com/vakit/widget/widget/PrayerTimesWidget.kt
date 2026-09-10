package com.vakit.widget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.glance.text.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.xentros.vakitwidget.R
import com.vakit.widget.VakitApplication
import com.vakit.widget.domain.model.DailyPrayerTimes
import com.vakit.widget.domain.model.PrayerType
import com.vakit.widget.domain.model.SavedCity
import com.vakit.widget.domain.prayer.NextPrayer
import com.vakit.widget.domain.prayer.PrayerTimeCalculator
import com.vakit.widget.util.Format
import com.vakit.widget.util.PrayerIcons
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import java.time.ZonedDateTime

class PrayerTimesWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(200.dp, 52.dp),
            DpSize(300.dp, 68.dp),
            DpSize(420.dp, 84.dp),
            DpSize(560.dp, 104.dp),
        )
    )

    /**
     * Glance only calls [provideGlance] once per widget session. Later
     * [androidx.glance.appwidget.update]/updateAll calls merely recompose the
     * already-running composition, so the content MUST observe its data
     * sources as state (flows via collectAsState). Reading data once at the
     * top made every later recomposition render identical RemoteViews, which
     * Glance then skipped - the widget only updated once per app process.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as VakitApplication).container

        val dataFlow: Flow<WidgetData> = container.settingsRepository.settingsFlow
            .flatMapLatest { settings ->
                val city = settings.activeCity
                val base = WidgetData(
                    foreground = Color(settings.widgetForegroundColor),
                    background = Color(settings.widgetBackgroundColor)
                        .copy(alpha = backgroundAlpha(settings.widgetBackgroundOpacity)),
                    city = city,
                )
                if (city == null) {
                    flowOf(base)
                } else {
                    container.prayerTimeRepository.cacheFlow(city.locationId)
                        .map { cache -> base.copy(days = cache?.days.orEmpty()) }
                }
            }

        provideContent {
            val data by dataFlow.collectAsState(initial = WidgetData())
            val now = ZonedDateTime.now(data.city?.timezone ?: ZoneId.systemDefault())
            val next = data.city?.let {
                PrayerTimeCalculator.nextPrayer(data.days, it.timezone, now)
            }
            val today = data.days.firstOrNull { it.date == now.toLocalDate() }
            TimesContent(context, data, today, next)
        }
    }

    companion object {
        /**
         * Updates every currently placed widget. Falls back to the platform
         * widget ids when Glance has not registered them yet (fresh install
         * edge case). Each widget is updated independently so one failure
         * cannot abort the remaining instances.
         */
        suspend fun updateAllReal(context: Context) {
            val widget = PrayerTimesWidget()
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(PrayerTimesWidget::class.java)
            if (glanceIds.isEmpty()) {
                val platformIds = AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(ComponentName(context, PrayerTimesWidgetReceiver::class.java))
                platformIds.forEach { appWidgetId ->
                    runCatching { widget.update(context, manager.getGlanceIdBy(appWidgetId)) }
                }
            } else {
                glanceIds.forEach { id ->
                    runCatching { widget.update(context, id) }
                }
            }
        }
    }
}

private data class WidgetData(
    val foreground: Color = Color(0xFFFFFFFF),
    val background: Color = Color(0x00000000),
    val city: SavedCity? = null,
    val days: List<DailyPrayerTimes> = emptyList(),
)

private val ActiveHighlight = Color(0xFF78F0FE)

private const val TRANSPARENT_ALPHA_FLOOR = 0.004f

private fun backgroundAlpha(opacity: Int): Float =
    if (opacity <= 0) TRANSPARENT_ALPHA_FLOOR else (opacity.coerceAtMost(100)) / 100f

@Composable
private fun TimesContent(
    context: Context,
    data: WidgetData,
    today: DailyPrayerTimes?,
    next: NextPrayer?,
) {
    val fg = data.foreground
    val metrics = sizeMetrics(LocalSize.current.width)

    val description = data.city?.let {
        context.getString(R.string.widget_description, it.displayName)
    } ?: context.getString(R.string.app_name)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(data.background)
            .padding(horizontal = metrics.padding)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PrayerType.entries.forEach { prayer ->
            val isNext = prayer == next?.prayer
            val itemColor = if (isNext) ActiveHighlight else fg
            val name = Format.prayerName(context, prayer)
            val time = Format.prayerTime(today?.times?.get(prayer))

            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(horizontal = metrics.itemPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = name,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(itemColor),
                        fontSize = metrics.nameSize,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
                Spacer(GlanceModifier.size(metrics.iconGap))
                Image(
                    provider = ImageProvider(PrayerIcons.res(prayer)),
                    contentDescription = null,
                    modifier = GlanceModifier.size(metrics.iconSize),
                    colorFilter = ColorFilter.tint(ColorProvider(itemColor)),
                )
                Spacer(GlanceModifier.size(metrics.iconGap))
                Text(
                    text = time,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(itemColor),
                        fontSize = metrics.timeSize,
                        fontWeight = if (isNext) FontWeight.Medium else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}

private data class SizeMetrics(
    val nameSize: TextUnit,
    val timeSize: TextUnit,
    val extraSmallSize: TextUnit,
    val iconSize: Dp,
    val iconGap: Dp,
    val padding: Dp,
    val itemPadding: Dp,
)

private fun sizeMetrics(width: Dp): SizeMetrics = when {
    width < 260.dp -> SizeMetrics(15.sp, 16.sp, 12.sp, 15.dp, 4.dp, 5.dp, 1.dp)
    width < 360.dp -> SizeMetrics(18.sp, 19.sp, 14.sp, 18.dp, 5.dp, 7.dp, 2.dp)
    width < 480.dp -> SizeMetrics(21.sp, 22.sp, 16.sp, 21.dp, 6.dp, 8.dp, 3.dp)
    else -> SizeMetrics(25.sp, 26.sp, 18.sp, 25.dp, 7.dp, 10.dp, 4.dp)
}