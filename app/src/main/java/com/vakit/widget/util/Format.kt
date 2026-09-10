package com.vakit.widget.util

import android.content.Context
import com.xentros.vakitwidget.R
import com.vakit.widget.domain.model.PrayerType
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Format {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val hourMinuteFormatter = DateTimeFormatter.ofPattern("HH.mm")

    fun prayerTime(time: LocalTime?): String = time?.format(timeFormatter) ?: "--:--"

    fun shortTime(time: LocalTime?): String = time?.format(hourMinuteFormatter) ?: "--:--"

    fun time(instant: Instant): String =
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(instant)

    fun is24HourFormat(context: Context): Boolean =
        android.text.format.DateFormat.is24HourFormat(context)

    /** Formats a countdown like "2 hours and 14 minutes". */
    fun countdownLong(context: Context, duration: Duration): String {
        val totalMinutes = duration.toMinutes()
        if (totalMinutes < 1) return context.getString(R.string.countdown_less_than_minute)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> {
                val h = context.resources.getQuantityString(R.plurals.hour, hours.toInt(), hours.toInt())
                val m = context.resources.getQuantityString(R.plurals.minute, minutes.toInt(), minutes.toInt())
                h + context.getString(R.string.countdown_connector) + m
            }
            hours > 0 ->
                context.resources.getQuantityString(R.plurals.hour, hours.toInt(), hours.toInt())
            else ->
                context.resources.getQuantityString(R.plurals.minute, minutes.toInt(), minutes.toInt())
        }
    }

    /** Formats a countdown compactly, e.g. "1h 24m". */
    fun countdownCompact(context: Context, duration: Duration): String {
        val totalMinutes = duration.toMinutes()
        if (totalMinutes < 1) return context.getString(R.string.countdown_less_than_minute)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 ->
                context.getString(R.string.countdown_compact_hours_minutes, hours.toInt(), minutes.toInt())
            hours > 0 ->
                context.getString(R.string.countdown_compact_hours, hours.toInt())
            else ->
                context.getString(R.string.countdown_compact_minutes, minutes.toInt())
        }
    }

    /** Returns the localized prayer name for the given type. */
    fun prayerName(context: Context, prayer: PrayerType): String = when (prayer) {
        PrayerType.IMSAK -> context.getString(R.string.prayer_imsak)
        PrayerType.SUNRISE -> context.getString(R.string.prayer_sunrise)
        PrayerType.DHUHR -> context.getString(R.string.prayer_dhuhr)
        PrayerType.ASR -> context.getString(R.string.prayer_asr)
        PrayerType.MAGHRIB -> context.getString(R.string.prayer_maghrib)
        PrayerType.ISHA -> context.getString(R.string.prayer_isha)
    }

    fun isRtl(context: Context): Boolean =
        context.resources.configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL
}