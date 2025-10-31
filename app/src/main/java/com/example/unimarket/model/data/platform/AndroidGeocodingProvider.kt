package com.example.unimarket.model.data.platform

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class AndroidGeocodingProvider(private val context: Context) : GeocodingProvider {
    override suspend fun geocodeOnce(address: String): LatLng? = withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            val results = geocoder.getFromLocationName(address, 1)
            val loc = results?.firstOrNull()
            loc?.let { LatLng(it.latitude, it.longitude) }
        }.getOrNull()
    }
}