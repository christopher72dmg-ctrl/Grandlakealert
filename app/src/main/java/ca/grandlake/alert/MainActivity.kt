package ca.grandlake.alert

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

```
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
        GrandLakeAlertApp()
    }
}
```

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrandLakeAlertApp() {

```
val context = LocalContext.current

var weather by remember {
    mutableStateOf("Loading weather...")
}

var fuelPrices by remember {
    mutableStateOf("Loading prices...")
}

var schoolAlerts by remember {
    mutableStateOf("Checking school alerts...")
}

var policeNews by remember {
    mutableStateOf("Latest RCMP information")
}

var fireInfo by remember {
    mutableStateOf("Checking wildfire information...")
}

LaunchedEffect(Unit) {

    weather = try {

        withContext(Dispatchers.IO) {

            val url =
                "https://api.open-meteo.com/v1/forecast" +
                "?latitude=46.0" +
                "&longitude=-66.0" +
                "&current=temperature_2m,wind_speed_10m"

            val json =
                JSONObject(URL(url).readText())

            val current =
                json.getJSONObject("current")

            val temperature =
                current.optDouble(
                    "temperature_2m",
                    0.0
                )

            val wind =
                current.optDouble(
                    "wind_speed_10m",
                    0.0
                )

            "${temperature.toInt()}°C  •  Wind ${wind.toInt()} km/h"
        }

    } catch (_: Exception) {

        "Weather temporarily unavailable"
    }
}

LaunchedEffect(Unit) {

    fuelPrices = try {

        withContext(Dispatchers.IO) {

            val html =
                URL(
                    "https://nbeub.ca/current-petroleum-prices-2"
                ).readText()

            val text =
                html
                    .replace(
                        Regex("<[^>]*>"),
                        " "
                    )
                    .replace(
                        "&nbsp;",
                        " "
                    )
                    .replace(
                        Regex("\\s+"),
                        " "
                    )
                    .trim()

            val regular =
                Regex(
                    "Regular Gasoline\\s+Self-serve\\s+([0-9]+\\.[0-9])",
                    RegexOption.IGNORE_CASE
                ).find(text)

            val diesel =
                Regex(
                    "Ultra-low Sulphur Diesel\\s+Self-serve\\s+([0-9]+\\.[0-9])",
                    RegexOption.IGNORE_CASE
                ).find(text)

            if (
                regular != null &&
                diesel != null
            ) {

                "Regular: ${regular.groupValues[1]}¢/L\n" +
                "Diesel: ${diesel.groupValues[1]}¢/L"

            } else {

                "Fuel prices unavailable"
            }
        }

    } catch (_: Exception) {

        "Fuel prices unavailable"
    }
}

LaunchedEffect(Unit) {

    schoolAlerts = try {

        withContext(Dispatchers.IO) {

            val html =
                URL(
                    "https://asdw.nbed.ca/news/alerts-dashboard/"
                ).readText()

            val text =
                html
                    .replace(
                        Regex("<[^>]*>"),
                        " "
                    )
                    .replace(
                        "&nbsp;",
                        " "
                    )
                    .replace(
                        Regex("\\s+"),
                        " "
                    )
                    .trim()

            val alert =
                Regex(
                    "(Bus\\s+#?3\\d{2}.*?running.*?late|Delay.*?Zone 8|Closure.*?Zone 8)",
                    RegexOption.IGNORE_CASE
                ).find(text)

            alert?.groupValues?.get(1)?.trim()
                ?: "No active Minto / Zone 8 alerts"
        }

    } catch (_: Exception) {

        "School alerts unavailable"
    }
}

LaunchedEffect(Unit) {

    policeNews = try {

        withContext(Dispatchers.IO) {

            val html =
                URL(
                    "https://rcmp.ca/en/nb/news"
                ).readText()

            val text =
                html
                    .replace(
                        Regex("<[^>]*>"),
                        " "
                    )
                    .replace(
                        Regex("\\s+"),
                        " "
                    )
                    .trim()

            val words =
                text.take(180)

            "RCMP NB: $words"
        }

    } catch (_: Exception) {

        "Latest RCMP information unavailable"
    }
}

LaunchedEffect(Unit) {

    fireInfo = try {

        withContext(Dispatchers.IO) {

            val url =
                "https://gis-erd-der.gnb.ca/gisserver/rest/services/" +
                "New_Brunswick_Fires/" +
                "New_Brunswick_Fire_Locations/" +
                "FeatureServer/0/query" +
                "?where=1%3D1" +
                "&outFields=FIELD_FIRE_NAME" +
                "&returnGeometry=false" +
                "&f=json"

            val json =
                JSONObject(
                    URL(url).readText()
                )

            val features =
                json.optJSONArray("features")

            val count =
                features?.length() ?: 0

            if (count == 0) {

                "No wildfire locations reported"

            } else {

                "$count NB wildfire location" +
                if (count == 1) "" else "s"
            }
        }

    } catch (_: Exception) {

        "Wildfire information unavailable"
    }
}

val tiles = listOf(

    Tile(
        title = "Weather",
        icon = "🌦️",
        subtitle = weather,
        url = null,
        accent = Color(0xFF42A5F5)
    ),

    Tile(
        title = "Minto School",
        icon = "🏫",
        subtitle = schoolAlerts,
        url = "https://asdw.nbed.ca/news/alerts-dashboard/",
        accent = Color(0xFF66BB6A)
    ),

    Tile(
        title = "Fuel Prices",
        icon = "⛽",
        subtitle = fuelPrices,
        url = "https://nbeub.ca/current-petroleum-prices-2",
        accent = Color(0xFFFFC107)
    ),

    Tile(
        title = "Road Conditions",
        icon = "🛣️",
        subtitle = "Hwy 10 • Traffic • Accidents • Closures • Construction",
        url = "https://511.gnb.ca/roadconditions?start=0&length=25&order%5Bi%5D=1&order%5Bdir%5D=asc&search=10",
        accent = Color(0xFFFF9800)
    ),

    Tile(
        title = "Police",
        icon = "🚓",
        subtitle = policeNews,
        url = "https://rcmp.ca/en/nb/news",
        accent = Color(0xFFEF5350)
    ),

    Tile(
        title = "Fire",
        icon = "🔥",
        subtitle = fireInfo,
        url = "https://nbdnr.maps.arcgis.com/apps/dashboards/7bb8645cf75c4aa2b7a43a3123f9e17f#locale=en-CA",
        accent = Color(0xFFFF7043)
    )
)

val colors =
    darkColorScheme(
        primary = Color(0xFF69F0AE),
        secondary = Color(0xFF80CBC4),
        background = Color(0xFF101214),
        surface = Color(0xFF181B1F),
        onBackground = Color.White,
        onSurface = Color.White
    )

MaterialTheme(
    colorScheme = colors
) {

    Scaffold(

        containerColor =
            Color(0xFF101214),

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text = "Grand Lake Alert",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge
                        )

                        Text(
                            text = "LOCAL INFORMATION",
                            color =
                                Color(0xFF69F0AE),
                            style =
                                MaterialTheme
                                    .typography
                                    .labelSmall
                        )
                    }
                },

                colors =
                    TopAppBarDefaults
                        .topAppBarColors(
                            containerColor =
                                Color(0xFF181B1F),
                            titleContentColor =
                                Color.White
                        )
            )
        }

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(12.dp)
        ) {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Grand Lake, NB",
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        color =
                            Color.White
                    )

                    Text(
                        text =
                            "Live local information",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            Color.LightGray
                    )
                }

                Text(
                    text = "● LIVE",
                    color =
                        Color(0xFF69F0AE),
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium
                )
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            LazyVerticalGrid(

                columns =
                    GridCells.Fixed(2),

                modifier =
                    Modifier.fillMaxSize(),

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp),

                verticalArrangement =
                    Arrangement.spacedBy(12.dp)

            ) {

                items(tiles) { tile ->

                    AlertTile(
                        tile = tile,
                        context = context
                    )
                }
            }
        }
    }
}
```

}

@Composable
fun AlertTile(
tile: Tile,
context: android.content.Context
) {

```
Card(

    modifier =
        Modifier
            .fillMaxWidth()
            .height(235.dp),

    shape =
        RoundedCornerShape(20.dp),

    colors =
        CardDefaults.cardColors(
            containerColor =
                Color(0xFF181B1F)
        ),

    elevation =
        CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )

) {

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(15.dp)

    ) {

        Box(

            modifier =
                Modifier
                    .size(48.dp)
                    .background(
                        tile.accent.copy(
                            alpha = 0.16f
                        ),
                        RoundedCornerShape(14.dp)
                    ),

            contentAlignment =
                Alignment.Center

        ) {

            Text(
                text = tile.icon,
                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )
        }

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        Text(

            text =
                tile.title,

            style =
                MaterialTheme
                    .typography
                    .titleMedium,

            color =
                tile.accent,

            maxLines = 2
        )

        Spacer(
            modifier =
                Modifier.height(6.dp)
        )

        Text(

            text =
                tile.subtitle,

            style =
                MaterialTheme
                    .typography
                    .bodySmall,

            color =
                Color.LightGray,

            maxLines = 5
        )

        Spacer(
            modifier =
                Modifier.weight(1f)
        )

        if (tile.url != null) {

            TextButton(

                onClick = {

                    try {

                        val intent =
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(tile.url)
                            )

                        context.startActivity(intent)

                    } catch (_: Exception) {
                    }
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)

            ) {

                Text(
                    text =
                        "OPEN SOURCE",
                    color =
                        tile.accent
                )
            }
        }
    }
}
```

}
