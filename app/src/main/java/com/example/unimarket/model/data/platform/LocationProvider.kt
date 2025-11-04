package com.example.unimarket.model.data.platform

import com.google.android.gms.maps.model.LatLng

interface LocationProvider {
    suspend fun getCurrentLocation(): LatLng?
}