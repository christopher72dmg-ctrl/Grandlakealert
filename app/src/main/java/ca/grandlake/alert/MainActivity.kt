package ca.grandlake.alert

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.net.URLEncoder

data class Tile(
    val title: String,
    val icon: String,
    val subtitle: String,
    val url: String?,
    val accent: Color,
    val chat: Boolean = false,
    val weather: Boolean = false
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
        mutableStateOf("Loading current weather…")
    }

    var fuelPrices by remember {
        mutableStateOf("Loading current prices…")
    }

    var schoolAlerts by remember {
        mutableStateOf("Checking school alerts…")
    }

    var policeNews by remember {
        mutableStateOf("Checking latest RCMP news…")
    }

    var policeUrl by remember {
        mutableStateOf("https://rcmp.ca/en/nb/news")
    }

    var fireInfo by remember {
        mutableStateOf("Checking NB wildfire information…")
    }

    var aircraftInfo by remember {
        mutableStateOf("Loading nearby aircraft…")
    }

    var outageInfo by remember {
        mutableStateOf("Checking NB Power outage information…")
    }

    var showTiles by remember {
        mutableStateOf(false)
    }

    var showChat by remember {
        mutableStateOf(false)
    }

    var showWeather by remember { mutableStateOf(false) }

    var showSources by remember { mutableStateOf(false) }

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
                val temperature =
                    current.optDouble("temperature_2m", 0.0)
                val wind =
                    current.optDouble("wind_speed_10m", 0.0)

                "${temperature.toInt()}°C • Wind ${wind.toInt()} km/h"
            }
        } catch (_: Exception) {
            "Weather temporarily unavailable"
        }
    }

    LaunchedEffect(Unit) {
        fuelPrices = try {
            withContext(Dispatchers.IO) {

                val html =
                    URL("https://nbeub.ca/current-petroleum-prices-2")
                        .readText()

                val pageText = html
                    .replace(Regex("<[^>]*>"), " ")
                    .replace("&nbsp;", " ")
                    .replace("&amp;", "&")
                    .replace(Regex("\\s+"), " ")
                    .trim()

                val regularMatch = Regex(
                    "Regular Gasoline\\s+Self-serve\\s+([0-9]+\\.[0-9])",
                    RegexOption.IGNORE_CASE
                ).find(pageText)

                val dieselMatch = Regex(
                    "Ultra-low Sulphur Diesel\\s+Self-serve\\s+([0-9]+\\.[0-9])",
                    RegexOption.IGNORE_CASE
                ).find(pageText)

                if (regularMatch != null && dieselMatch != null) {

                    val regular = regularMatch.groupValues[1]
                    val diesel = dieselMatch.groupValues[1]

                    "Regular: $regular¢/L\nDiesel: $diesel¢/L"

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
                    URL("https://asdw.nbed.ca/news/alerts-dashboard/")
                        .readText()

                val pageText = html
                    .replace(Regex("<[^>]*>"), " ")
                    .replace("&nbsp;", " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()

                val alertMatch = Regex(
                    "(Bus\\s+#?3\\d{2}\\s+.*?running.*?late|Delay.*?Zone 8|Closure.*?Zone 8)",
                    RegexOption.IGNORE_CASE
                ).find(pageText)

                alertMatch?.groupValues?.get(1)?.trim()
                    ?: "✅ No active Minto / Zone 8 alerts"
            }
        } catch (_: Exception) {
            "School alerts unavailable"
        }
    }

    LaunchedEffect(Unit) {

        try {

            val result = withContext(Dispatchers.IO) {

                val feedUrl =
                    "https://rcmp.ca/en/feed-flux/news-nouvelles/division/j"

                val parserFactory =
                    XmlPullParserFactory.newInstance()

                val parser =
                    parserFactory.newPullParser()

                parser.setInput(
                    URL(feedUrl).openStream(),
                    "UTF-8"
                )

                var eventType = parser.eventType
                var insideEntry = false
                var insideTitle = false
                var latestTitle = ""
                var latestUrl = ""

                while (
                    eventType != XmlPullParser.END_DOCUMENT
                ) {

                    when (eventType) {

                        XmlPullParser.START_TAG -> {

                            when (parser.name.lowercase()) {

                                "entry" -> {
                                    insideEntry = true
                                }

                                "title" -> {
                                    if (insideEntry) {
                                        insideTitle = true
                                    }
                                }

                                "link" -> {

                                    if (insideEntry) {

                                        val href =
                                            parser.getAttributeValue(
                                                null,
                                                "href"
                                            )

                                        if (
                                            !href.isNullOrBlank() &&
                                            latestUrl.isBlank()
                                        ) {
                                            latestUrl = href
                                        }
                                    }
                                }
                            }
                        }

                        XmlPullParser.TEXT -> {

                            if (
                                insideEntry &&
                                insideTitle
                            ) {
                                latestTitle +=
                                    parser.text.trim()
                            }
                        }

                        XmlPullParser.END_TAG -> {

                            when (parser.name.lowercase()) {

                                "title" -> {
                                    insideTitle = false
                                }

                                "entry" -> {

                                    if (insideEntry) {
                                        insideEntry = false
                                        break
                                    }
                                }
                            }
                        }
                    }

                    eventType = parser.next()
                }

                Pair(
                    latestTitle.trim(),
                    latestUrl.trim()
                )
            }

            if (result.first.isNotBlank()) {

                policeNews =
                    "Latest: ${result.first}"

                if (result.second.isNotBlank()) {
                    policeUrl = result.second
                }

            } else {

                policeNews =
                    "Latest RCMP news unavailable"
            }

        } catch (_: Exception) {

            policeNews =
                "RCMP news temporarily unavailable"
        }
    }

    LaunchedEffect(Unit) {

        fireInfo = try {

            withContext(Dispatchers.IO) {

                val fireUrl =
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
                        URL(fireUrl).readText()
                    )

                val features =
                    json.optJSONArray("features")

                val count =
                    features?.length() ?: 0

                if (count == 0) {

                    "✅ No wildfire locations reported"

                } else {

                    "🔥 $count NB wildfire location" +
                        if (count == 1) "" else "s"
                }
            }

        } catch (_: Exception) {

            "Wildfire information unavailable"
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            outageInfo = try {
                withContext(Dispatchers.IO) {
                    val connection = URL(
                        "https://www.nbpower.com/Open/Outages.aspx?lang=en"
                    ).openConnection().apply {
                        connectTimeout = 10000
                        readTimeout = 10000
                        setRequestProperty("User-Agent", "GrandLakeAlert/1.0")
                    }

                    val html = connection.getInputStream().bufferedReader().use { it.readText() }

                    fun cleanCell(value: String): String {
                        return value
                            .replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
                            .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
                            .replace(Regex("(?is)<[^>]*>"), " ")
                            .replace("&nbsp;", " ")
                            .replace("&#160;", " ")
                            .replace("&amp;", "&")
                            .replace("&gt;", ">")
                            .replace("&lt;", "<")
                            .replace("&quot;", "\"")
                            .replace(Regex("\\s+"), " ")
                            .trim()
                    }

                    val rows = Regex("(?is)<tr\\b[^>]*>(.*?)</tr>")
                        .findAll(html)
                        .map { row ->
                            Regex("(?is)<t[dh]\\b[^>]*>(.*?)</t[dh]>")
                                .findAll(row.groupValues[1])
                                .map { cleanCell(it.groupValues[1]) }
                                .toList()
                        }
                        .filter { it.isNotEmpty() }
                        .toList()

                    val summary = rows.firstOrNull {
                        it.firstOrNull()?.contains("Summary", ignoreCase = true) == true
                    }
                    val localRegion = rows.firstOrNull {
                        it.firstOrNull()?.contains("Central York Sunbury", ignoreCase = true) == true
                    }
                    val queensCounty = rows.firstOrNull {
                        it.firstOrNull()?.contains("Kings/Queens", ignoreCase = true) == true
                    }

                    fun outageLine(label: String, cells: List<String>?): String? {
                        if (cells == null || cells.size < 3) return null
                        val numbers = cells.drop(1).mapNotNull { cell ->
                            Regex("^\\d+$").find(cell)?.value
                        }
                        if (numbers.size < 2) return null
                        return "$label: ${numbers[0]} outage(s) • ${numbers[1]} customers affected"
                    }

                    val updatedMatch = Regex(
                        "(?is)Outage Data Last Updated at:?\\s*</?[^>]*>?\\s*([^<\\r\n]{4,50})"
                    ).find(html)
                    val updatedText = updatedMatch?.groupValues?.getOrNull(1)
                        ?.let { cleanCell(it) }
                        ?.takeIf { it.isNotBlank() }

                    val lines = mutableListOf<String>()
                    outageLine("NB-wide", summary)?.let { lines.add(it) }
                    outageLine("Central York Sunbury", localRegion)?.let { lines.add(it) }
                    outageLine("Queens County (Kings/Queens region)", queensCounty)?.let { lines.add(it) }
                    if (updatedText != null) lines.add("NB Power updated: $updatedText")

                    if (lines.isEmpty()) {
                        "NB Power page reachable\nOpen source for current outage details"
                    } else {
                        lines.joinToString("\n")
                    }
                }
            } catch (_: Exception) {
                "NB Power data unavailable\nTap OPEN SOURCE to check the official map"
            }

            kotlinx.coroutines.delay(10 * 60 * 1000L)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            aircraftInfo = try {
                withContext(Dispatchers.IO) {
                    val connection = URL(
                        "https://api.adsb.lol/v2/lat/46.0/lon/-66.0/dist/50"
                    ).openConnection().apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                    }

                    val json = JSONObject(
                        connection.getInputStream().bufferedReader().use { it.readText() }
                    )
                    val aircraft = json.optJSONArray("ac")
                    val reports = mutableListOf<Pair<Double, String>>()

                    if (aircraft != null) {
                        for (i in 0 until aircraft.length()) {
                            val plane = aircraft.optJSONObject(i) ?: continue
                            val seen = plane.optDouble("seen_pos", 9999.0)
                            if (seen > 120.0) continue

                            val callsign = plane.optString("flight", "").trim()
                                .ifBlank { plane.optString("r", "").trim() }
                                .ifBlank { "Unknown aircraft" }
                            val type = plane.optString("t", "").trim()
                            val altitudeValue = plane.opt("alt_baro")
                            val altitude = if (altitudeValue is Number) {
                                "${altitudeValue.toInt()} ft"
                            } else if (altitudeValue?.toString() == "ground") {
                                "on ground"
                            } else {
                                "altitude n/a"
                            }
                            val distance = plane.optDouble("dst", -1.0)
                            val distanceText = if (distance >= 0.0) {
                                "${distance.toInt()} nm"
                            } else {
                                "distance n/a"
                            }
                            val typeText = if (type.isNotBlank()) " $type" else ""
                            val line = "$callsign$typeText • $altitude • $distanceText"
                            reports.add(seen to line)
                        }
                    }

                    val nearest = reports.sortedBy { it.first }.take(2)
                    if (nearest.isEmpty()) {
                        "No recent aircraft positions detected\nCoverage depends on nearby receivers"
                    } else {
                        "Nearby aircraft • refreshes each minute\n" +
                            nearest.joinToString("\n") { it.second }
                    }
                }
            } catch (_: Exception) {
                "Live aircraft data unavailable\nTap OPEN SOURCE to view the map"
            }

            kotlinx.coroutines.delay(60_000)
        }
    }

    LaunchedEffect(Unit) {

        kotlinx.coroutines.delay(250)

        showTiles = true
    }

    val tiles = listOf(

        Tile(
            title = "Minto School",
            icon = "🏫",
            subtitle = schoolAlerts,
            url = "https://asdw.nbed.ca/news/alerts-dashboard/",
            accent = Color(0xFF66BB6A)
        ),

        Tile(
            title = "Fuel",
            icon = "⛽",
            subtitle = fuelPrices,
            url = "https://nbeub.ca/current-petroleum-prices-2",
            accent = Color(0xFFFFC107)
        ),

        Tile(
            title = "Hwy 10",
            icon = "🛣️",
            subtitle = "Current Hwy 10 road conditions",
            url = "https://511.gnb.ca/roadconditions?start=0&length=25&order%5Bi%5D=1&order%5Bdir%5D=asc&search=10",
            accent = Color(0xFFFF9800)
        ),

        Tile(
            title = "Police",
            icon = "🚓",
            subtitle = policeNews,
            url = policeUrl,
            accent = Color(0xFFEF5350)
        ),

        Tile(
            title = "Fire",
            icon = "🔥",
            subtitle = fireInfo,
            url = "https://nbdnr.maps.arcgis.com/apps/dashboards/7bb8645cf75c4aa2b7a43a3123f9e17f#locale=en-CA",
            accent = Color(0xFFFF7043)
        ),

        Tile(
            title = "Traffic & Accidents",
            icon = "🚗",
            subtitle = "Live NB traffic events, accidents, closures & construction",
            url = "https://511.gnb.ca/list/events/traffic",
            accent = Color(0xFFAB47BC)
        ),
        Tile(
            title = "Power Outages",
            icon = "⚡",
            subtitle = outageInfo,
            url = "https://www.nbpower.com/Open/Outages.aspx",
            accent = Color(0xFFFFC107)
        ),

        Tile(
            title = "Aircraft Overhead",
            icon = "✈️",
            subtitle = aircraftInfo,
            url = "https://globe.adsb.lol/?lat=46.0&lon=-66.0&zoom=8",
            accent = Color(0xFF42A5F5)
        ),

        Tile(
            title = "Offline Chat",
            icon = "📡",
            subtitle = "Nearby phone chat • No Internet required",
            url = null,
            accent = Color(0xFF29B6F6),
            chat = true
        )
    )

    val darkColors = darkColorScheme(
        primary = Color(0xFF69F0AE),
        secondary = Color(0xFF80CBC4),
        background = Color(0xFF101214),
        surface = Color(0xFF181B1F),
        surfaceVariant = Color(0xFF24282D),
        onBackground = Color.White,
        onSurface = Color.White
    )

    MaterialTheme(
        colorScheme = darkColors
    ) {

        if (showSources) {
            AlertDialog(
                onDismissRequest = { showSources = false },
                containerColor = Color(0xFF24282D),
                title = {
                    Text("Sources & Information", color = Color.White)
                },
                text = {
                    Text(
                        "Grand Lake Alert gathers local information from public sources. Tap SOURCE on any tile to open its official information page. Weather and forecast data come from public weather services; solar/radio activity uses NOAA data. School alerts, fuel prices, road conditions, RCMP news, wildfire information, NB Power outages, and aircraft reports link to their respective public sources.\n\nInformation may be delayed or unavailable. Always check the official source for urgent or emergency decisions.",
                        color = Color.LightGray
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showSources = false }) {
                        Text("GOT IT", color = Color(0xFF69F0AE))
                    }
                }
            )
        } else if (showChat) {
            OfflineChatScreen(onBack = { showChat = false })
        } else {
            Scaffold(

            containerColor = Color(0xFF101214),

            topBar = {

                TopAppBar(

                    title = {

                        Column {

                            Text(
                                text = "Grand Lake Alert",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Text(
                                text = "LOCAL INFORMATION",
                                color = Color(0xFF69F0AE),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },

                    actions = {
                        StatusIndicator()
                        Spacer(modifier = Modifier.size(8.dp))
                        TextButton(onClick = { showSources = true }) {
                            Text(
                                text = "ⓘ",
                                color = Color(0xFF80CBC4),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },

                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF181B1F),
                        titleContentColor = Color.White
                    )
                )
            }

        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp)
            ) {

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                DateTimeBanner()

                WeatherDashboardScreen(onBack = {})

                Spacer(modifier = Modifier.height(8.dp))

                Spacer(modifier = Modifier.height(4.dp))

                LazyVerticalGrid(

                    columns = GridCells.Fixed(2),

                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp),

                    content = {

                        items(tiles) { tile ->

                            AnimatedVisibility(

                                visible = showTiles,

                                enter =
                                    fadeIn(
                                        animationSpec =
                                            tween(500)
                                    ) +
                                        slideInVertically(
                                            initialOffsetY = {
                                                80
                                            },
                                            animationSpec =
                                                tween(500)
                                        )
                            ) {

                                AlertTile(
                                    tile = tile,
                                    context = context,
                                    onChat = { if (tile.chat) showChat = true },
                                    onWeather = { if (tile.weather) showWeather = !showWeather },
                                    weatherExpanded = showWeather
                                )
                            }
                        }
                    }
                )
            }
        }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDashboardScreen(onBack: () -> Unit) {
    var now by remember { mutableStateOf("Loading current conditions…") }
    var tomorrow by remember { mutableStateOf("Loading tomorrow’s forecast…") }
    var alerts by remember { mutableStateOf("Checking Environment Canada…") }
    var frost by remember { mutableStateOf("Checking overnight frost risk…") }
    var moon by remember { mutableStateOf("Checking moon phase…") }
    var solar by remember { mutableStateOf("Checking solar weather…") }
    var updated by remember { mutableStateOf("") }
    var tideTicker by remember { mutableStateOf("Loading official Saint John tide predictions…") }
    var seasonalBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        seasonalBitmap = withContext(Dispatchers.IO) {
            try {
                val month = java.time.LocalDate.now(ZoneId.of("America/Moncton")).monthValue
                val photo = when (month) {
                    12, 1, 2 -> "photo-1517299321609-52687d1bc55a"
                    3, 4, 5 -> "photo-1470770841072-f978cf4d019e"
                    6, 7, 8 -> "photo-1500530855697-b586d89ba3ee"
                    else -> "photo-1501785888041-af3ef285b470"
                }
                val imageUrl = URL("https://images.unsplash.com/" + photo + "?auto=format&fit=crop&w=1200&q=75")
                imageUrl.openConnection().apply { connectTimeout = 5000; readTimeout = 5000 }
                    .getInputStream().use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) { null }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val data = withContext(Dispatchers.IO) {
                    val url = "https://api.open-meteo.com/v1/forecast?latitude=46.0&longitude=-66.0" +
                        "&current=temperature_2m,apparent_temperature,wind_speed_10m,relative_humidity_2m,weather_code" +
                        "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset,moon_phase" +
                        "&timezone=America%2FMoncton&forecast_days=2"
                    val conn = URL(url).openConnection().apply { connectTimeout=10000; readTimeout=10000 }
                    val j = JSONObject(conn.getInputStream().bufferedReader().use { it.readText() })
                    val c=j.getJSONObject("current"); val d=j.getJSONObject("daily")
                    val hi=d.optJSONArray("temperature_2m_max"); val lo=d.optJSONArray("temperature_2m_min")
                    val rain=d.optJSONArray("precipitation_probability_max"); val rise=d.optJSONArray("sunrise")
                    val set=d.optJSONArray("sunset"); val phase=d.optJSONArray("moon_phase")
                    fun n(v:Double)=if(v.isFinite()) v.toInt().toString() else "—"
                    fun clock(v:String?)=try { if(v.isNullOrBlank()) "—" else SimpleDateFormat("yyyy-MM-dd'T'HH:mm",Locale.CANADA).parse(v)?.let { SimpleDateFormat("h:mm a",Locale.CANADA).format(it) } ?: "—" } catch(_:Exception){"—"}
                    val code=c.optInt("weather_code",-1)
                    val desc=when(code){0->"Clear sky";1->"Mainly clear";2->"Partly cloudy";3->"Overcast";45,48->"Fog";51,53,55->"Drizzle";56,57->"Freezing drizzle";61,63,65->"Rain";66,67->"Freezing rain";71,73,75,77->"Snow";80,81,82->"Rain showers";85,86->"Snow showers";95,96,99->"Thunderstorm";else->"Conditions available"}
                    val low=lo?.optDouble(1,Double.NaN)?:Double.NaN
                    val ph=phase?.optDouble(0,Double.NaN)?:Double.NaN
                    val p=if(ph.isFinite()) ((ph%1.0)+1.0)%1.0 else -1.0
                    val moonName=when {p<0->"Moon phase unavailable";p<0.035||p>=0.965->"New Moon";p<0.22->"Waxing Crescent";p<0.28->"First Quarter";p<0.47->"Waxing Gibbous";p<0.53->"Full Moon";p<0.72->"Waning Gibbous";p<0.78->"Last Quarter";else->"Waning Crescent"}
                    val frostText=when { !low.isFinite()->"Frost estimate unavailable";low<=0->"❄️ Frost likely • forecast low ${n(low)}°C";low<=2->"⚠️ Frost possible • forecast low ${n(low)}°C";else->"Low frost risk • forecast low ${n(low)}°C" }
                    listOf(
                        "${desc}\n${n(c.optDouble("temperature_2m",Double.NaN))}°C • Feels like ${n(c.optDouble("apparent_temperature",Double.NaN))}°C\nWind ${n(c.optDouble("wind_speed_10m",Double.NaN))} km/h • Humidity ${n(c.optDouble("relative_humidity_2m",Double.NaN))}%",
                        "High ${n(hi?.optDouble(1,Double.NaN)?:Double.NaN)}°C / Low ${n(low)}°C\nPrecipitation chance: ${rain?.optInt(1,-1)?.let { if(it>=0) "$it%" else "—" } ?: "—"}",
                        "Sunrise ${clock(rise?.optString(0))} • Sunset ${clock(set?.optString(0))}",
                        moonName,
                        frostText
                    )
                }
                now=data[0]; tomorrow=data[1]; moon=data[3]+"\n"+data[2]; frost=data[4]
            } catch(_:Exception) {
                now="Weather data unavailable"; tomorrow="Forecast unavailable"
                moon="Moon/daylight data unavailable"; frost="Frost estimate unavailable"
            }

            alerts = try {
                withContext(Dispatchers.IO) {
                    val conn=URL("https://weather.gc.ca/rss/battleboard/nb14_e.xml").openConnection().apply { connectTimeout=8000; readTimeout=8000 }
                    val parser=XmlPullParserFactory.newInstance().newPullParser()
                    parser.setInput(conn.getInputStream(),"UTF-8")
                    var event=parser.eventType; var inside=false; var inTitle=false; var title=""
                    val found=mutableListOf<String>()
                    while(event!=XmlPullParser.END_DOCUMENT) {
                        when(event) {
                            XmlPullParser.START_TAG -> when(parser.name.lowercase()) { "entry","item"->inside=true; "title"->if(inside){inTitle=true;title=""} }
                            XmlPullParser.TEXT -> if(inside&&inTitle) title+=parser.text
                            XmlPullParser.END_TAG -> when(parser.name.lowercase()) {
                                "title"->{ if(inside&&inTitle){val t=title.replace(Regex("\\s+")," ").trim();if(t.isNotBlank()&&Regex("warning|watch|advisory",RegexOption.IGNORE_CASE).containsMatchIn(t)&&!t.contains("no watches or warnings",true))found.add(t)};inTitle=false}
                                "entry","item"->inside=false
                            }
                        }
                        event=parser.next()
                    }
                    if(found.isEmpty()) "No active alerts • check official forecast" else found.distinct().take(3).joinToString(" • ")
                }
            } catch(_:Exception) { "Alert feed unavailable • check official Environment Canada page" }

            solar = try {
                withContext(Dispatchers.IO) {
                    val conn=URL("https://services.swpc.noaa.gov/products/noaa-planetary-k-index.json").openConnection().apply { connectTimeout=8000; readTimeout=8000 }
                    val arr=org.json.JSONArray(conn.getInputStream().bufferedReader().use { it.readText() })
                    var kp:String?=null

                    // NOAA changed this feed to an array of named objects in 2026.
                    // Read the current format first, while retaining support for the older table format.
                    for(i in arr.length()-1 downTo 0) {
                        val row=arr.optJSONObject(i) ?: continue
                        val value=row.optString("Kp", row.optString("kp", "")).trim()
                        if(value.isNotBlank() && value!="null") { kp=value; break }
                    }

                    if(kp==null) {
                        val headers=arr.optJSONArray(0)
                        val kpIndex=if(headers!=null) (0 until headers.length()).firstOrNull { headers.optString(it).equals("Kp",true) } else null
                        if(kpIndex!=null) {
                            for(i in arr.length()-1 downTo 1) {
                                val row=arr.optJSONArray(i)?:continue
                                val value=row.optString(kpIndex).trim()
                                if(value.isNotBlank() && value!="null") { kp=value; break }
                            }
                        }
                    }

                    val k=kp?.toDoubleOrNull()
                    val kpText=k?.let { String.format(Locale.CANADA, "%.1f", it) } ?: kp
                    val kpLine = when {
                        kp==null -> "K-index data unavailable"
                        k==null -> "Latest planetary K-index: $kp"
                        k>=5 -> "Kp $kpText • Storm conditions may affect HF radio"
                        k>=4 -> "Kp $kpText • Geomagnetic activity elevated"
                        else -> "Kp $kpText • Geomagnetic activity quiet"
                    }

                    // HamQSL puts the band range in the "name" attribute and
                    // the condition as the text inside <band>, not a "condition" attribute.
                    // Keep this optional so a feed outage never hides NOAA Kp.
                    val bandLines = try {
                        val bandConn = URL("https://www.hamqsl.com/solarxml.php").openConnection().apply {
                            connectTimeout=7000
                            readTimeout=7000
                            setRequestProperty("User-Agent", "GrandLakeAlert/1.0")
                        }
                        val parser = XmlPullParserFactory.newInstance().newPullParser()
                        parser.setInput(bandConn.getInputStream(), "UTF-8")
                        val found = mutableListOf<String>()
                        val wantedRanges = listOf("80m-40m", "30m-20m", "17m-15m", "12m-10m")
                        var event = parser.eventType
                        while(event != XmlPullParser.END_DOCUMENT) {
                            if(event == XmlPullParser.START_TAG && parser.name.equals("band", true)) {
                                val band = parser.getAttributeValue(null, "name")?.trim().orEmpty()
                                val period = parser.getAttributeValue(null, "time")?.trim().orEmpty()
                                val condition = parser.nextText().trim()
                                if(band in wantedRanges && period.equals("day", true) && condition.isNotBlank()) {
                                    val label = condition.replaceFirstChar { if(it.isLowerCase()) it.titlecase(Locale.CANADA) else it.toString() }
                                    found.add("$band: $label")
                                }
                            }
                            event = parser.next()
                        }
                        found.distinct()
                    } catch(_:Exception) { emptyList<String>() }

                    (listOf(kpLine) + bandLines.ifEmpty { listOf("Ham-band conditions unavailable") })
                        .joinToString("\n")
                }
            } catch(_:Exception) { "Solar weather temporarily unavailable" }

            tideTicker = try { withContext(Dispatchers.IO) { fetchTideTicker() } } catch (_: Exception) { "Tide predictions temporarily unavailable • tap TIDES for official times" }
            updated="Updated "+SimpleDateFormat("h:mm a",Locale.CANADA).format(Date())
            kotlinx.coroutines.delay(15*60*1000L)
        }
    }
    val context=LocalContext.current
    // Compact scenic weather tile: preserve every existing weather item and ticker.
    val currentLines = now.split("\n")
    val currentSummary = currentLines.getOrElse(0) { "Current conditions" }
    val currentTemp = Regex("-?\\d+°C").find(currentLines.getOrElse(1) { "" })?.value ?: "—°C"
    val feelsLike = Regex("Feels like -?\\d+°C").find(currentLines.getOrElse(1) { "" })?.value ?: ""
    val windLine = currentLines.getOrElse(2) { "" }.substringBefore(" • ")
    val humidityLine = currentLines.getOrElse(2) { "" }.substringAfter(" • ", "")
    val weatherEmoji = when {
        currentSummary.contains("rain", true) || currentSummary.contains("drizzle", true) -> "🌦️"
        currentSummary.contains("snow", true) -> "🌨️"
        currentSummary.contains("thunder", true) -> "⛈️"
        currentSummary.contains("cloud", true) || currentSummary.contains("overcast", true) -> "🌤️"
        currentSummary.contains("fog", true) -> "🌫️"
        currentSummary.contains("clear", true) -> "☀️"
        else -> "🌤️"
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF07518A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
          if (seasonalBitmap != null) {
            Image(
              bitmap = seasonalBitmap!!.asImageBitmap(),
              contentDescription = "Seasonal New Brunswick lake scenery",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxWidth().height(340.dp).alpha(0.42f)
            )
          }
          Column(
            modifier = Modifier.fillMaxWidth()
              .background(Color(0xB807518A))
              .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text("GRAND LAKE  •  WEATHER", color = Color(0xFFBDEBFF), style = MaterialTheme.typography.labelSmall)
                    Text(currentTemp, color = Color.White, style = MaterialTheme.typography.headlineLarge)
                    Text(currentSummary + if (feelsLike.isNotBlank()) "  •  $feelsLike" else "", color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                Text(weatherEmoji, style = MaterialTheme.typography.displayMedium)
            }
            Text(
                listOf("💨 $windLine", "💧 $humidityLine", "📅 ${tomorrow.replace("\n", " ")}")
                    .filter { it.trim().length > 2 }.joinToString("   •   "),
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2
            )
            Text(
                "🌅 ${moon.replace("\n", "  •  ")}   •   ❄️ $frost",
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFE1F4FF),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2
            )
            CompactWeatherTicker("⚠ ALERTS", alerts, Color(0xFFFFD65C))
            CompactWeatherTicker("🌊 TIDES", tideTicker, Color(0xFF8BEAFF))
            CompactWeatherTicker("☀ SOLAR / RADIO", solar, Color(0xFFFFE08A))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(updated.removePrefix("Updated ").let { "Updated $it" }, modifier = Modifier.weight(1f), color = Color(0xFFBDEBFF), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                TextButton(
                    onClick = { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://weather.gc.ca/city/pages/nb-14_metric_e.html"))) } catch (_: Exception) {} },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) { Text("OFFICIAL ↗", color = Color.White, style = MaterialTheme.typography.labelSmall) }
            }
          }
        }
    }
}


@Composable
fun CompactWeatherTicker(label: String, value: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = accent, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
        Spacer(Modifier.size(7.dp))
        Text(
            value.replace("\n", "  •  "),
            modifier = Modifier.weight(1f).basicMarquee(iterations = Int.MAX_VALUE),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun WeatherMiniCard(
    title: String,
    body: String,
    accent: Color,
    modifier: Modifier = Modifier,
    ticker: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181B1F)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = accent, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
            if (ticker) {
                SolarBandTicker(body)
            } else {
                Text(body, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 3)
            }
        }
    }
}

@Composable
fun SolarBandTicker(text: String) {
    val lines = remember(text) { text.lines().map { it.trim() }.filter { it.isNotBlank() } }
    var currentLine by remember(text) { mutableStateOf(0) }

    LaunchedEffect(lines) {
        currentLine = 0
        while (lines.size > 1) {
            kotlinx.coroutines.delay(3200L)
            currentLine = (currentLine + 1) % lines.size
        }
    }

    Box(modifier = Modifier.fillMaxWidth().height(38.dp), contentAlignment = Alignment.CenterStart) {
        AnimatedContent(
            targetState = lines.getOrElse(currentLine) { "Waiting for band conditions…" },
            transitionSpec = {
                (slideInVertically(animationSpec = tween(300)) { height -> height / 2 } + fadeIn(animationSpec = tween(300))) togetherWith
                    (slideOutVertically(animationSpec = tween(300)) { height -> -height } + fadeOut(animationSpec = tween(300)))
            },
            label = "solarBandTicker"
        ) { line ->
            Text(
                text = line,
                modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun WeatherInfoCard(title:String,icon:String,body:String,accent:Color){
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit){kotlinx.coroutines.delay(100);visible=true}
    AnimatedVisibility(visible=visible,enter=fadeIn(tween(350))+slideInVertically(initialOffsetY={it/5},animationSpec=tween(350))){
        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF181B1F)),elevation=CardDefaults.cardElevation(defaultElevation=2.dp)){
            Column(Modifier.padding(horizontal=15.dp,vertical=13.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){Text(icon,style=MaterialTheme.typography.titleMedium);Spacer(Modifier.size(8.dp));Text(title,color=accent,style=MaterialTheme.typography.labelLarge)}
                Spacer(Modifier.height(7.dp))
                AnimatedContent(targetState=body,transitionSpec={fadeIn(tween(250)) togetherWith fadeOut(tween(180))},label="weatherCardContent"){text->Text(text,color=Color.White,style=MaterialTheme.typography.bodyMedium)}
            }
        }
    }
}


@Composable
fun StatusIndicator() {

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "status"
        )

    val alpha by
        infiniteTransition.animateFloat(

            initialValue = 0.35f,

            targetValue = 1f,

            animationSpec =
                infiniteRepeatable(

                    animation =
                        tween(
                            900,
                            easing = LinearEasing
                        ),

                    repeatMode =
                        RepeatMode.Reverse
                ),

            label = "statusAlpha"
        )

    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(10.dp)
                .alpha(alpha)
                .background(
                    Color(0xFF69F0AE),
                    RoundedCornerShape(50)
                )
        )

        Spacer(
            modifier = Modifier.size(6.dp)
        )

        Text(
            text = "LIVE",
            color = Color(0xFF69F0AE),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun DateTimeBanner() {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000L)
        }
    }
    val dateText = remember(currentTime) { SimpleDateFormat("EEE, MMM d", Locale.CANADA).format(Date(currentTime)) }
    val time12Text = remember(currentTime) { SimpleDateFormat("h:mm a", Locale.CANADA).format(Date(currentTime)) }
    val time24Text = remember(currentTime) { SimpleDateFormat("HH:mm", Locale.CANADA).format(Date(currentTime)) }
    val timeText = "$time12Text  |  $time24Text"
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181B1F)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("📍 $dateText", color = Color.LightGray, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            Text(timeText, color = Color(0xFF69F0AE), style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

@Composable
fun AlertTile(
    tile: Tile,
    context: android.content.Context,
    onChat: () -> Unit,
    onWeather: () -> Unit,
    weatherExpanded: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF181B1F)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(29.dp)
                        .background(
                            tile.accent.copy(alpha = 0.16f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tile.icon,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Text(
                    text = tile.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = tile.accent,
                    maxLines = 2
                )

                if (tile.url != null) {
                    TextButton(
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(tile.url))
                                )
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.size(30.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "ⓘ",
                            color = tile.accent,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopStart) {
                if (tile.title == "Aircraft Overhead" || tile.title == "Power Outages") {
                    AircraftTicker(text = tile.subtitle, compact = true)
                } else {
                    Text(
                        text = tile.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        maxLines = 3
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tile.weather) {
                    TextButton(
                        onClick = onWeather,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            if (weatherExpanded) "HIDE" else "WEATHER",
                            color = tile.accent,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else if (tile.chat) {
                    TextButton(
                        onClick = onChat,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            "OPEN",
                            color = tile.accent,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun AircraftTicker(text: String, compact: Boolean = false) {
    val lines = remember(text) {
        text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    var currentLine by remember(text) {
        mutableStateOf(0)
    }

    LaunchedEffect(lines) {
        currentLine = 0
        while (lines.size > 1) {
            kotlinx.coroutines.delay(2800)
            currentLine = (currentLine + 1) % lines.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 68.dp else 72.dp),
        contentAlignment = Alignment.TopStart
    ) {
        AnimatedContent(
            targetState = lines.getOrElse(currentLine) {
                "Waiting for aircraft data…"
            },
            transitionSpec = {
                (slideInVertically(
                    animationSpec = tween(350)
                ) { height -> height } + fadeIn(
                    animationSpec = tween(350)
                )) togetherWith
                    (slideOutVertically(
                        animationSpec = tween(350)
                    ) { height -> -height } + fadeOut(
                        animationSpec = tween(350)
                    ))
            },
            label = "aircraftTicker"
        ) { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray,
                maxLines = 3
            )
        }
    }
}


private fun fetchTideTicker(): String {
    val zone = ZoneId.of("America/Moncton")
    val now = Instant.now()
    val from = now.minusSeconds(12 * 60 * 60L)
    val to = now.plusSeconds(48 * 60 * 60L)
    fun encoded(value: String) = URLEncoder.encode(value, "UTF-8")

    val stationsUrl = URL("https://api-iwls.dfo-mpo.gc.ca/api/v1/stations?code=00065")
    val stationConn = stationsUrl.openConnection().apply {
        connectTimeout = 10000
        readTimeout = 10000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "GrandLakeAlert/1.0")
    }
    val stations = org.json.JSONArray(stationConn.getInputStream().bufferedReader().use { it.readText() })
    var stationId: String? = null
    for (i in 0 until stations.length()) {
        val station = stations.optJSONObject(i) ?: continue
        if (station.optString("code") == "00065") {
            stationId = station.optString("id").takeIf { it.isNotBlank() }
            if (stationId != null) break
        }
    }
    val id = stationId ?: throw IllegalStateException("Saint John tide station not found")
    val dataUrl = URL(
        "https://api-iwls.dfo-mpo.gc.ca/api/v1/stations/$id/data" +
            "?time-series-code=wlp-hilo" +
            "&from=${encoded(from.toString())}" +
            "&to=${encoded(to.toString())}"
    )
    val dataConn = dataUrl.openConnection().apply {
        connectTimeout = 10000
        readTimeout = 10000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "GrandLakeAlert/1.0")
    }
    val values = org.json.JSONArray(dataConn.getInputStream().bufferedReader().use { it.readText() })
    data class TideEvent(val instant: Instant, val height: Double)
    val events = mutableListOf<TideEvent>()
    for (i in 0 until values.length()) {
        val item = values.optJSONObject(i) ?: continue
        val dateText = item.optString("eventDate")
        val height = item.optDouble("value", Double.NaN)
        if (!height.isFinite() || dateText.isBlank()) continue
        try { events.add(TideEvent(Instant.parse(dateText), height)) } catch (_: Exception) {}
    }
    val sorted = events.distinctBy { it.instant }.sortedBy { it.instant }
    if (sorted.size < 3) throw IllegalStateException("Not enough tide events")

    val highs = mutableListOf<TideEvent>()
    val lows = mutableListOf<TideEvent>()
    for (i in 1 until sorted.lastIndex) {
        val prev = sorted[i - 1]
        val current = sorted[i]
        val next = sorted[i + 1]
        if (current.instant.isAfter(now) && current.height >= prev.height && current.height > next.height) highs.add(current)
        if (current.instant.isAfter(now) && current.height <= prev.height && current.height < next.height) lows.add(current)
    }
    val high = highs.firstOrNull()
    val low = lows.firstOrNull()
    if (high == null || low == null) throw IllegalStateException("Upcoming high/low tide not found")

    val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.CANADA)
    val dayFmt = DateTimeFormatter.ofPattern("EEE", Locale.CANADA)
    val today = now.atZone(zone).toLocalDate()
    fun label(event: TideEvent): String {
        val local = event.instant.atZone(zone)
        val day = when (local.toLocalDate()) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            else -> dayFmt.format(local)
        }
        return "$day ${timeFmt.format(local)} (${"%.1f".format(Locale.CANADA, event.height)} m)"
    }
    return "Saint John coastal reference • High ${label(high)} • Low ${label(low)} • CHS predictions"
}
