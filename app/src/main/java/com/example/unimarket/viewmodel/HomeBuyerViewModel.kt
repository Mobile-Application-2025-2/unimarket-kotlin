package com.example.unimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.model.domain.entity.Business
import com.example.unimarket.model.domain.service.BusinessService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeBuyerUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val items: List<Business> = emptyList()
)

class HomeBuyerViewModel(
    private val businessService: BusinessService = BusinessService()
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeBuyerUiState(isLoading = true))
    val ui: StateFlow<HomeBuyerUiState> = _ui

    fun loadBusinesses(force: Boolean = false) {
        if (!force && _ui.value.items.isNotEmpty()) return

        _ui.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            businessService.getAllBusinesses()
                .onSuccess { list ->
                    _ui.update { it.copy(isLoading = false, items = list) }
                }
                .onFailure { e ->
                    _ui.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Error al cargar negocios"
                        )
                    }
                }
        }
    }
}