package com.example.unimarket.view.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.unimarket.model.geocode.AndroidGeocodingRepository
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

@Composable
fun CourierHomeScreen(
    deliveryAddress: String = "Cra 1 #1-1, Bogotá",
) {
    val context = LocalContext.current

    // --- Estado UI ---
    var isLoading by remember { mutableStateOf(true) }
    var dest by remember { mutableStateOf<LatLng?>(null) }
    var courier by remember { mutableStateOf<LatLng?>(null) }
    var hasCenteredOnce by remember { mutableStateOf(false) }

    // Permisos
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

    // Mapa
    val cameraState = rememberCameraPositionState()

    // Geocodificación una sola vez
    LaunchedEffect(deliveryAddress) {
        val geocoder = AndroidGeocodingRepository(context.applicationContext)
        dest = geocoder.geocodeOnce(deliveryAddress)
        isLoading = false
    }

    // Ubicación en tiempo real (start/stop)
    DisposableEffect(locationGranted) {
        if (!locationGranted) {
            // No arrancamos updates. Igual devolvemos un onDispose "no-op".
            return@DisposableEffect onDispose { /* no-op */ }
        }

        val fused = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1500L)
            .setWaitForAccurateLocation(true)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val last = result.lastLocation ?: return
                courier = LatLng(last.latitude, last.longitude)
            }
        }

        try {
            fused.requestLocationUpdates(request, callback, context.mainLooper)
        } catch (_: SecurityException) { /* permiso no concedido */ }

        // <- Aquí sí retornas el DisposableEffectResult correcto
        onDispose {
            fused.removeLocationUpdates(callback)
        }
    }


    // Comandos de cámara reactivos
    LaunchedEffect(courier, dest) {
        when {
            courier != null && dest != null -> {
                val bounds = LatLngBounds.builder().include(courier!!).include(dest!!).build()
                cameraState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 120))
                hasCenteredOnce = true
            }
            !hasCenteredOnce && dest != null -> {
                cameraState.animate(CameraUpdateFactory.newLatLngZoom(dest!!, 16f))
                hasCenteredOnce = true
            }
            !hasCenteredOnce && courier != null -> {
                cameraState.animate(CameraUpdateFactory.newLatLngZoom(courier!!, 16f))
                hasCenteredOnce = true
            }
        }
    }

    // UI
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

        if (!locationGranted) {
            PermissionBanner(
                text = "Activa el permiso de ubicación para ver tu posición.",
                onGrant = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
            )
        }
    }
}

@Composable
private fun PermissionBanner(
    text: String,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
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
