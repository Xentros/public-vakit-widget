package com.vakit.widget.util

import androidx.annotation.DrawableRes
import com.xentros.vakitwidget.R
import com.vakit.widget.domain.model.PrayerType

object PrayerIcons {
    @DrawableRes
    fun res(prayer: PrayerType): Int = when (prayer) {
        PrayerType.IMSAK -> R.drawable.ic_imsak
        PrayerType.SUNRISE -> R.drawable.ic_sunrise
        PrayerType.DHUHR -> R.drawable.ic_dhuhr
        PrayerType.ASR -> R.drawable.ic_asr
        PrayerType.MAGHRIB -> R.drawable.ic_maghrib
        PrayerType.ISHA -> R.drawable.ic_isha
    }
}