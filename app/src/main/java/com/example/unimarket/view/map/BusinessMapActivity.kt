package com.example.unimarket.view.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
import com.example.unimarket.view.home.BusinessDetailActivity
import com.example.unimarket.view.home.HomeBuyerActivity
import com.example.unimarket.view.profile.BuyerAccountActivity
import com.example.unimarket.view.home.ExploreBuyerActivity
import com.example.unimarket.viewmodel.BusinessMapViewModel
import com.example.unimarket.viewmodel.MapMarkerUi
import com.example.unimarket.viewmodel.MapNav
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class BusinessMapActivity : AppCompatActivity(R.layout.business_map_page) {

    private lateinit var mapView: MapView
    private var map: GoogleMap? = null
    private var loadedOnce = false

    private var lastMarkers: List<MapMarkerUi> = emptyList()

    // Fused Location (para centrar incluso sin internet)
    private lateinit var fusedClient: FusedLocationProviderClient

    private val markerHue: Float by lazy {
        val colorInt = ContextCompat.getColor(this, R.color.yellowLight)
        val hsv = FloatArray(3)
        Color.colorToHSV(colorInt, hsv)
        hsv[0]
    }

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
            ),
            markerHue = markerHue
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
            // Si estamos offline, al menos centra en la ubicación del dispositivo
            if (!isOnline()) centerOnMyLocationIfAvailable()
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
        try { map?.isMyLocationEnabled = hasLocationPermission() } catch (_: SecurityException) {}
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

    // -------- Conectividad --------
    private fun isOnline(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
               caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun showTopToast(message: String) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.parseColor("#FFFFFF"))
        }

        val icon = ImageView(this).apply {
            setImageResource(R.drawable.personajesingup)
            val s = dp(20)
            layoutParams = LinearLayout.LayoutParams(s, s).apply { rightMargin = dp(8) }
        }

        val text = TextView(this).apply {
            this.text = message
            setTextColor(Color.BLACK)
            textSize = 14f
        }

        container.addView(icon)
        container.addView(text)

        android.widget.Toast(this).apply {
            duration = android.widget.Toast.LENGTH_SHORT
            view = container
            setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, dp(24))
            show()
        }
    }

    // -------- Centrar cámara en mi ubicación (offline también) --------
    private fun centerOnMyLocationIfAvailable(zoom: Float = 14f) {
        if (!hasLocationPermission()) return
        fusedClient.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null && map != null) {
                    val here = LatLng(loc.latitude, loc.longitude)
                    map!!.animateCamera(CameraUpdateFactory.newLatLngZoom(here, zoom))
                } else {
                    // Fallback razonable para evitar "África": Bogotá centro
                    val bogota = LatLng(4.7110, -74.0721)
                    map?.moveCamera(CameraUpdateFactory.newLatLngZoom(bogota, 11f))
                }
            }
            .addOnFailureListener {
                // Otro fallback a Bogotá
                val bogota = LatLng(4.7110, -74.0721)
                map?.moveCamera(CameraUpdateFactory.newLatLngZoom(bogota, 11f))
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init fused client
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        mapView = findViewById(R.id.mapView)
        val progress = findViewById<View>(R.id.progress)
        val fabMyLocation = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabMyLocation)

        fabMyLocation.visibility = View.GONE

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { gmap ->
            map = gmap.apply {
                uiSettings.isZoomControlsEnabled = true
                uiSettings.isMyLocationButtonEnabled = true

                setOnInfoWindowClickListener { marker ->
                    val businessId = marker.tag as? String ?: return@setOnInfoWindowClickListener
                    val markerUi = lastMarkers.firstOrNull { it.businessId == businessId }
                        ?: return@setOnInfoWindowClickListener

                    val intent = Intent(this@BusinessMapActivity, BusinessDetailActivity::class.java).apply {
                        putExtra(BusinessDetailActivity.EXTRA_BUSINESS_ID, markerUi.businessId)
                        putExtra(BusinessDetailActivity.EXTRA_BUSINESS_NAME, markerUi.businessName)
                        putExtra(BusinessDetailActivity.EXTRA_BUSINESS_RATING, markerUi.rating)
                        putExtra(BusinessDetailActivity.EXTRA_BUSINESS_AMOUNT_RATINGS, markerUi.amountRatings)
                        putExtra(BusinessDetailActivity.EXTRA_BUSINESS_LOGO_URL, markerUi.logoUrl)
                        putStringArrayListExtra(
                            BusinessDetailActivity.EXTRA_BUSINESS_PRODUCT_IDS,
                            ArrayList(markerUi.productIds)
                        )
                    }
                    startActivity(intent)
                }
            }
            enableMyLocation()

            // Si no hay internet, centra en la ubicación del dispositivo inmediatamente
            if (!isOnline()) centerOnMyLocationIfAvailable()

            loadIfNeeded()
        }

        if (!hasLocationPermission()) requestLocationPermission()

        // Footer
        highlightFooterSelection()
        findViewById<ImageButton>(R.id.nav_home).setOnClickListener {
            startActivity(Intent(this, HomeBuyerActivity::class.java)); finish()
        }
        findViewById<ImageButton>(R.id.nav_search).setOnClickListener {
            startActivity(Intent(this, ExploreBuyerActivity::class.java))
        }
        findViewById<ImageButton>(R.id.nav_map).setOnClickListener { /* ya estás aquí */ }
        findViewById<ImageButton>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, BuyerAccountActivity::class.java)); finish()
        }

        // Observer del VM
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ui.collect { ui ->
                    progress.visibility = if (ui.isLoading) View.VISIBLE else View.GONE

                    ui.error?.let {
                        showTopToast(it)
                        viewModel.clearError()
                    }

                    if (ui.myLocation != null || ui.markers.isNotEmpty()) {
                        map?.let { gmap ->
                            gmap.clear()

                            lastMarkers = ui.markers

                            ui.markers.forEach { markerUi ->
                                val marker = gmap.addMarker(markerUi.options)
                                marker?.tag = markerUi.businessId
                            }

                            val builder = LatLngBounds.builder()
                            var any = false
                            ui.myLocation?.let { builder.include(it); any = true }
                            ui.markers.forEach { builder.include(it.options.position); any = true }
                            if (any) {
                                gmap.animateCamera(
                                    CameraUpdateFactory.newLatLngBounds(builder.build(), 80)
                                )
                            }
                        }
                    }

                    when (ui.nav) {
                        MapNav.None -> Unit
                        MapNav.Close -> viewModel.clearNav() // no cerrar activity en offline
                    }
                }
            }
        }

        viewModel.onInit()
    }

    private fun loadIfNeeded(force: Boolean = false) {
        if (!hasLocationPermission()) return

        if (!isOnline()) {
            showTopToast("Sin conexión. Mostrando tu ubicación si está disponible.")
            // En offline: centra en mi ubicación para evitar “África”
            centerOnMyLocationIfAvailable()
            return
        }

        if (!loadedOnce || force) {
            loadedOnce = true
            viewModel.loadNearby()
        }
    }

    private fun highlightFooterSelection() {
        fun setAlpha(id: Int, alpha: Float) =
            findViewById<ImageButton>(id).apply { imageAlpha = (alpha * 255).toInt() }

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
        // Si volvemos sin datos, al menos centra en mi posición
        if (!isOnline()) centerOnMyLocationIfAvailable()
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

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}