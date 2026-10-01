package ca.grandlake.alert

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

    var weather by remember {
        mutableStateOf("Loading weather…")
    }

    var fuelPrices by remember {
        mutableStateOf("Loading official prices…")
    }

    var schoolAlerts by remember {
        mutableStateOf("Checking Minto / Zone 8 alerts…")
    }

    /*
     * WEATHER
     */
    LaunchedEffect(Unit) {

        weather = try {

            withContext(Dispatchers.IO) {

                val url =
                    "https://api.open-meteo.com/v1/forecast" +
                            "?latitude=46.00" +
                            "&longitude=-66.05" +
                            "&current=temperature_2m,weather_code,wind_speed_10m" +
                            "&timezone=America%2FHalifax"

                val json = JSONObject(
                    URL(url).readText()
                )

                val current = json.getJSONObject("current")

                val temperature =
                    current.getDouble("temperature_2m")

                val wind =
                    current.getDouble("wind_speed_10m")

                "${temperature.toInt()}°C • Wind ${wind.toInt()} km/h"
            }

        } catch (_: Exception) {

            "Weather unavailable"
        }
    }

    /*
     * FUEL PRICES
     */
    LaunchedEffect(Unit) {

        fuelPrices = try {

            withContext(Dispatchers.IO) {

                val html =
                    URL("https://nbeub.ca/current-petroleum-prices-2")
                        .readText()

                val plainText = html
                    .replace(Regex("<[^>]*>"), " ")
                    .replace("&nbsp;", " ")
                    .replace("&amp;", "&")
                    .replace(Regex("\\s+"), " ")
                    .trim()

                /*
                 * Look for the official current maximum
                 * regular gasoline price.
                 */
                val regularMatch = Regex(
                    "Current Max\\s+Regular\\s+Self-serve\\s+is\\s+#\\s*([0-9]+(?:\\.[0-9]+)?)",
                    RegexOption.IGNORE_CASE
                ).find(plainText)

                /*
                 * Look for the official current maximum
                 * diesel price.
                 */
                val dieselMatch = Regex(
                    "Current Max\\s+ultra-low sulphur diesel\\s+Self-serve\\s+is\\s+#\\s*([0-9]+(?:\\.[0-9]+)?)",
                    RegexOption.IGNORE_CASE
                ).find(plainText)

                if (regularMatch != null && dieselMatch != null) {

                    val regular =
                        regularMatch.groupValues[1]

                    val diesel =
                        dieselMatch.groupValues[1]

                    "Regular ${regular}¢/L • Diesel ${diesel}¢/L"

                } else {

                    /*
                     * Backup search in case the wording
                     * on the official page changes.
                     */
                    val regularBackup = Regex(
                        "Regular Gasoline\\s+Self-serve\\s+([0-9]+(?:\\.[0-9]+)?)",
                        RegexOption.IGNORE_CASE
                    ).find(plainText)

                    val dieselBackup = Regex(
                        "Ultra-low Sulphur Diesel\\s+Self-serve\\s+([0-9]+(?:\\.[0-9]+)?)",
                        RegexOption.IGNORE_CASE
                    ).find(plainText)

                    if (regularBackup != null && dieselBackup != null) {

                        "Regular ${regularBackup.groupValues[1]}¢/L • Diesel ${dieselBackup.groupValues[1]}¢/L"

                    } else {

                        "Tap for official prices"
                    }
                }
            }

        } catch (_: Exception) {

            "Tap for official prices"
        }
    }

    /*
     * MINTO / ZONE 8 SCHOOL & BUS ALERTS
     */
    LaunchedEffect(Unit) {

        schoolAlerts = try {

            withContext(Dispatchers.IO) {

                val html =
                    URL("https://asdw.nbed.ca/news/alerts-dashboard/")
                        .readText()

                val plainText = html
                    .replace(Regex("<[^>]*>"), " ")
                    .replace("&nbsp;", " ")
                    .replace("&amp;", "&")
                    .replace(Regex("\\s+"), " ")
                    .trim()

                val lowerText =
                    plainText.lowercase()

                /*
                 * Look for Minto / Zone 8 information.
                 */
                when {

                    lowerText.contains("minto") &&
                            lowerText.contains("alert") -> {

                        "⚠️ Minto / Zone 8 alert"
                    }

                    lowerText.contains("zone 8") &&
                            lowerText.contains("alert") -> {

                        "⚠️ Minto / Zone 8 alert"
                    }

                    lowerText.contains("bus") &&
                            lowerText.contains("delay") -> {

                        "⚠️ Bus delay / alert"
                    }

                    lowerText.contains("cancellation") -> {

                        "⚠️ School / bus cancellation"
                    }

                    else -> {

                        "✅ No active Minto / Zone 8 alerts"
                    }
                }
            }

        } catch (_: Exception) {

            "Tap for current alerts"
        }
    }

    /*
     * MAIN TILES
     */
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
            "https://rcmp.ca/en/nb/news"
        ),

        Tile(
            "Fire",
            "🔥",
            "Public fire information",
            "https://nbdnr.maps.arcgis.com/apps/dashboards/7bb8645cf75c4aa2b7a43a3123f9e17f#locale=en-CA"
        ),

        Tile(
            "Ambulance",
            "🚑",
            "Public emergency information",
            "https://www2.gnb.ca/content/gnb/en/departments/health.html"
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
            "https://511.gnb.ca/"
        ),

        Tile(
            "Fuel",
            "⛽",
            fuelPrices,
            "https://nbeub.ca/current-petroleum-prices-2"
        )
    )

    /*
     * APP SCREEN
     */
    Scaffold(

        topBar = {

            TopAppBar(
                title = {
                    Text("Grand Lake Alert")
                }
            )
        }

    ) { paddingValues ->

        LazyVerticalGrid(

            columns = GridCells.Fixed(2),

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),

            contentPadding = PaddingValues(8.dp),

            horizontalArrangement =
                Arrangement.spacedBy(8.dp),

            verticalArrangement =
                Arrangement.spacedBy(8.dp)

        ) {

            items(tiles) { tile ->

                AlertTile(
                    tile = tile,
                    onClick = {

                        tile.url?.let { url ->

                            val intent =
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(url)
                                )

                            context.startActivity(intent)
                        }
                    }
                )
            }
        }
    }
}

/*
 * INDIVIDUAL ALERT TILE
 */
@Composable
fun AlertTile(
    tile: Tile,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {

        Column(
            modifier = Modifier
                .padding(12.dp)
        ) {

            Text(
                text = tile.icon,
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = tile.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = tile.subtitle,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (tile.url != null) {

                TextButton(
                    onClick = onClick,
                    modifier = Modifier
                        .padding(top = 2.dp)
                ) {

                    Text("Open source")
                }
            }
        }
    }
}
