package ca.grandlake.alert

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionOptions
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionType
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import java.nio.charset.StandardCharsets

private const val NEARBY_SERVICE_ID = "ca.grandlake.alert.offlinechat"

private class NearbyChatManager(context: Context) {

    private val client = Nearby.getConnectionsClient(context)
    private val strategy = Strategy.P2P_CLUSTER

    private val discovered = linkedMapOf<String, String>()
    private val requested = mutableSetOf<String>()
    private val connected = mutableSetOf<String>()

    var onStatus: (String) -> Unit = {}
    var onNearbyCount: (Int) -> Unit = {}
    var onMessage: (String) -> Unit = {}

    private val payloadCallback = object : PayloadCallback() {

        override fun onPayloadReceived(
            endpointId: String,
            payload: Payload
        ) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes() ?: return
                val message = String(bytes, StandardCharsets.UTF_8)

                if (message.isNotBlank()) {
                    val name =
                        discovered[endpointId] ?: "Nearby phone"

                    onMessage(name + ": " + message)
                }
            }
        }

        override fun onPayloadTransferUpdate(
            endpointId: String,
            update: PayloadTransferUpdate
        ) {
        }
    }

    private val connectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {

            override fun onConnectionInitiated(
                endpointId: String,
                connectionInfo: ConnectionInfo
            ) {
                discovered[endpointId] =
                    connectionInfo.endpointName.ifBlank {
                        "Nearby phone"
                    }

                onStatus(
                    "Connecting to " +
                        connectionInfo.endpointName +
                        "…"
                )

                client.acceptConnection(
                    endpointId,
                    payloadCallback
                )
            }

            override fun onConnectionResult(
                endpointId: String,
                resolution: ConnectionResolution
            ) {
                val statusCode =
                    resolution.status.statusCode

                if (
                    statusCode ==
                    ConnectionsStatusCodes.STATUS_OK
                ) {
                    connected.add(endpointId)
                    requested.remove(endpointId)

                    onStatus(
                        "🟢 Connected to " +
                            (discovered[endpointId]
                                ?: "nearby phone")
                    )
                } else {
                    requested.remove(endpointId)
                    onStatus(
                        "Nearby connection failed. Looking again…"
                    )
                }
            }

            override fun onDisconnected(endpointId: String) {
                connected.remove(endpointId)
                requested.remove(endpointId)

                onStatus(
                    if (connected.isEmpty()) {
                        "No nearby phones connected. Searching…"
                    } else {
                        "One nearby phone disconnected."
                    }
                )
            }
        }

    private val endpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {

            override fun onEndpointFound(
                endpointId: String,
                info: DiscoveredEndpointInfo
            ) {
                discovered[endpointId] =
                    info.endpointName.ifBlank {
                        "Nearby phone"
                    }

                onNearbyCount(discovered.size)

                if (
                    endpointId !in requested &&
                    endpointId !in connected
                ) {
                    requested.add(endpointId)

                    val options =
                        ConnectionOptions.Builder()
                            .setConnectionType(
                                ConnectionType.NON_DISRUPTIVE
                            )
                            .setLowPower(false)
                            .build()

                    client.requestConnection(
                        "Grand Lake Alert",
                        endpointId,
                        connectionLifecycleCallback,
                        options
                    ).addOnFailureListener {
                        requested.remove(endpointId)
                    }

                    onStatus(
                        "📡 Found " +
                            info.endpointName +
                            ". Connecting…"
                    )
                }
            }

            override fun onEndpointLost(
                endpointId: String
            ) {
                discovered.remove(endpointId)
                requested.remove(endpointId)

                onNearbyCount(discovered.size)

                if (connected.isEmpty()) {
                    onStatus(
                        "Searching for nearby phones…"
                    )
                }
            }
        }

    fun start() {

        val advertisingOptions =
            AdvertisingOptions.Builder()
                .setStrategy(strategy)
                .setConnectionType(
                    ConnectionType.NON_DISRUPTIVE
                )
                .setLowPower(false)
                .build()

        val discoveryOptions =
            DiscoveryOptions.Builder()
                .setStrategy(strategy)
                .setLowPower(false)
                .build()

        client.startAdvertising(
            "Grand Lake Alert",
            NEARBY_SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            onStatus(
                "📡 Visible to nearby Grand Lake Alert phones"
            )
        }.addOnFailureListener { error ->
            onStatus(
                "Nearby advertising unavailable: " +
                    (error.message ?: "check permissions")
            )
        }

        client.startDiscovery(
            NEARBY_SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnFailureListener { error ->
            onStatus(
                "Nearby discovery unavailable: " +
                    (error.message ?: "check permissions")
            )
        }
    }

    fun sendMessage(message: String) {

        val ids = connected.toList()

        if (ids.isEmpty()) {
            onStatus(
                "No nearby phone is connected yet."
            )
            return
        }

        val payload =
            Payload.fromBytes(
                message.toByteArray(
                    StandardCharsets.UTF_8
                )
            )

        client.sendPayload(ids, payload)
            .addOnFailureListener {
                onStatus("Message could not be sent.")
            }
    }

    fun stop() {
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()

        discovered.clear()
        requested.clear()
        connected.clear()
    }
}

private fun nearbyPermissions(): Array<String> {

    val permissions = mutableListOf<String>()

    // Nearby Connections may require location access for discovery,
    // including on newer Android versions.
    permissions +=
        Manifest.permission.ACCESS_COARSE_LOCATION

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions +=
            Manifest.permission.BLUETOOTH_ADVERTISE
        permissions +=
            Manifest.permission.BLUETOOTH_CONNECT
        permissions +=
            Manifest.permission.BLUETOOTH_SCAN
    } else if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    ) {
        permissions +=
            Manifest.permission.ACCESS_FINE_LOCATION
    }

    if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.TIRAMISU
    ) {
        permissions +=
            Manifest.permission.NEARBY_WIFI_DEVICES
    }

    return permissions.toTypedArray()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineChatScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var permissionReady by remember {
        mutableStateOf(false)
    }

    var status by remember {
        mutableStateOf(
            "Getting ready to find nearby phones…"
        )
    }

    var nearbyCount by remember {
        mutableStateOf(0)
    }

    var messageText by remember {
        mutableStateOf("")
    }

    val messages = remember {
        mutableStateListOf<String>()
    }

    val manager = remember(context) {
        NearbyChatManager(context)
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->

            permissionReady =
                nearbyPermissions().all { permission ->
                    result[permission] == true ||
                        context.checkSelfPermission(
                            permission
                        ) ==
                        android.content.pm.PackageManager
                            .PERMISSION_GRANTED
                }

            status =
                if (permissionReady) {
                    "📡 Searching automatically for nearby phones…"
                } else {
                    "Nearby permissions are needed for offline chat."
                }
        }

    fun requestNearbyPermissions() {

        val needed =
            nearbyPermissions().filter { permission ->
                context.checkSelfPermission(
                    permission
                ) !=
                    android.content.pm.PackageManager
                        .PERMISSION_GRANTED
            }

        if (needed.isEmpty()) {
            permissionReady = true
        } else {
            permissionLauncher.launch(
                needed.toTypedArray()
            )
        }
    }

    LaunchedEffect(Unit) {

        manager.onStatus = { newStatus ->
            status = newStatus
        }

        manager.onNearbyCount = { count ->
            nearbyCount = count
        }

        manager.onMessage = { message ->
            messages.add("Them: " + message)
        }

        requestNearbyPermissions()
    }

    LaunchedEffect(permissionReady) {
        if (permissionReady) {
            manager.start()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            manager.stop()
        }
    }

    Scaffold(
        containerColor = Color(0xFF101214),
        topBar = {

            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "📡 Offline Chat",
                            color = Color.White
                        )

                        Text(
                            text = "AUTOMATIC NEARBY CHAT",
                            color = Color(0xFF69F0AE),
                            style =
                                MaterialTheme.typography
                                    .labelSmall
                        )
                    }
                },
                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {
                        Text(
                            text = "‹",
                            color = Color.White,
                            style =
                                MaterialTheme.typography
                                    .headlineMedium
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            Color(0xFF181B1F)
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
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFF181B1F)
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(12.dp)
                ) {

                    Text(
                        text =
                            if (nearbyCount == 0) {
                                "🔎 No nearby phones yet"
                            } else {
                                "📱 " +
                                    nearbyCount +
                                    " nearby phone(s) found"
                            },
                        color = Color(0xFF69F0AE),
                        style =
                            MaterialTheme.typography
                                .titleMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text = status,
                        color = Color.LightGray,
                        style =
                            MaterialTheme.typography
                                .bodySmall
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "No pairing, IP address or Wi-Fi setup required.",
                        color = Color.Gray,
                        style =
                            MaterialTheme.typography
                                .bodySmall
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFF181B1F)
                    )
            ) {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {

                    items(messages) { message ->

                        Text(
                            text = message,
                            color = Color.White,
                            style =
                                MaterialTheme.typography
                                    .bodyMedium
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                OutlinedTextField(
                    value = messageText,
                    onValueChange = {
                        messageText = it
                    },
                    modifier =
                        Modifier.weight(1f),
                    placeholder = {
                        Text("Type a message…")
                    },
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(1.dp)
                )

                TextButton(
                    onClick = {

                        val text =
                            messageText.trim()

                        if (text.isNotEmpty()) {
                            manager.sendMessage(text)
                            messages.add(
                                "Me: " + text
                            )
                            messageText = ""
                        }
                    }
                ) {

                    Text(
                        text = "SEND",
                        color = Color(0xFF69F0AE)
                    )
                }
            }

            if (!permissionReady) {

                TextButton(
                    onClick = {
                        requestNearbyPermissions()
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        "ALLOW NEARBY DEVICES"
                    )
                }
            }
        }
    }
}
