package ca.grandlake.alert

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class IodaTickerItem(
    val headline: String,
    val severity: String,
    val time: Long
)

private data class IodaTickerSnapshot(
    val items: List<IodaTickerItem>,
    val updatedAt: Long
)

private fun fetchIodaTicker(): IodaTickerSnapshot {
    val now = System.currentTimeMillis() / 1000L
    val from = now - 24L * 60L * 60L
    val endpoint = URL(
        "https://api.ioda.inetintel.cc.gatech.edu/v2/outages/alerts" +
            "?from=$from&until=$now&limit=100"
    )
    val connection = (endpoint.openConnection() as HttpsURLConnection).apply {
        connectTimeout = 20000
        readTimeout = 25000
        useCaches = false
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "GrandLakeAlert/1.0 (public IODA outage monitor)")
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Accept-Encoding", "identity")

        // Older Android TV firmware can negotiate HTTPS differently from newer phones.
        // IODA's API supports TLS 1.2; explicitly use it for broad device compatibility.
        val tls12 = SSLContext.getInstance("TLSv1.2")
        tls12.init(null, null, null)
        sslSocketFactory = tls12.socketFactory
    }
    try {
        val status = connection.responseCode
        if (status !in 200..299) {
            throw IllegalStateException("IODA HTTP $status")
        }
        val raw = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val trimmed = raw.trim()
        val data = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val root = JSONObject(trimmed)
                root.optJSONArray("data")
                    ?: root.optJSONArray("alerts")
                    ?: JSONArray()
            }
            else -> JSONArray()
        }
    val newestByEntity = linkedMapOf<String, IodaTickerItem>()

    for (i in 0 until data.length()) {
        val alert = data.optJSONObject(i) ?: continue
        val level = alert.optString("level", "unknown").lowercase(Locale.ROOT)
        if (level == "normal" || level == "unknown" || level.isBlank()) continue

        val entity = alert.optJSONObject("entity")
        val name = entity?.optString("name").orEmpty()
            .ifBlank { entity?.optString("code").orEmpty() }
            .ifBlank { "Unidentified network" }
        val entityType = entity?.optString("type").orEmpty()
        val source = alert.optString("datasource", "network signal")
            .replace("-", " ")
        val time = alert.optLong("time", 0L)
        val severity = when (level) {
            "critical", "severe", "emergency" -> "MAJOR"
            "warning", "alert" -> "WARNING"
            else -> "WATCH"
        }
        val category = when (entityType.lowercase(Locale.ROOT)) {
            "country" -> "Country"
            "region" -> "Region"
            "asn", "geoasn" -> "Network"
            else -> "Internet"
        }
        val headline = "$category: $name • $source signal"
        val item = IodaTickerItem(headline, severity, time)
        val key = "$entityType:$name"
        val existing = newestByEntity[key]
        if (existing == null || item.time > existing.time) newestByEntity[key] = item
    }

    val items = newestByEntity.values
        .sortedByDescending { it.time }
        .take(12)

        return IodaTickerSnapshot(items, System.currentTimeMillis())
    } finally {
        connection.disconnect()
    }
}

@Composable
fun InternetOutageTicker() {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf<IodaTickerSnapshot?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val fresh = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    fetchIodaTicker()
                }
                snapshot = fresh
                errorMessage = null
                currentIndex = 0
            } catch (e: Exception) {
                if (snapshot == null) {
                    val detail = e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
                    errorMessage = detail.take(72)
                }
            }
            kotlinx.coroutines.delay(10 * 60 * 1000L)
        }
    }

    val items = snapshot?.items.orEmpty()
    LaunchedEffect(items) {
        currentIndex = 0
        while (items.size > 1) {
            kotlinx.coroutines.delay(8000L)
            currentIndex = (currentIndex + 1) % items.size
        }
    }

    val item = items.getOrNull(currentIndex)
    val updatedText = snapshot?.updatedAt?.let {
        SimpleDateFormat("HH:mm", Locale.CANADA).format(Date(it))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable {
                try {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://ioda.inetintel.cc.gatech.edu/dashboard/")
                        )
                    )
                } catch (_: Exception) {
                }
            },
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when {
                    errorMessage != null -> "IODA FEED ERROR"
                    snapshot == null -> "CONNECTING TO IODA…"
                    items.isEmpty() -> "NO SIGNIFICANT ALERTS"
                    else -> "${items.size} NETWORK ALERTS • LAST 24H"
                },
                color = if (errorMessage != null) Color(0xFFFFB74D) else Color(0xFF80CBC4),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
            Text(
                text = updatedText?.let { "UPDATED $it" } ?: "WAITING",
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        AnimatedContent(
            targetState = item?.headline ?: when {
                errorMessage != null -> "Feed error: $errorMessage"
                snapshot == null -> "Loading worldwide network reports…"
                else -> "No significant outage alerts in the last 24 hours"
            },
            transitionSpec = {
                (slideInVertically(animationSpec = tween(300)) { it / 2 } + fadeIn(tween(300))) togetherWith
                    (slideOutVertically(animationSpec = tween(300)) { -it / 2 } + fadeOut(tween(300)))
            },
            label = "internetOutageTicker"
        ) { headline ->
            Text(
                text = headline,
                modifier = Modifier.fillMaxWidth(),
                color = Color.LightGray,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = item?.let { "${it.severity} • IODA public network measurements • Tap for global map" }
                ?: "IODA • BGP routing and active connectivity measurements",
            color = item?.let {
                if (it.severity == "MAJOR") Color(0xFFFF6E6E)
                else if (it.severity == "WARNING") Color(0xFFFFB74D)
                else Color(0xFF80CBC4)
            } ?: Color.Gray,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}
