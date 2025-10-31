package com.example.unimarket.view.map


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.unimarket.R
import com.example.unimarket.model.data.platform.AndroidGeocodingProvider
import com.example.unimarket.model.data.platform.FusedLocationProvider
import com.example.unimarket.model.domain.service.BusinessService
import com.example.unimarket.model.domain.service.NearbyBusinessesService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.snackbar.Snackbar

class BusinessMapFragment : Fragment(R.layout.fragment_business_map), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var map: GoogleMap? = null

    private val viewModel: BusinessMapViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appCtx = requireContext().applicationContext
                val service = NearbyBusinessesService(
                    businessService = BusinessService(),
                    geocoder = AndroidGeocodingProvider(appCtx),
                    locationProvider = FusedLocationProvider(appCtx)
                )
                return BusinessMapViewModel(service) as T
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> checkAndLoad() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val progress = view.findViewById<View>(R.id.progress)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BusinessMapViewModel.UiState.Loading -> progress.visibility = View.VISIBLE
                is BusinessMapViewModel.UiState.Error -> {
                    progress.visibility = View.GONE
                    Snackbar.make(requireView(), state.message, Snackbar.LENGTH_LONG).show()
                }
                is BusinessMapViewModel.UiState.Ready -> {
                    progress.visibility = View.GONE
                    map?.let { gmap ->
                        gmap.clear()
                        // Habilitar punto azul si hay permiso
                        if (hasLocationPermission()) {
                            try { gmap.isMyLocationEnabled = true } catch (_: SecurityException) {}
                        }
                        // Marcadores de negocios
                        state.markers.forEach { gmap.addMarker(it) }

                        // Encajar mi ubicación + todos los marcadores
                        val bounds = LatLngBounds.builder()
                            .include(state.data.myLocation)
                        state.data.pins.forEach { bounds.include(it.position) }
                        val b = bounds.build()
                        val padding = (resources.displayMetrics.widthPixels * 0.10).toInt()
                        gmap.animateCamera(CameraUpdateFactory.newLatLngBounds(b, padding))
                    }
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isMyLocationButtonEnabled = true
        }
        checkAndLoad()
    }

    private fun checkAndLoad() {
        if (hasLocationPermission()) {
            viewModel.load()
            try { map?.isMyLocationEnabled = true } catch (_: SecurityException) {}
        } else {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun hasLocationPermission(): Boolean {
        val ctx = requireContext()
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    // Propagar ciclo de vida al MapView
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onDestroyView() { mapView.onDestroy(); super.onDestroyView() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState)
    }
}