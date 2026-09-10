package com.vakit.widget.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DiyanetApi {

    @GET("api/diyanet/countries")
    suspend fun getCountries(): List<String>

    @GET("api/diyanet/countries/{country}/cities")
    suspend fun getCities(@Path("country") country: String): List<String>

    @GET("api/diyanet/locations")
    suspend fun getLocations(
        @Query("country") country: String,
        @Query("city") city: String,
    ): List<LocationDto>

    @GET("api/diyanet/search")
    suspend fun search(@Query("q") query: String): List<LocationDto>

    @GET("api/diyanet/prayertimes")
    suspend fun getPrayerTimes(@Query("location_id") locationId: Long): List<PrayerTimesDto>
}