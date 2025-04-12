package com.example.weatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.weatherapp.data.WeatherViewModel
import com.example.weatherapp.data.WeatherDay
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherAppTheme {
                WeatherScreen()
            }
        }
    }
}

@Composable
fun WeatherAppTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = colorResource(id = R.color.primary),
        secondary = colorResource(id = R.color.secondary),
        surface = colorResource(id = R.color.surface),
        onPrimary = Color.White,
        onSurface = colorResource(id = R.color.onsurface)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(viewModel: WeatherViewModel = remember { WeatherViewModel() }) {
    var cityInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var daysInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var daysError by rememberSaveable { mutableStateOf<String?>(null) }

    val weatherData by viewModel.weatherData.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val isButtonEnabled = cityInput.text.isNotBlank() && daysInput.text.toIntOrNull()?.let { it in 1..14 } == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header()
        Spacer(modifier = Modifier.height(16.dp))
        CityInputField(cityInput) { cityInput = it }
        DaysInputField(daysInput, daysError) { input, error ->
            daysInput = input
            daysError = error
        }
        Spacer(modifier = Modifier.height(8.dp))
        WeatherButton(isButtonEnabled) {
            viewModel.fetchWeather(cityInput.text, daysInput.text.toInt())
        }
        Spacer(modifier = Modifier.height(16.dp))
        WeatherContent(isLoading, errorMessage, weatherData)
    }
}

@Composable
fun Header() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Weather by City",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Check the weather for any city",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Divider(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityInputField(cityInput: TextFieldValue, onValueChange: (TextFieldValue) -> Unit) {
    OutlinedTextField(
        value = cityInput,
        onValueChange = onValueChange,
        label = { Text("Enter city name") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaysInputField(daysInput: TextFieldValue, daysError: String?, onValueChange: (TextFieldValue, String?) -> Unit) {
    OutlinedTextField(
        value = daysInput,
        onValueChange = {
            if (it.text.all { char -> char.isDigit() }) {
                val days = it.text.toIntOrNull()
                val error = if (days == null || days !in 1..14) {
                    "Please enter a number between 1 and 14"
                } else {
                    null
                }
                onValueChange(it, error)
            }
        },
        label = { Text("Forecast days (1-14)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = daysError != null,
        supportingText = {
            daysError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
fun WeatherButton(isEnabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    ) {
        Text("Search", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun WeatherContent(isLoading: Boolean, errorMessage: String?, weatherData: List<WeatherDay>) {
    when {
        isLoading -> LoadingIndicator()
        errorMessage != null -> ErrorText(errorMessage)
        weatherData.isEmpty() -> PlaceholderText()
        else -> WeatherList(weatherData)
    }
}

@Composable
fun PlaceholderText() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Enter a city to see the forecast",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ErrorText(errorMessage: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Error: $errorMessage",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherList(weatherData: List<WeatherDay>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(weatherData) { day -> WeatherCard(day) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherCard(weatherDay: WeatherDay) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = weatherDay.date,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Температура по времени суток (максимальная)
            TemperatureRow(
                icon = "🌅",
                time = "Morning",
                temp = weatherDay.morningTemp,
                isMax = weatherDay.morningTemp == weatherDay.tempMax
            )
            TemperatureRow(
                icon = "☀️",
                time = "Day",
                temp = weatherDay.dayTemp,
                isMax = weatherDay.dayTemp == weatherDay.tempMax
            )
            TemperatureRow(
                icon = "🌆",
                time = "Evening",
                temp = weatherDay.eveningTemp,
                isMax = weatherDay.eveningTemp == weatherDay.tempMax
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Daily min: ${weatherDay.tempMin.roundToInt()}°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Daily max: ${weatherDay.tempMax.roundToInt()}°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun TemperatureRow(icon: String, time: String, temp: Double, isMax: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$icon $time",
                style = MaterialTheme.typography.bodyMedium
            )
            if (isMax) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "↑",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Text(
            text = "${temp.roundToInt()}°C",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isMax) FontWeight.Bold else FontWeight.Normal,
            color = if (isMax) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}