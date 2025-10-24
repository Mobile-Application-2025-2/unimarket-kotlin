package com.example.unimarket.controller.home
/*
import android.content.Context
import android.util.Log
import com.example.unimarket.model.repository.CourierHomeRepository
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.*

sealed class CameraCommand {
    data class ZoomTo(val target: LatLng, val zoom: Float) : CameraCommand()
    data class FitBounds(val bounds: LatLngBounds, val padding: Int) : CameraCommand()
}

interface CourierHomeViewPort {
    fun setLoading(show: Boolean)
    fun setCourierPosition(pos: LatLng?)
    fun setDestinationPosition(pos: LatLng?)
    fun setDeliveryAddress(address: String)
    fun applyCamera(cmd: CameraCommand)
    fun showMessage(msg: String)
}

class CourierHomeController(
    private val context: Context,
    private val view: CourierHomeViewPort,
    private val repo: CourierHomeRepository,
    private val fusedClient: FusedLocationProviderClient,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {

    private var callback: LocationCallback? = null

    fun onStart() {
        view.setLoading(true)
        startLocation()
        scope.launch {
            val addressResult = repo.fetchRandomDeliveryAddress()
            addressResult.onSuccess { addr ->
                Log.d("Courier", "address OK: $addr")
                view.setDeliveryAddress(addr)
                val dest = withContext(Dispatchers.IO) { repo.geocode(addr) }
                view.setDestinationPosition(dest)
                centerCamera()
            }.onFailure { e ->
                Log.e("Courier", "address FAIL: ${e.message}", e)
                view.showMessage(e.message ?: "Error cargando dirección")
            }
            view.setLoading(false)
        }
    }

    private fun startLocation() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1500L)
            .setWaitForAccurateLocation(true)
            .build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val last = result.lastLocation ?: return
                view.setCourierPosition(LatLng(last.latitude, last.longitude))
                centerCamera()
            }
        }

        callback?.let { repo.startLocation(fusedClient, request, it) }
    }

    private var lastCourier: LatLng? = null
    private var lastDest: LatLng? = null

    fun updateCourier(pos: LatLng?) {
        lastCourier = pos
        centerCamera()
    }

    fun updateDestination(pos: LatLng?) {
        lastDest = pos
        centerCamera()
    }

    private fun centerCamera() {
        val courier = lastCourier
        val dest = lastDest
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
        callback?.let { repo.stopLocation(fusedClient, it) }
        callback = null
        scope.coroutineContext.cancelChildren()
    }

    fun onViewCourierChanged(pos: LatLng?) { lastCourier = pos }
    fun onViewDestChanged(pos: LatLng?) { lastDest = pos }
}*/