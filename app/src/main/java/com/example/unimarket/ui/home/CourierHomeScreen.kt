package com.example.unimarket.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.os.Looper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unimarket.ui.scaffold.AppScaffold
import com.example.unimarket.ui.scaffold.BottomDest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.CameraUpdateFactory as GmsCameraUpdateFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

private val Accent = Color(0xFFF7B500)
private val CardBg = Color(0xFFFDFCFB)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CourierHomeScreen() {
    val permissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    LaunchedEffect(Unit) {
        if (!permissions.allPermissionsGranted) {
            permissions.launchMultiplePermissionRequest()
        }
    }
    val hasPermission = permissions.allPermissionsGranted
    val context = LocalContext.current

    var myPosition by remember { mutableStateOf<LatLng?>(null) }
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(myPosition) {
        val p = myPosition ?: return@LaunchedEffect
        cameraPositionState.animate(
            update = GmsCameraUpdateFactory.newLatLngZoom(p, 16f),
            durationMs = 700
        )
    }

    DisposableEffect(hasPermission) {
        if (!hasPermission) return@DisposableEffect onDispose {}
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(2000L)
            .setWaitForAccurateLocation(true)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val l = result.lastLocation ?: return
                myPosition = LatLng(l.latitude, l.longitude)
            }
        }
        startLocationUpdates(client, request, callback)
        onDispose { client.removeLocationUpdates(callback) }
    }

    AppScaffold(current = BottomDest.Home, onNavigate = { }) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            if (hasPermission) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = true),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = true,
                        zoomControlsEnabled = false,
                        compassEnabled = true
                    )
                ) {
                    myPosition?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Tu ubicación",
                            snippet = "En tiempo real"
                        )
                    }
                }
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Otorga permisos de ubicación para ver el mapa en tiempo real.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Button(
                onClick = { },
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
                name = "Cliff Rogers",
                role = "Client",
                eta = "10 mins",
                deliverTo = "W - 403",
                onChat = { },
                onCall = { },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@SuppressLint("MissingPermission")
private fun startLocationUpdates(
    client: FusedLocationProviderClient,
    request: LocationRequest,
    callback: LocationCallback
) {
    client.requestLocationUpdates(request, callback, Looper.getMainLooper())
}

@Composable
private fun ClientCard(
    name: String,
    role: String,
    eta: String,
    deliverTo: String,
    onChat: () -> Unit,
    onCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
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
                    Text(
                        name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(role, color = Color.Gray, fontSize = 13.sp)
                }
                YellowIconButton(icon = Icons.Outlined.Chat, onClick = onChat)
                Spacer(Modifier.width(8.dp))
                YellowIconButton(icon = Icons.Outlined.Call, onClick = onCall)
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color(0xFFEAEAEA))

            InfoRow(title = "Estimated time", value = eta)
            Spacer(Modifier.height(6.dp))
            InfoRow(title = "Deliver to", value = deliverTo)

            Spacer(Modifier.height(12.dp))
            MoreDetailsButton(onClick = { })
        }
    }
}

@Composable
private fun YellowIconButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    OutlinedIconButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, Accent),
        colors = IconButtonDefaults.outlinedIconButtonColors(
            contentColor = Accent
        )
    ) {
        Icon(icon, contentDescription = null)
    }
}

@Composable
private fun MoreDetailsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent.copy(alpha = 0.18f),
            contentColor = Accent
        ),
        border = BorderStroke(1.dp, Accent),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text("MORE DETAILS", letterSpacing = 1.2.sp)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_6")
@Composable
private fun PreviewCourierHomeScreen() {
    CourierHomeScreen()
}
