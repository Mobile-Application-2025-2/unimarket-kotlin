package com.example.unimarket.view.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.unimarket.R
import com.example.unimarket.model.data.platform.AndroidGeocodingProvider
import com.example.unimarket.model.data.platform.FusedLocationProvider
import com.example.unimarket.model.domain.service.BusinessService
import com.example.unimarket.model.domain.service.NearbyBusinessesService
import com.example.unimarket.view.home.HomeBuyerActivity
import com.example.unimarket.view.profile.BuyerAccountActivity
import com.example.unimarket.viewmodel.BusinessMapViewModel
import com.example.unimarket.viewmodel.MapNav
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class BusinessMapActivity : AppCompatActivity(R.layout.business_map_page) {

    private lateinit var mapView: MapView
    private var map: GoogleMap? = null
    private var loadedOnce = false

    private val viewModel: BusinessMapViewModel by viewModels {
        BusinessMapViewModel.Factory(
            nearbyService = NearbyBusinessesService(
                businessService = BusinessService(),
                geocoder = AndroidGeocodingProvider(
                    applicationContext,
                    cityHint = "Bogotá, Colombia",
                    bbox = floatArrayOf(4.45f, -74.20f, 4.85f, -73.95f)
                ),
                locationProvider = FusedLocationProvider(applicationContext)
            )
        )
    }

    // ---------- Permissions ----------
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val fine = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            enableMyLocation()
            loadIfNeeded()
        } else {
            showPermissionSnackbar()
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun enableMyLocation() {
        try {
            map?.isMyLocationEnabled = hasLocationPermission()
        } catch (_: SecurityException) { }
    }

    private fun showPermissionSnackbar() {
        Snackbar.make(
            findViewById(R.id.root_map),
            "Activa la ubicación para ver negocios cercanos",
            Snackbar.LENGTH_LONG
        ).setAction("Configurar") {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
            startActivity(intent)
        }.show()
    }
    // ---------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ids del XML: mapView, progress, fabMyLocation
        mapView = findViewById(R.id.mapView)
        val progress = findViewById<View>(R.id.progress)
        val fabMyLocation = findViewById<FloatingActionButton>(R.id.fabMyLocation)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { gmap ->
            map = gmap.apply {
                uiSettings.isZoomControlsEnabled = true
                uiSettings.isMyLocationButtonEnabled = false
            }
            enableMyLocation()
            loadIfNeeded()
        }

        if (!hasLocationPermission()) requestLocationPermission()

        fabMyLocation.setOnClickListener {
            val loc = viewModel.ui.value.myLocation
            when {
                loc != null -> map?.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 16f))
                !hasLocationPermission() -> requestLocationPermission()
                else -> loadIfNeeded(force = true)
            }
        }

        // Footer: ids nav_home, nav_search, nav_map, nav_profile
        highlightFooterSelection()

        findViewById<ImageButton>(R.id.nav_home).setOnClickListener {
            startActivity(Intent(this, HomeBuyerActivity::class.java))
            finish()
        }

        // search aún no disponible → no hace nada (o podrías poner Snackbar/Toast si quieres)
        findViewById<ImageButton>(R.id.nav_search).setOnClickListener {
            Snackbar.make(
                findViewById(R.id.root_map),
                "Esta opción aún no está habilitada",
                Snackbar.LENGTH_SHORT
            ).show()
        }

        // estamos en map → no navegamos a ningún lado al tocarlo
        findViewById<ImageButton>(R.id.nav_map).setOnClickListener {
            // no-op
        }

        // perfil de buyer
        findViewById<ImageButton>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, BuyerAccountActivity::class.java))
            finish()
        }

        // Observa el estado del ViewModel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ui.collect { ui ->
                    progress.visibility = if (ui.isLoading) View.VISIBLE else View.GONE

                    ui.error?.let {
                        Snackbar.make(findViewById(R.id.root_map), it, Snackbar.LENGTH_LONG).show()
                        viewModel.clearError()
                    }

                    if (ui.myLocation != null || ui.markers.isNotEmpty()) {
                        map?.let { gmap ->
                            gmap.clear()
                            ui.markers.forEach { gmap.addMarker(it) }

                            val builder = LatLngBounds.builder()
                            var any = false
                            ui.myLocation?.let { builder.include(it); any = true }
                            ui.markers.forEach { builder.include(it.position); any = true }
                            if (any) {
                                gmap.animateCamera(
                                    CameraUpdateFactory.newLatLngBounds(builder.build(), 80)
                                )
                            }
                        }
                    }

                    when (ui.nav) {
                        MapNav.None -> Unit
                        MapNav.Close -> {
                            viewModel.clearNav()
                            finish()
                        }
                    }
                }
            }
        }

        viewModel.onInit()
    }

    private fun loadIfNeeded(force: Boolean = false) {
        if (!hasLocationPermission()) return
        if (!loadedOnce || force) {
            loadedOnce = true
            viewModel.loadNearby()
        }
    }

    private fun highlightFooterSelection() {
        fun setAlpha(id: Int, alpha: Float) =
            findViewById<ImageButton>(id).apply {
                imageAlpha = (alpha * 255).toInt()
            }

        // home y profile apagados, map encendido (ids del XML)
        setAlpha(R.id.nav_home, 0.55f)
        setAlpha(R.id.nav_search, 0.55f)
        setAlpha(R.id.nav_map, 1.0f)
        setAlpha(R.id.nav_profile, 0.55f)
    }

    // MapView lifecycle
    override fun onResume() {
        super.onResume()
        mapView.onResume()
        enableMyLocation()
        loadIfNeeded()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
