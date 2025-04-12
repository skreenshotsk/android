package com.example.weatherapp.data

import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {
    @GET("search")
    suspend fun searchCity(
        @Query("city") city: String,
        @Query("format") format: String = "json"
    ): List<NominatimResponse>
}