package com.example.unimarket.controller.home

import com.example.unimarket.model.geocode.GeocodingRepository
import com.example.unimarket.model.location.LocationRepository
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.*

interface CourierHomeViewPort {
    fun setLoading(show: Boolean)
    fun setCourierPosition(pos: LatLng?)
    fun setDestinationPosition(pos: LatLng?)
    fun applyCamera(cmd: CameraCommand)
    fun showMessage(msg: String)
}

sealed class CameraCommand {
    data class FitBounds(val bounds: LatLngBounds, val padding: Int = 120) : CameraCommand()
    data class ZoomTo(val target: LatLng, val zoom: Float = 16f) : CameraCommand()
}

class CourierHomeController(
    private val view: CourierHomeViewPort,
    private val geocoder: GeocodingRepository,
    private val locationRepo: LocationRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private var client: FusedLocationProviderClient? = null
    private var callback: LocationCallback? = null

    fun onInit(deliveryAddress: String, fusedClient: FusedLocationProviderClient) {
        view.setLoading(true)
        client = fusedClient
        scope.launch {
            val dest = geocoder.geocodeOnce(deliveryAddress)
            view.setDestinationPosition(dest)
            view.setLoading(false)
            dest?.let { view.applyCamera(CameraCommand.ZoomTo(it, 16f)) }
        }
    }

    fun onPermissionsGranted() {
        val c = client ?: return
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1500L)
            .setWaitForAccurateLocation(true)
            .build()
        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val me = LatLng(loc.latitude, loc.longitude)
                view.setCourierPosition(me)
            }
        }
        callback = cb
        locationRepo.start(c, req, cb)
    }

    fun onPositionsChanged(courier: LatLng?, dest: LatLng?) {
        when {
            courier != null && dest != null -> {
                val bounds = LatLngBounds.builder().include(courier).include(dest).build()
                view.applyCamera(CameraCommand.FitBounds(bounds, 120))
            }
            courier != null -> view.applyCamera(CameraCommand.ZoomTo(courier, 16f))
            dest != null -> view.applyCamera(CameraCommand.ZoomTo(dest, 16f))
        }
    }

    fun onStop() {
        client?.let { c -> callback?.let { locationRepo.stop(c, it) } }
        callback = null
    }
}