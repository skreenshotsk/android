package com.example.weatherapp.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WeatherViewModel : ViewModel() {
    private val _weatherData = MutableStateFlow<List<WeatherDay>>(emptyList())
    val weatherData: StateFlow<List<WeatherDay>> = _weatherData

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun fetchWeather(city: String, days: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val location = RetrofitInstance.nominatimApi.searchCity(city).firstOrNull()
                if (location != null) {
                    val weatherResponse = RetrofitInstance.weatherApi.getWeather(
                        latitude = location.lat.toDouble(),
                        longitude = location.lon.toDouble(),
                        days = days
                    )

                    _weatherData.value = weatherResponse.daily.time.indices.map { i ->
                        val date = weatherResponse.daily.time[i]
                        val hourlyTemps = getDayTemperatures(date, weatherResponse.hourly)

                        WeatherDay(
                            date = formatDate(date),
                            morningTemp = hourlyTemps.morning,
                            dayTemp = hourlyTemps.day,
                            eveningTemp = hourlyTemps.evening,
                            tempMin = weatherResponse.daily.temperature_2m_min[i],
                            tempMax = weatherResponse.daily.temperature_2m_max[i]
                        )
                    }
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "City is not found."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error while fetching data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getDayTemperatures(date: String, hourly: HourlyWeather?): DayTemperatures {
        if (hourly == null) return DayTemperatures(0.0, 0.0, 0.0)

        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val currentDate = dateFormat.parse(date)

            val morningStart = 6
            val morningEnd = 9
            val dayStart = 12
            val dayEnd = 15
            val eveningStart = 18
            val eveningEnd = 21

            var morningMax = Double.MIN_VALUE
            var dayMax = Double.MIN_VALUE
            var eveningMax = Double.MIN_VALUE

            hourly.time.forEachIndexed { index, timeStr ->
                val time = dateFormat.parse(timeStr.take(10))
                if (time == currentDate) {
                    val hour = timeStr.substring(11..12).toInt()
                    val temp = hourly.temperature_2m[index]

                    when {
                        hour in morningStart..morningEnd -> {
                            if (temp > morningMax) morningMax = temp
                        }
                        hour in dayStart..dayEnd -> {
                            if (temp > dayMax) dayMax = temp
                        }
                        hour in eveningStart..eveningEnd -> {
                            if (temp > eveningMax) eveningMax = temp
                        }
                    }
                }
            }

            //если не нашли данные для периода, используем 0.0
            DayTemperatures(
                morning = if (morningMax != Double.MIN_VALUE) morningMax else 0.0,
                day = if (dayMax != Double.MIN_VALUE) dayMax else 0.0,
                evening = if (eveningMax != Double.MIN_VALUE) eveningMax else 0.0
            )
        } catch (e: Exception) {
            DayTemperatures(0.0, 0.0, 0.0)
        }
    }

    private data class DayTemperatures(
        val morning: Double,
        val day: Double,
        val evening: Double
    )

    private fun formatDate(inputDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.ENGLISH)
            val date = inputFormat.parse(inputDate)
            outputFormat.format(date)
        } catch (e: Exception) {
            inputDate
        }
    }
}

data class WeatherDay(
    val date: String,
    val morningTemp: Double,
    val dayTemp: Double,
    val eveningTemp: Double,
    val tempMin: Double,
    val tempMax: Double
)