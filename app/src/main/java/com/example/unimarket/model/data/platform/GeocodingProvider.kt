package com.example.unimarket.model.data.platform

import com.google.android.gms.maps.model.LatLng

interface GeocodingProvider {
    suspend fun geocodeOnce(address: String): LatLng?
}