package com.example.unimarket.view.home
/*
import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.unimarket.controller.home.CameraCommand
import com.example.unimarket.controller.home.CourierHomeController
import com.example.unimarket.controller.home.CourierHomeViewPort
import com.example.unimarket.model.location.LocationRepository
import com.example.unimarket.model.repository.CourierHomeRepository
import com.example.unimarket.model.repository.DefaultCourierHomeRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

private val Accent = Color(0xFFF7B500)

@Composable
fun CourierHomeScreen() {
    val context = LocalContext.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var isLoading by remember { mutableStateOf(false) }
    var courier by remember { mutableStateOf<LatLng?>(null) }
    var dest by remember { mutableStateOf<LatLng?>(null) }
    var address by remember { mutableStateOf<String?>(null) }
    var snack by remember { mutableStateOf<String?>(null) }

    val cameraState = rememberCameraPositionState()

    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locationGranted = (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
    }

    val repo: CourierHomeRepository = remember {
        DefaultCourierHomeRepository(context = context, locationRepo = object : LocationRepository {
            override fun start(client: com.google.android.gms.location.FusedLocationProviderClient, request: com.google.android.gms.location.LocationRequest, callback: com.google.android.gms.location.LocationCallback) {
                try { client.requestLocationUpdates(request, callback, Looper.getMainLooper()) } catch (_: SecurityException) {}
            }
            override fun stop(client: com.google.android.gms.location.FusedLocationProviderClient, callback: com.google.android.gms.location.LocationCallback) {
                client.removeLocationUpdates(callback)
            }
        })
    }

    val controller = remember {
        CourierHomeController(
            context = context,
            view = object : CourierHomeViewPort {
                override fun setLoading(show: Boolean) { isLoading = show }
                override fun setCourierPosition(pos: LatLng?) { courier = pos }
                override fun setDestinationPosition(pos: LatLng?) { dest = pos }
                override fun setDeliveryAddress(addressStr: String) { address = addressStr }
                override fun applyCamera(cmd: CameraCommand) {
                    when (cmd) {
                        is CameraCommand.ZoomTo -> cameraState.move(CameraUpdateFactory.newLatLngZoom(cmd.target, cmd.zoom))
                        is CameraCommand.FitBounds -> cameraState.move(CameraUpdateFactory.newLatLngBounds(cmd.bounds, cmd.padding))
                    }
                }
                override fun showMessage(msg: String) { snack = msg }
            },
            repo = repo,
            fusedClient = fusedClient
        )
    }

    LaunchedEffect(locationGranted) {
        if (locationGranted) controller.onStart()
    }
    DisposableEffect(Unit) { onDispose { controller.onStop() } }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(isMyLocationEnabled = locationGranted),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false)
        ) {
            dest?.let { Marker(state = rememberMarkerState(position = it), title = "Destino") }
            courier?.let { Marker(state = rememberMarkerState(position = it), title = "Repartidor") }
        }

        VerifyOrderPill(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
        )

        if (!locationGranted) {
            PermissionBanner(
                text = "Activa el permiso de ubicación para ver tu posición.",
                onGrant = {
                    permissionLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 76.dp)
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
            )
        }

        DeliveryInfoCard(
            address = address ?: "Cargando…",
            eta = "10 mins",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(56.dp)
            )
        }
    }

    snack?.let {
        LaunchedEffect(it) { kotlinx.coroutines.delay(2500); snack = null }
    }
}

@Composable
fun PermissionBanner(text: String, onGrant: () -> Unit, modifier: Modifier = Modifier) {
    Surface(tonalElevation = 4.dp, shadowElevation = 2.dp, modifier = modifier) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Button(onClick = onGrant) { Text("Conceder") }
        }
    }
}

@Composable
private fun VerifyOrderPill(modifier: Modifier = Modifier) {
    Surface(
        color = Accent,
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = modifier.height(56.dp).widthIn(min = 260.dp)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Verify the order", color = Color.White, fontWeight = FontWeight.Bold)
            Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun DeliveryInfoCard(address: String, eta: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        color = Color.White,
        modifier = modifier
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFEAEAEA)),
                ) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = "Client",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Client", fontWeight = FontWeight.Bold)
                    Text(
                        "Cliente",
                        color = Color(0xFF6B7280),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = { }) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Chat"
                    )
                }
                IconButton(onClick = { }) {
                    Icon(
                        Icons.Outlined.Phone,
                        contentDescription = "Call"
                    )
                }
            }

            Divider()

            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF6B7280)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Deliver to",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            address,
                            color = Color(0xFF6B7280),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) { Text("MORE DETAILS", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }

}*/