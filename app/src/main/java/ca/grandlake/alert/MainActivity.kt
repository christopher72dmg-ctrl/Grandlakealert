package ca.grandlake.alert

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

private data class Tile(val title: String, val icon: String, val subtitle: String, val url: String?)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GrandLakeAlertApp() }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrandLakeAlertApp() {
    val context = LocalContext.current
    var area by remember { mutableStateOf("Grand Lake, NB") }
    var weather by remember { mutableStateOf("Loading current weather…") }
    var weatherUpdated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        weather = try {
            withContext(Dispatchers.IO) {
                val json = JSONObject(URL("https://api.open-meteo.com/v1/forecast?latitude=46.00&longitude=-66.05&current=temperature_2m,weather_code,wind_speed_10m&timezone=America%2FHalifax").readText())
                val current = json.getJSONObject("current")
                val temp = current.getDouble("temperature_2m")
                val wind = current.getDouble("wind_speed_10m")
                "${temp.toInt()}°C • Wind ${wind.toInt()} km/h"
            }
        } catch (_: Exception) {
            "Weather temporarily unavailable"
        }
        weatherUpdated = true
    }

    val tiles = listOf(
        Tile("Weather", "🌦", weather, null),
        Tile("Police", "🚓", "Public RCMP information", "https://rcmp.ca/en/nb/news"),
        Tile("Fire", "🔥", "Public fire information", "https://www.gnb.ca/en/emergency/fire-watch.html"),
        Tile("Ambulance", "🚑", "Public emergency information", "https://www2.gnb.ca/content/gnb/en/departments/health.html"),
        Tile("Schools", "🏫", "Closures & announcements", "https://www2.gnb.ca/content/gnb/en/departments/education.html"),
        Tile("Buses", "🚌", "School transportation notices", "https://www2.gnb.ca/content/gnb/en/departments/education.html"),
        Tile("Roads", "🛣", "NB 511 conditions & incidents", "https://511.gnb.ca/"),
        Tile("Fuel", "⛽", "Daily fuel information", "https://nbeub.ca/current-petroleum-prices-2")
    )

    MaterialTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("Grand Lake Alert") }) }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
                Text("AREA", style = MaterialTheme.typography.labelLarge)
                Text(area, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("LIVE STATUS", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(if (weatherUpdated) "Weather connected" else "Connecting…", style = MaterialTheme.typography.titleMedium)
                        Text("Public information only • Source links open official pages", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tiles) { tile ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(tile.icon, style = MaterialTheme.typography.headlineMedium)
                                Text(tile.title, style = MaterialTheme.typography.titleMedium)
                                Text(tile.subtitle, style = MaterialTheme.typography.bodySmall)
                                if (tile.url != null) {
                                    Spacer(Modifier.height(8.dp))
                                    TextButton(onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tile.url)))
                                    }) { Text("Open source") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
