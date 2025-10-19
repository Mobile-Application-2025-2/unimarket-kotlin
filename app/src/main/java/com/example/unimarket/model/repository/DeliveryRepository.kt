package com.example.unimarket.model.repository

import android.content.Context
import android.util.Log
import com.example.unimarket.SupaConst
import com.example.unimarket.model.api.AuthApiFactory
import com.example.unimarket.model.api.DeliveriesApi
import com.example.unimarket.model.entity.Delivery
import com.example.unimarket.model.geocode.AndroidGeocodingRepository
import com.example.unimarket.model.geocode.GeocodingRepository
import com.example.unimarket.model.location.LocationRepository
import com.example.unimarket.model.session.SessionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.LatLng
import retrofit2.HttpException
import java.io.IOException
import kotlin.random.Random

interface CourierHomeRepository {
    suspend fun fetchRandomDeliveryAddress(): Result<String>
    suspend fun geocode(address: String): LatLng?
    fun startLocation(client: FusedLocationProviderClient, request: LocationRequest, callback: LocationCallback)
    fun stopLocation(client: FusedLocationProviderClient, callback: LocationCallback)
}

class DefaultCourierHomeRepository(
    context: Context,
    private val locationRepo: LocationRepository,
    private val geocodingRepo: GeocodingRepository = AndroidGeocodingRepository(context),
    deliveriesApiFactory: () -> DeliveriesApi = {
        AuthApiFactory.createDeliveriesApi(
            baseUrl = SupaConst.SUPABASE_URL,
            anonKey = SupaConst.SUPABASE_ANON_KEY,
            userJwt = SessionManager.get()?.accessToken,
            enableLogging = true
        )
    }
) : CourierHomeRepository {

    private val deliveriesApi: DeliveriesApi = deliveriesApiFactory()

    override suspend fun fetchRandomDeliveryAddress(): Result<String> = runCatching {
        val resp = deliveriesApi.list(order = "created_at.desc", limit = 50)
        Log.d("Deliveries", "HTTP ${resp.code()}  url=${resp.raw().request.url}")
        if (!resp.isSuccessful) throw HttpException(resp)
        val items: List<Delivery> = resp.body().orEmpty().filter { !it.addressDelivery.isNullOrBlank() }
        if (items.isEmpty()) error("No hay direcciones de entrega disponibles.")
        val pick = items[Random.nextInt(items.size)]
        pick.addressDelivery!!
    }.recoverCatching { e ->
        when (e) {
            is IOException -> error("Sin conexión. Verifica tu red.")
            else -> throw e
        }
    }

    override suspend fun geocode(address: String): LatLng? = geocodingRepo.geocodeOnce(address)

    override fun startLocation(
        client: FusedLocationProviderClient,
        request: LocationRequest,
        callback: LocationCallback
    ) = locationRepo.start(client, request, callback)

    override fun stopLocation(
        client: FusedLocationProviderClient,
        callback: LocationCallback
    ) = locationRepo.stop(client, callback)
}