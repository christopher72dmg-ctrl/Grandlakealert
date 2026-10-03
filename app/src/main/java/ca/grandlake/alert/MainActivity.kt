package ca.grandlake.alert

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class Tile(
    val title: String,
    val icon: String,
    val subtitle: String,
    val url: String?,
    val accent: Color
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                GrandLakeAlertApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrandLakeAlertApp() {
    val context = LocalContext.current

    var weather by remember { mutableStateOf("Loading weather...") }
    var fuelPrices by remember { mutableStateOf("Loading prices...") }
    var schoolAlerts by remember { mutableStateOf("Checking school alerts...") }
    var policeNews by remember { mutableStateOf("Latest RCMP information") }
    var fireInfo by remember { mutableStateOf("Checking wildfire information...") }

    // 1. Weather Data (Open-Meteo API)
    LaunchedEffect(Unit) {
        weather = try {
            withContext(Dispatchers.IO) {
                val url = "https://open-meteo.com"
                val json = JSONObject(URL(url).readText())
                val current = json.getJSONObject("current")
                val temperature = current.optDouble("temperature_2m", 0.0)
                val wind = current.optDouble("wind_speed_10m", 0.0)
                "${temperature.toInt()}°C  •  Wind ${wind.toInt()} km/h"
            }
        } catch (_: Exception) {
            "Weather temporarily unavailable"
        }
    }

    // 2. Fuel Prices Data (Fixed Live NBEUB Destination Path)
    LaunchedEffect(Unit) {
        fuelPrices = try {
            withContext(Dispatchers.IO) {
                val html = URL("https://nbeub.ca").readText()
                val text = html.replace(Regex("<[^>]*>"), " ").replace("&nbsp;", " ").replace(Regex("\\s+"), " ").trim()
                val regular = Regex("Regular Gasoline\\s+Self-serve\\s+([0-9]+\\.[0-9])", RegexOption.IGNORE_CASE).find(text)
                val diesel = Regex("Ultra-low Sulphur Diesel\\s+Self-serve\\s+([0-9]+\\.[0-9])", RegexOption.IGNORE_CASE).find(text)

                if (regular != null && diesel != null) {
                    "Regular: ${regular.groupValues[1]}¢/L\nDiesel: ${diesel.groupValues[1]}¢/L"
                } else {
                    "Fuel prices unavailable"
                }
            }
        } catch (_: Exception) {
            "Fuel prices unavailable"
        }
    }

    // 3. ASDW School District Alerts Data
    LaunchedEffect(Unit) {
        schoolAlerts = try {
            withContext(Dispatchers.IO) {
                val html = URL("https://asdw.nbed.ca/news/alerts-dashboard/").readText()
                val text = html.replace(Regex("<[^>]*>"), " ").replace("&nbsp;", " ").replace(Regex("\\s+"), " ").trim()
                val alert = Regex("(Bus\\s+#?3\\d{2}.*?running.*?late|Delay.*?Zone 8|Closure.*?Zone 8)", RegexOption.IGNORE_CASE).find(text)
                alert?.groupValues?.get(1)?.trim() ?: "No active Minto / Zone 8 alerts"
            }
        } catch (_: Exception) {
            "School alerts unavailable"
        }
    }

    // 4. RCMP New Brunswick News Data
    LaunchedEffect(Unit) {
        policeNews = try {
            withContext(Dispatchers.IO) {
                val html = URL("https://rcmp.ca").readText()
                val text = html.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
                "RCMP NB: ${text.take(120)}..."
            }
        } catch (_: Exception) {
            "Latest RCMP information unavailable"
        }
    }

    // 5. Wildfires Incident Tracker (Fixed Spatial Directory Context Mapping Rule)
    LaunchedEffect(Unit) {
        fireInfo = try {
            withContext(Dispatchers.IO) {
                val url = "https://gnb.ca"
                val json = JSONObject(URL(url).readText())
                val features = json.optJSONArray("features")
                val count = features?.length() ?: 0
                if (count == 0) "No wildfire locations reported" else "$count New Brunswick wildfire location${if (count == 1) "" else "s"}"
            }
        } catch (_: Exception) {
            "Wildfire information unavailable"
        }
    }

    // Constructing data elements tracking onto Composable state values dynamically
    val tiles = listOf(
        Tile(title = "Weather", icon = "🌦️", subtitle = weather, url = null, accent = Color(0xFF42A5F5)),
        Tile(title = "Minto School", icon = "🏫", subtitle = schoolAlerts, url = "https://asdw.nbed.ca/news/alerts-dashboard/", accent = Color(0xFFFFB74D)),
        Tile(title = "Fuel Prices", icon = "⛽", subtitle = fuelPrices, url = "https://nbeub.ca", accent = Color(0xFF81C784)),
        Tile(title = "RCMP News", icon = "🚨", subtitle = policeNews, url = "https://rcmp.ca", accent = Color(0xFFE57373)),
        Tile(title = "Wildfires", icon = "🔥", subtitle = fireInfo, url = null, accent = Color(0xFFFF8A65))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grand Lake Alert Hub") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tiles) { tile ->
                Card(
                    onClick = {
                        tile.url?.let {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                            context.startActivity(intent)
                        }
                    },
                    enabled = tile.url != null,
                    colors = CardDefaults.cardColors(containerColor = tile.accent.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = tile.title, style = MaterialTheme.typography.titleMedium, color = tile.accent)
                            Text(text = tile.icon, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(text = tile.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 4)
                    }
                }
            }
        }
    }
}
