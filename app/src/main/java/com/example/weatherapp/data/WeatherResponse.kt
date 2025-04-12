package com.example.weatherapp.data

data class WeatherResponse(
    val daily: DailyWeather,
    val hourly: HourlyWeather? = null
)