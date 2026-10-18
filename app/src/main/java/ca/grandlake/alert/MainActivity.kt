package ca.grandlake.alert

import android.content.Intent
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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

data class Tile(
    val title: String,
    val icon: String,
    val subtitle: String,
    val url: String?,
    val accent: Color,
    val chat: Boolean = false
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
                        "(?is)Outage Data Last Updated at:?\\s*</?[^>]*>?\\s*([^<\\r\\n]{4,50})"
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

        if (showChat) {
            OfflineChatScreen(
                onBack = { showChat = false }
            )
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

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Grand Lake, NB",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )

                        Text(
                            text = "Live local information",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }

                    StatusIndicator()
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                LazyVerticalGrid(

                    columns = GridCells.Fixed(2),

                    modifier = Modifier.fillMaxSize(),

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
                                    onChat = {
                                        if (tile.chat) showChat = true
                                    }
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

    val dateText = remember(currentTime) {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.CANADA).format(Date(currentTime))
    }
    val time12 = remember(currentTime) {
        SimpleDateFormat("h:mm:ss a", Locale.CANADA).format(Date(currentTime))
    }
    val time24 = remember(currentTime) {
        SimpleDateFormat("HH:mm:ss", Locale.CANADA).format(Date(currentTime))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181B1F)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = "LOCAL DATE & TIME",
                color = Color(0xFF69F0AE),
                style = MaterialTheme.typography.labelSmall
            )

            Spacer(modifier = Modifier.height(3.dp))

            AnimatedContent(
                targetState = dateText,
                transitionSpec = {
                    (slideInVertically(animationSpec = tween(300)) { height -> height / 2 } +
                        fadeIn(animationSpec = tween(300))) togetherWith
                        (slideOutVertically(animationSpec = tween(300)) { height -> -height / 2 } +
                            fadeOut(animationSpec = tween(300)))
                },
                label = "dateTicker"
            ) { date ->
                Text(
                    text = date,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "12-HOUR",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.labelSmall
                    )
                    AnimatedContent(
                        targetState = time12,
                        transitionSpec = {
                            (slideInVertically(animationSpec = tween(220)) { height -> height / 2 } +
                                fadeIn(animationSpec = tween(220))) togetherWith
                                (slideOutVertically(animationSpec = tween(220)) { height -> -height / 2 } +
                                    fadeOut(animationSpec = tween(220)))
                        },
                        label = "twelveHourClock"
                    ) { time ->
                        Text(
                            text = time,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "24-HOUR",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.labelSmall
                    )
                    AnimatedContent(
                        targetState = time24,
                        transitionSpec = {
                            (slideInVertically(animationSpec = tween(220)) { height -> height / 2 } +
                                fadeIn(animationSpec = tween(220))) togetherWith
                                (slideOutVertically(animationSpec = tween(220)) { height -> -height / 2 } +
                                    fadeOut(animationSpec = tween(220)))
                        },
                        label = "twentyFourHourClock"
                    ) { time ->
                        Text(
                            text = time,
                            color = Color(0xFF69F0AE),
                            style = MaterialTheme.typography.titleMedium,
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
fun AlertTile(
    tile: Tile,
    context: android.content.Context,
    onChat: () -> Unit
) {

    Card(

        modifier = Modifier
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

            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp)
        ) {

            Box(

                modifier = Modifier
                    .size(48.dp)
                    .background(
                        tile.accent.copy(alpha = 0.16f),
                        RoundedCornerShape(14.dp)
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = tile.icon,
                    style =
                        MaterialTheme.typography.titleLarge
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(

                text = tile.title,

                style =
                    MaterialTheme.typography.titleMedium,

                color = tile.accent,

                maxLines = 2
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            if (tile.title == "Aircraft Overhead" || tile.title == "Power Outages") {
                AircraftTicker(text = tile.subtitle)
            } else {
                Text(

                    text = tile.subtitle,

                    style =
                        MaterialTheme.typography.bodySmall,

                    color = Color.LightGray,

                    maxLines = 5
                )
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )

            if (tile.chat) {
                TextButton(
                    onClick = onChat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "OPEN CHAT",
                        color = tile.accent,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            } else if (tile.url != null) {

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

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {

                    Text(

                        text = "OPEN SOURCE",

                        color = tile.accent,

                        maxLines = 1,

                        softWrap = false
                    )
                }
            }
        }
    }
}


@Composable
fun AircraftTicker(text: String) {
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
            .height(72.dp),
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
