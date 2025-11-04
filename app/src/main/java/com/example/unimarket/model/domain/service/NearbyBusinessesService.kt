package com.example.unimarket.model.domain.service

import android.util.Log
import com.example.unimarket.model.data.platform.GeocodingProvider
import com.example.unimarket.model.data.platform.LocationProvider
import com.example.unimarket.model.domain.entity.Business
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

data class BusinessPin(val business: Business, val position: LatLng)
data class GeocodeDebug(val name: String, val address: String, val latLng: LatLng?)
data class NearbyResult(
    val myLocation: LatLng,
    val pins: List<BusinessPin>,
    val debug: List<GeocodeDebug>
)

class NearbyBusinessesService(
    private val businessService: BusinessService,
    private val geocoder: GeocodingProvider,
    private val locationProvider: LocationProvider
) {
    suspend fun loadNearby(radiusMeters: Double = 2500.0): Result<NearbyResult> =
        withContext(Dispatchers.IO) {
            val me = locationProvider.getCurrentLocation()
                ?: return@withContext Result.failure(IllegalStateException("Sin ubicación"))

            val businesses = businessService.getAllBusinesses().getOrElse {
                Log.e("BusinessMap", "Firestore error: ${it.message}")
                return@withContext Result.failure(it)
            }

            val pins = mutableListOf<BusinessPin>()
            val debug = mutableListOf<GeocodeDebug>()

            for (b in businesses) {
                val addr = b.address?.direccion?.trim().orEmpty()
                val latLng = if (addr.isNotBlank()) geocoder.geocodeOnce(addr) else null
                debug += GeocodeDebug(b.name ?: "Negocio", addr, latLng)

                if (latLng != null && distanceMeters(me, latLng) <= radiusMeters) {
                    pins += BusinessPin(b, latLng)
                }
            }
            Result.success(NearbyResult(me, pins, debug))
        }

    private fun distanceMeters(a: LatLng, b: LatLng): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val sa = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(a.latitude)) *
                cos(Math.toRadians(b.latitude)) *
                sin(dLng / 2).pow(2.0)
        return 2 * R * asin(min(1.0, sqrt(sa)))
    }
}