package com.example.weatherapp.data

data class HourlyWeather(
    val time: List<String>,
    val temperature_2m: List<Double>
)