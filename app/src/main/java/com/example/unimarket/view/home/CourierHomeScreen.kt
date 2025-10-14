package com.example.unimarket.view.home

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory as GmsCameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

/* --- MVC imports (controller + repos en sus propias carpetas) --- */
import com.example.unimarket.controller.home.CameraCommand
import com.example.unimarket.controller.home.CourierHomeController
import com.example.unimarket.controller.home.CourierHomeViewPort
import com.example.unimarket.model.geocode.impl.AndroidGeocodingRepository
import com.example.unimarket.model.location.impl.AndroidLocationRepository

class CourierHomeActivity : ComponentActivity(), CourierHomeViewPort {

    private lateinit var controller: CourierHomeController

    // Estado UI
    private var isLoading by mutableStateOf(false)
    private var courierPos by mutableStateOf<LatLng?>(null)
    private var destPos by mutableStateOf<LatLng?>(null)
    private var pendingCameraCmd by mutableStateOf<CameraCommand?>(null)

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val geocoder = AndroidGeocodingRepository(this)
        val locationRepo = AndroidLocationRepository()
        controller = CourierHomeController(this, geocoder, locationRepo)

        val deliveryAddress = "Cra 1 E #19a-70, Bogotá, Colombia"
        val clientName = "Cliff Rogers"
        val clientPlaceLabel = "W - 403"
        val etaLabel = "10 mins"

        setContent {
            val permissions = rememberMultiplePermissionsState(
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )

            val cameraPositionState = rememberCameraPositionState()

            LaunchedEffect(Unit) {
                if (!permissions.allPermissionsGranted) permissions.launchMultiplePermissionRequest()
                val fused = LocationServices.getFusedLocationProviderClient(this@CourierHomeActivity)
                controller.onInit(deliveryAddress, fused)
            }

            LaunchedEffect(permissions.allPermissionsGranted) {
                if (permissions.allPermissionsGranted) controller.onPermissionsGranted()
            }

            LaunchedEffect(pendingCameraCmd) {
                when (val cmd = pendingCameraCmd) {
                    is CameraCommand.FitBounds -> {
                        cameraPositionState.animate(
                            update = GmsCameraUpdateFactory.newLatLngBounds(cmd.bounds, cmd.padding),
                            durationMs = 700
                        )
                    }
                    is CameraCommand.ZoomTo -> {
                        cameraPositionState.animate(
                            update = GmsCameraUpdateFactory.newLatLngZoom(cmd.target, cmd.zoom),
                            durationMs = 700
                        )
                    }
                    null -> Unit
                }
            }

            LaunchedEffect(courierPos, destPos) {
                controller.onPositionsChanged(courierPos, destPos)
            }

            CourierHomeScreen(
                hasPermission = permissions.allPermissionsGranted,
                cameraPositionState = cameraPositionState,
                courierPos = courierPos,
                destPos = destPos,
                isLoading = isLoading,
                clientName = clientName,
                clientPlaceLabel = clientPlaceLabel,
                etaLabel = etaLabel,
                onVerify = { /* TODO: acción de verificación */ },
                onChat = { /* TODO */ },
                onCall = { /* TODO */ }
            )
        }
    }

    override fun onStop() {
        super.onStop()
        controller.onStop()
    }
    override fun setLoading(show: Boolean) { isLoading = show }
    override fun setCourierPosition(pos: LatLng?) { courierPos = pos }
    override fun setDestinationPosition(pos: LatLng?) { destPos = pos }
    override fun applyCamera(cmd: CameraCommand) { pendingCameraCmd = cmd }
    override fun showMessage(msg: String) { /* usa Toast si quieres */ }
}

@Composable
fun CourierHomeScreen(
    hasPermission: Boolean,
    cameraPositionState: CameraPositionState,
    courierPos: LatLng?,
    destPos: LatLng?,
    isLoading: Boolean,
    clientName: String,
    clientPlaceLabel: String,
    etaLabel: String,
    onVerify: () -> Unit,
    onChat: () -> Unit,
    onCall: () -> Unit
) {
    val Accent = Color(0xFFF7B500)
    val CardBg = Color(0xFFFDFCFB)

    Scaffold(
        topBar = {},
        bottomBar = {},
        containerColor = Color.White
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasPermission),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = true,
                    zoomControlsEnabled = false,
                    compassEnabled = true
                )
            ) {
                courierPos?.let {
                    Marker(MarkerState(it), title = "Tú", snippet = "Ubicación del repartidor")
                }
                destPos?.let {
                    Marker(MarkerState(it), title = "Entrega", snippet = "Destino")
                }
                if (courierPos != null && destPos != null) {
                    Polyline(points = listOf(courierPos, destPos))
                }
            }

            Button(
                onClick = onVerify,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .fillMaxWidth(0.92f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text("Verify the order", fontWeight = FontWeight.SemiBold)
            }

            ClientCard(
                name = clientName,
                role = "Client",
                eta = etaLabel,
                deliverTo = clientPlaceLabel,
                onChat = onChat,
                onCall = onCall,
                accent = Accent,
                cardBg = CardBg,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )

            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun ClientCard(
    name: String,
    role: String,
    eta: String,
    deliverTo: String,
    onChat: () -> Unit,
    onCall: () -> Unit,
    accent: Color,
    cardBg: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDADADA))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(role, color = Color.Gray, fontSize = 13.sp)
                }
                YellowIconButton(icon = Icons.AutoMirrored.Outlined.Chat, onClick = onChat, accent = accent)
                Spacer(Modifier.width(8.dp))
                YellowIconButton(icon = Icons.Outlined.Call, onClick = onCall, accent = accent)
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color(0xFFEAEAEA))
            InfoRow(title = "Estimated time", value = eta)
            Spacer(Modifier.height(6.dp))
            InfoRow(title = "Deliver to", value = deliverTo)
            Spacer(Modifier.height(12.dp))
            MoreDetailsButton(onClick = { }, accent = accent)
        }
    }
}

@Composable
private fun YellowIconButton(icon: ImageVector, onClick: () -> Unit, accent: Color) {
    OutlinedIconButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, accent),
        colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = accent)
    ) { Icon(icon, contentDescription = null) }
}

@Composable
private fun MoreDetailsButton(onClick: () -> Unit, accent: Color, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent.copy(alpha = 0.18f),
            contentColor = accent
        ),
        border = BorderStroke(1.dp, accent),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) { Text("MORE DETAILS", letterSpacing = 1.2.sp) }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}