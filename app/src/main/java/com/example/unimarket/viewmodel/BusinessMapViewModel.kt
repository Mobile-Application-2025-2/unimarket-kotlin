package com.example.unimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.unimarket.model.domain.service.NearbyBusinessesService
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MapMarkerUi(
    val options: MarkerOptions,
    val businessId: String,
    val businessName: String,
    val rating: Float,
    val amountRatings: Int,
    val logoUrl: String?,
    val productIds: List<String>
)

data class MapUi(
    val isLoading: Boolean = false,
    val myLocation: LatLng? = null,
    val markers: List<MapMarkerUi> = emptyList(),
    val nav: MapNav = MapNav.None,
    val error: String? = null
)

sealed class MapNav {
    data object None : MapNav()
    data object Close : MapNav()
}

class BusinessMapViewModel(
    private val nearbyService: NearbyBusinessesService,
    private val radiusMeters: Double = 2500.0,
    private val markerHue: Float
) : ViewModel() {

    private val _ui = MutableStateFlow(MapUi())
    val ui: StateFlow<MapUi> = _ui

    fun onInit() { /* reservado para futuras inicializaciones */ }

    fun loadNearby() {
        _ui.value = _ui.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            nearbyService.loadNearby(radiusMeters)
                .onSuccess { nearby ->
                    val markers = nearby.pins.map { pin ->
                        val b = pin.business

                        val markerOptions = MarkerOptions()
                            .position(pin.position)
                            .title(b.name ?: "Negocio")
                            .snippet(b.address?.direccion ?: "")
                            .icon(BitmapDescriptorFactory.defaultMarker(markerHue))

                        MapMarkerUi(
                            options = markerOptions,
                            businessId = b.id ?: "",
                            businessName = b.name ?: "",
                            rating = (b.rating ?: 0.0).toFloat(),
                            amountRatings = (b.amountRatings ?: 0L).toInt(),
                            logoUrl = b.logo,
                            productIds = b.products ?: emptyList()
                        )
                    }

                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        myLocation = nearby.myLocation,
                        markers = markers,
                        error = null,
                        nav = MapNav.None
                    )
                }
                .onFailure { ex ->
                    // No cierres la vista por errores; conserva el estado y muestra mensaje
                    val msg = when (ex) {
                        is java.net.UnknownHostException,
                        is java.io.IOException -> "Sin conexión. No se pudo actualizar el mapa."
                        else -> ex.message ?: "Error cargando negocios cercanos"
                    }
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        error = msg,
                        nav = MapNav.None
                    )
                }
        }
    }

    fun clearError() { _ui.value = _ui.value.copy(error = null) }
    fun clearNav()   { _ui.value = _ui.value.copy(nav = MapNav.None) }

    class Factory(
        private val nearbyService: NearbyBusinessesService,
        private val radiusMeters: Double = 2500.0,
        private val markerHue: Float
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BusinessMapViewModel(nearbyService, radiusMeters, markerHue) as T
        }
    }
}