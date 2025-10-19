package com.example.unimarket.model.geocode

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

interface GeocodingRepository {
    suspend fun geocodeOnce(address: String): LatLng?
}

class AndroidGeocodingRepository(
    private val context: Context,
    private val locale: Locale = Locale.getDefault(),
    private val io: CoroutineDispatcher = Dispatchers.IO
) : GeocodingRepository {

    private val geocoder by lazy { Geocoder(context, locale) }

    override suspend fun geocodeOnce(address: String): LatLng? = withContext(io) {
        if (!Geocoder.isPresent()) return@withContext null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocationName(address, 1) { results: MutableList<Address>? ->
                        val latLng = results?.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
                        if (!cont.isCompleted) cont.resume(latLng)
                    }
                }
            } else {
                val results = geocoder.getFromLocationName(address, 1)
                results?.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
            }
        } catch (_: IOException) { null } catch (_: Exception) { null }
    }
}
