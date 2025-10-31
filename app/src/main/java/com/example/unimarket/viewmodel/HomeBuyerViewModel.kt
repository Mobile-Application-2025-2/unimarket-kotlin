package com.example.unimarket.view.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.model.domain.entity.Business
import com.example.unimarket.model.domain.entity.Category
import com.example.unimarket.model.domain.service.BusinessService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Filtros disponibles
enum class Filter { ALL, FOOD, STATIONERY, TUTORING, ACCESSORIES }

// Estado de UI
data class HomeUiState(
    val businessesAll: List<Business> = emptyList(),
    val businessesFiltered: List<Business> = emptyList(),
    val selected: Filter = Filter.ALL,
    val loading: Boolean = false,
    val error: String? = null
)

// Contrato de navegación (MVVM)
sealed class HomeNav {
    data object None : HomeNav()
    data object ToBuyerProfile : HomeNav()
}

class HomeBuyerViewModel(
    private val service: BusinessService = BusinessService()
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui

    private val _nav = MutableStateFlow<HomeNav>(HomeNav.None)
    val nav: StateFlow<HomeNav> = _nav

    init {
        loadBusinesses()
    }

    private fun loadBusinesses() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            service.getAllBusinesses()
                .onSuccess { list ->
                    _ui.update { st ->
                        val filtered = filterBy(st.selected, list)
                        st.copy(
                            businessesAll = list,
                            businessesFiltered = filtered,
                            loading = false
                        )
                    }
                }
                .onFailure { e ->
                    _ui.update {
                        it.copy(loading = false, error = e.message ?: "Error cargando negocios")
                    }
                }
        }
    }

    fun onFilterSelected(filter: Filter) {
        _ui.update { st ->
            val filtered = filterBy(filter, st.businessesAll)
            st.copy(selected = filter, businessesFiltered = filtered)
        }
    }

    // Si en algún punto usas ids de categorías externas, puedes mapearlas aquí
    fun onCategoryClicked(categoryId: String) {
        val id = categoryId.trim().lowercase()
        val mapped = when {
            id.contains("food") || id.contains("comida") -> Filter.FOOD
            id.contains("stationery") || id.contains("papeler") || id.contains("copias") -> Filter.STATIONERY
            id.contains("tutor") -> Filter.TUTORING
            id.contains("accessor") || id.contains("accesor") -> Filter.ACCESSORIES
            else -> Filter.ALL
        }
        onFilterSelected(mapped)
    }

    // Navegación a perfil de comprador
    fun onClickProfile() {
        _nav.value = HomeNav.ToBuyerProfile
    }

    fun navHandled() {
        _nav.value = HomeNav.None
    }

    /* -------------------- Helpers -------------------- */

    private fun filterBy(filter: Filter, list: List<Business>): List<Business> {
        return when (filter) {
            Filter.ALL -> list
            Filter.FOOD -> list.filter { it.matchesAny("food", "comida", "rest", "snack") }
            Filter.STATIONERY -> list.filter { it.matchesAny("stationery", "papeler", "copias", "impresion", "impresión") }
            Filter.TUTORING -> list.filter { it.matchesAny("tutor", "clases", "curso") }
            Filter.ACCESSORIES -> list.filter { it.matchesAny("accessor", "accesor") }
        }
    }

    private fun Business.matchesAny(vararg keys: String): Boolean {
        return categories.any { it.matchesAny(*keys) }
    }

    private fun Category.matchesAny(vararg keys: String): Boolean {
        val n = name.trim().lowercase()
        val i = id.trim().lowercase()
        return keys.any { k -> n.contains(k) || i.contains(k) }
    }
}