package com.example.unimarket.model.data.platform

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class AndroidGeocodingProvider(
    private val context: Context,
    private val cityHint: String? = "Bogotá, Colombia",
    private val bbox: FloatArray? = floatArrayOf(4.45f, -74.20f, 4.85f, -73.95f)
) : GeocodingProvider {

    override suspend fun geocodeOnce(address: String): LatLng? = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        val biased = if (!cityHint.isNullOrBlank() && !address.contains(",")) {
            "$address, $cityHint"
        } else address

        val results = if (bbox != null && bbox.size == 4) {
            geocoder.getFromLocationName(
                biased, 5,
                bbox[0].toDouble(), bbox[1].toDouble(),
                bbox[2].toDouble(), bbox[3].toDouble()
            )
        } else {
            geocoder.getFromLocationName(biased, 5)
        }

        results?.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
    }
}
