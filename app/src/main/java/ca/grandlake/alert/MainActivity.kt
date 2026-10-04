package ca.grandlake.alert

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
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
            title = "Emergency Chat",
            icon = "💬",
            subtitle = "Local chat • works without internet",
            url = null,
            accent = Color(0xFF69F0AE)
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
            LocalEmergencyChat(
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
                                    onChatClick = { showChat = true }
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
fun AlertTile(
    tile: Tile,
    context: android.content.Context,
    onChatClick: () -> Unit
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

            Text(

                text = tile.subtitle,

                style =
                    MaterialTheme.typography.bodySmall,

                color = Color.LightGray,

                maxLines = 5
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )

            if (tile.url != null || tile.title == "Emergency Chat") {

                TextButton(

                    onClick = {

                        if (tile.title == "Emergency Chat") {
                            onChatClick()
                            return@TextButton
                        }

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

                        text = if (tile.title == "Emergency Chat") "OPEN CHAT" else "OPEN SOURCE",

                        color = tile.accent,

                        maxLines = 1,

                        softWrap = false
                    )
                }
            }
        }
    }
}


private data class ChatMessage(
    val sender: String,
    val text: String,
    val mine: Boolean
)

private const val CHAT_TOPIC = "grandlakealert-emergency-chat"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalEmergencyChat(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var messageText by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("Guest") }
    var statusText by remember { mutableStateOf("Starting local radio…") }
    var peerCount by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val connectedEndpoints = remember { mutableStateListOf<String>() }
    val nearbyClient = remember { Nearby.getConnectionsClient(context) }

    val prefs = remember {
        context.getSharedPreferences(
            "grand_lake_local_chat",
            android.content.Context.MODE_PRIVATE
        )
    }

    fun requiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= 32 -> arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
            Build.VERSION.SDK_INT >= 31 -> arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            Build.VERSION.SDK_INT >= 29 -> arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            else -> arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    fun hasPermissions(): Boolean {
        return requiredPermissions().all {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun addLocalMessage(sender: String, text: String, mine: Boolean) {
        if (text.isNotBlank()) {
            messages.add(ChatMessage(sender, text, mine))
        }
    }

    LaunchedEffect(Unit) {
        userName = prefs.getString("username", "Guest") ?: "Guest"

        if (messages.isEmpty()) {
            addLocalMessage(
                "Grand Lake Alert",
                "LOCAL OFFLINE CHAT TEST. Internet is not required.",
                false
            )
        }

        if (!hasPermissions()) {
            activity?.requestPermissions(
                requiredPermissions(),
                7001
            )
            statusText = "Waiting for Nearby permissions…"
        }
    }

    val payloadCallback = remember {
        object : PayloadCallback() {
            override fun onPayloadReceived(
                endpointId: String,
                payload: Payload
            ) {
                val bytes = payload.asBytes() ?: return
                val received = String(bytes, Charsets.UTF_8)
                val parts = received.split("|", limit = 2)

                if (parts.size == 2) {
                    val sender = parts[0].ifBlank { "Nearby phone" }
                    val text = parts[1]

                    if (text.isNotBlank()) {
                        scope.launch {
                            addLocalMessage(sender, text, false)
                        }
                    }
                }
            }

            override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate
            ) {
            }
        }
    }

    val connectionLifecycleCallback = remember {
        object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(
                endpointId: String,
                connectionInfo: ConnectionInfo
            ) {
                nearbyClient.acceptConnection(endpointId, payloadCallback)
            }

            override fun onConnectionResult(
                endpointId: String,
                result: ConnectionResolution
            ) {
                if (result.status.isSuccess) {
                    if (!connectedEndpoints.contains(endpointId)) {
                        connectedEndpoints.add(endpointId)
                    }
                    peerCount = connectedEndpoints.size
                    statusText = if (peerCount == 1) {
                        "LOCAL • 1 phone connected"
                    } else {
                        "LOCAL • $peerCount phones connected"
                    }
                } else {
                    statusText = "Nearby connection failed"
                }
            }

            override fun onDisconnected(endpointId: String) {
                connectedEndpoints.remove(endpointId)
                peerCount = connectedEndpoints.size
                statusText = if (peerCount == 0) {
                    "LOCAL • waiting for nearby phones"
                } else {
                    "LOCAL • $peerCount phones connected"
                }
            }
        }
    }

    val endpointDiscoveryCallback = remember {
        object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(
                endpointId: String,
                info: com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
            ) {
                statusText = "LOCAL • phone found — connecting…"

                nearbyClient.requestConnection(
                    userName.ifBlank { "Grand Lake phone" },
                    endpointId,
                    connectionLifecycleCallback
                ).addOnFailureListener { error ->
                    statusText = "LOCAL • connection request failed: " + error.message
                }
            }

            override fun onEndpointLost(endpointId: String) {
            }
        }
    }

    LaunchedEffect(hasPermissions()) {
        if (!hasPermissions()) return@LaunchedEffect

        try {
            val strategy = Strategy.P2P_CLUSTER

            nearbyClient.startAdvertising(
                userName.ifBlank { "Grand Lake phone" },
                "ca.grandlake.alert.localchat",
                connectionLifecycleCallback,
                AdvertisingOptions.Builder()
                    .setStrategy(strategy)
                    .build()
            ).addOnSuccessListener {
                statusText = "LOCAL • advertising + searching…"
            }.addOnFailureListener { error ->
                statusText = "LOCAL • advertising failed: " + error.message
            }

            nearbyClient.startDiscovery(
                "ca.grandlake.alert.localchat",
                endpointDiscoveryCallback,
                DiscoveryOptions.Builder()
                    .setStrategy(strategy)
                    .build()
            ).addOnSuccessListener {
                statusText = "LOCAL • searching for nearby phones…"
            }.addOnFailureListener { error ->
                statusText = "LOCAL • discovery failed: " + error.message
            }
        } catch (error: Exception) {
            statusText = "LOCAL • radio error: " + error.message
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }

        val saved = messages.joinToString("\\n") {
            it.sender + "|" +
                if (it.mine) "me" else "nearby" +
                "|" +
                it.text.replace("\\n", " ")
        }

        prefs.edit()
            .putString("messages", saved)
            .apply()
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            try {
                nearbyClient.stopAdvertising()
                nearbyClient.stopDiscovery()
                nearbyClient.stopAllEndpoints()
            } catch (_: Exception) {
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF101214),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Emergency Chat")
                        Text(
                            statusText,
                            color = Color(0xFF69F0AE),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("BACK", color = Color(0xFF69F0AE))
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
                .padding(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF181B1F)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "📡 LOCAL OFFLINE TEST",
                        color = Color(0xFF69F0AE),
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text(
                        "Phone-to-phone chat using Nearby Connections. Internet is not required.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        "Connected nearby phones: $peerCount",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    OutlinedTextField(
                        value = userName,
                        onValueChange = {
                            userName = it.take(20)
                            prefs.edit()
                                .putString("username", userName)
                                .apply()
                        },
                        label = { Text("Your name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                lazyItems(messages) { message ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            if (message.mine) Arrangement.End
                            else Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    if (message.mine) Color(0xFF245C43)
                                    else Color(0xFF24282D)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = 14.dp,
                                    vertical = 10.dp
                                )
                            ) {
                                Text(
                                    message.sender,
                                    color = Color(0xFF69F0AE),
                                    style = MaterialTheme.typography.labelSmall
                                )

                                Text(
                                    message.text,
                                    color = Color.White,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a local message…") },
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )

                Spacer(modifier = Modifier.size(8.dp))

                Button(
                    onClick = {
                        val clean = messageText.trim()
                        val sender = userName.trim().ifBlank { "Guest" }

                        if (clean.isNotEmpty()) {
                            addLocalMessage(sender, clean, true)
                            messageText = ""

                            val payload = Payload.fromBytes(
                                (sender + "|" + clean)
                                    .toByteArray(Charsets.UTF_8)
                            )

                            connectedEndpoints.toList().forEach { endpointId ->
                                nearbyClient.sendPayload(
                                    endpointId,
                                    payload
                                )
                            }
                        }
                    },
                    enabled = messageText.trim().isNotEmpty() &&
                        connectedEndpoints.isNotEmpty()
                ) {
                    Text("SEND")
                }
            }
        }
    }
}
