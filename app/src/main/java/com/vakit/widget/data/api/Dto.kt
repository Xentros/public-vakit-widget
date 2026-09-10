package com.vakit.widget.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationDto(
    val id: Long,
    val country: String,
    val city: String,
    val region: String? = null,
)

@Serializable
data class PrayerTimesDto(
    val date: String,
    val fajr: String = "",
    val sun: String = "",
    val dhuhr: String = "",
    val asr: String = "",
    val maghrib: String = "",
    val isha: String = "",
)