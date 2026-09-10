package com.vakit.widget.domain.prayer

import com.vakit.widget.domain.model.DailyPrayerTimes
import com.vakit.widget.domain.model.PrayerType
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

/** The next upcoming prayer together with the time it happens. */
data class NextPrayer(
    val prayer: PrayerType,
    val at: ZonedDateTime,
    val countdown: Duration,
)

object PrayerTimeCalculator {

    /**
     * Finds the next upcoming prayer across dates.
     *
     * Searches all cached days starting from [now]'s date. This correctly handles
     * the case where the next prayer (e.g. Imsak) belongs to the following day.
     *
     * Returns null when there is no usable data after [now].
     */
    fun nextPrayer(
        days: List<DailyPrayerTimes>,
        zone: ZoneId,
        now: ZonedDateTime,
    ): NextPrayer? {
        val byDate = days.associateBy { it.date }
        val today = now.toLocalDate()

        var best: Pair<PrayerType, ZonedDateTime>? = null
        var index = 0
        while (index < MAX_SEARCH_DAYS) {
            val date = today.plusDays(index.toLong())
            val day = byDate[date] ?: run { index++; continue }
            for (prayer in PrayerType.entries) {
                val time = day.times[prayer] ?: continue
                val instant = ZonedDateTime.of(date, time, zone)
                if (instant.isAfter(now) && (best == null || instant < best.second)) {
                    best = prayer to instant
                }
            }
            index++
        }
        return best?.let { (prayer, at) ->
            NextPrayer(prayer, at, Duration.between(now, at))
        }
    }

    private const val MAX_SEARCH_DAYS = 40
}
