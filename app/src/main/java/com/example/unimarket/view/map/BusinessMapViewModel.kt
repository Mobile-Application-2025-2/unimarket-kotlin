package com.example.unimarket.view.map

import androidx.lifecycle.*
import com.example.unimarket.model.domain.service.NearbyBusinessesService
import com.example.unimarket.model.domain.service.NearbyResult
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class BusinessMapViewModel(
    private val nearbyService: NearbyBusinessesService,
    private val radiusMeters: Double = 2500.0
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Ready(
            val data: NearbyResult,
            val markers: List<MarkerOptions>
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _state = MutableLiveData<UiState>()
    val state: LiveData<UiState> = _state

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            nearbyService.loadNearby(radiusMeters)
                .onSuccess { nearby ->
                    val markers = nearby.pins.map {
                        MarkerOptions()
                            .position(it.position)
                            .title(it.business.name ?: "Negocio")
                            .snippet(it.business.address?.direccion ?: "")
                    }
                    _state.value = UiState.Ready(nearby, markers)
                }
                .onFailure {
                    _state.value = UiState.Error(it.message ?: "Error cargando mapa")
                }
        }
    }
}