```kotlin
package ca.grandlake.alert

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

private data class Tile(
    val title: String,
    val icon: String,
    val subtitle: String,
    val url: String?
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GrandLakeAlertApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrandLakeAlertApp() {

    val context = LocalContext.current

    var area by remember {
        mutableStateOf("Grand Lake, NB")
    }

    var weather by remember {
        mutableStateOf("Loading current weather…")
    }

    var fuelPrices by remember {
        mutableStateOf("Loading official prices…")
    }

    var schoolAlerts by remember {
        mutableStateOf("Checking ASD-W alerts…")
    }

    // ---------------------------------------------------------
    // LIVE WEATHER
    // ---------------------------------------------------------

    LaunchedEffect(Unit) {

        weather = try {

            withContext(Dispatchers.IO) {

                val json = JSONObject(
                    URL(
                        "https://api.open-meteo.com/v1/forecast" +
                                "?latitude=46.0" +
                                "&longitude=-66.0" +
                                "&current=temperature_2m,wind_speed_10m"
                    ).readText()
                )

                val current = json.getJSONObject("current")

                val temp = current.optDouble(
                    "temperature_2m",
                    0.0
                )

                val wind = current.optDouble(
                    "wind_speed_10m",
                    0.0
                )

                "${temp.toInt()}°C • Wind ${wind.toInt()} km/h"
            }

        } catch (_: Exception) {

            "Weather temporarily unavailable"
        }
    }

    // ---------------------------------------------------------
    // FUEL PRICES
    // ---------------------------------------------------------

    LaunchedEffect(Unit) {

        fuelPrices = try {

            withContext(Dispatchers.IO) {

                val html = URL(
                    "https://nbeub.ca"
                ).readText()

                val plainText = html
                    .replace(Regex("<[^>]*>"), " ")
                    .replace("&nbsp;", " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()

                val regularMatch = Regex(
                    "Regular Gasoline\\s+Self-serve\\s+([0-9]+\\.?[0-9]*)",
                    RegexOption.IGNORE_CASE
                ).find(plainText)

                val dieselMatch = Regex(
                    "Ultra-low Sulphur Diesel\\s+Self-serve\\s+([0-9]+\\.?[0-9]*)",
                    RegexOption.IGNORE_CASE
                ).find(plainText)

                if (
                    regularMatch != null &&
                    dieselMatch != null
                ) {

                    val regular =
                        regularMatch.groupValues[1]

                    val diesel =
                        dieselMatch.groupValues[1]

                    "Regular $regular¢/L • Diesel $diesel¢/L"

                } else {

                    "Tap to check official prices"
                }
            }

        } catch (_: Exception) {

            "Tap to check official prices"
        }
    }

    // ---------------------------------------------------------
    // SCHOOL / BUS ALERTS
    // ---------------------------------------------------------

    LaunchedEffect(Unit) {

        schoolAlerts = try {

            withContext(Dispatchers.IO) {

                val html = URL(
                    "https://asdw.nbed.ca/news/alerts-dashboard/"
                ).readText()

                val plainText = html
                    .replace(Regex("<[^>]*>"), " ")
                    .replace("&nbsp;", " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()

                val alertMatch = Regex(
                    "(Bus\\s+#?3\\d{2}\\s+.*?running.*?late|Delay.*?Zone 8|Closure.*?Zone 8)",
                    RegexOption.IGNORE_CASE
                ).find(plainText)

                alertMatch
                    ?.groupValues
                    ?.get(1)
                    ?.trim()
                    ?: "✅ No active Minto / Zone 8 alerts"
            }

        } catch (_: Exception) {

            "ASD-W alerts unavailable"
        }
    }

    // ---------------------------------------------------------
    // APP TILES
    // ---------------------------------------------------------

    val tiles = listOf(

        Tile(
            "Weather",
            "🌦",
            weather,
            null
        ),

        Tile(
            "Police",
            "🚓",
            "Public RCMP information",
            "https://rcmp.ca"
        ),

        Tile(
            "Fire",
            "🔥",
            "Public fire information",
            "https://www.arcgis.com"
        ),

        Tile(
            "Ambulance",
            "🚑",
            "Public emergency information",
            "https://www.gnb.ca"
        ),

        Tile(
            "Minto School",
            "🎒",
            schoolAlerts,
            "https://asdw.nbed.ca/news/alerts-dashboard/"
        ),

        Tile(
            "Roads",
            "🛣",
            "NB 511 conditions & incidents",
            "https://www.gnb.ca"
        ),

        Tile(
            "Fuel",
            "⛽",
            fuelPrices,
            "https://nbeub.ca"
        )
    )

    // ---------------------------------------------------------
    // USER INTERFACE
    // ---------------------------------------------------------

    MaterialTheme {

        Scaffold(

            topBar = {

                TopAppBar(
                    title = {
                        Text("Grand Lake Alert")
                    }
                )
            }

        ) { paddingValues ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {

                Text(
                    text = "AREA",
                    style = MaterialTheme.typography.labelLarge
                )

                Text(
                    text = area,
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                LazyVerticalGrid(

                    columns = GridCells.Fixed(2),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)

                ) {

                    items(tiles) { tile ->

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {

                                Text(
                                    text = tile.icon,
                                    style = MaterialTheme
                                        .typography
                                        .headlineMedium
                                )

                                Text(
                                    text = tile.title,
                                    style = MaterialTheme
                                        .typography
                                        .titleMedium
                                )

                                Text(
                                    text = tile.subtitle,
                                    style = MaterialTheme
                                        .typography
                                        .bodySmall
                                )

                                if (tile.url != null) {

                                    Spacer(
                                        modifier = Modifier.height(8.dp)
                                    )

                                    TextButton(

                                        onClick = {

                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(tile.url)
                                            )

                                            context.startActivity(intent)
                                        }

                                    ) {

                                        Text("Open source")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```
