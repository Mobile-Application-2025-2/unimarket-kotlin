package com.example.unimarket.model.data.platform

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FusedLocationProvider(private val context: Context) : LocationProvider {
    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LatLng? = suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (!cont.isCompleted) cont.resume(loc?.let { LatLng(it.latitude, it.longitude) })
            }
            .addOnFailureListener { if (!cont.isCompleted) cont.resume(null) }
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            client.lastLocation
                .addOnSuccessListener { last ->
                    if (!cont.isCompleted) cont.resume(last?.let { LatLng(it.latitude, it.longitude) })
                }
                .addOnFailureListener { if (!cont.isCompleted) cont.resume(null) }
        }, 3000)
    }
}