package com.example.unimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.model.domain.entity.Business
import com.example.unimarket.model.domain.entity.Category
import com.example.unimarket.model.domain.entity.Product
import com.example.unimarket.model.domain.service.BusinessService
import com.example.unimarket.model.domain.service.CategoryService
import com.example.unimarket.model.domain.service.ProductService
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Filter { ALL, FOOD, STATIONERY, TUTORING, ACCESSORIES }

data class HomeUiState(
    val businessesAll: List<Business> = emptyList(),
    val businessesFiltered: List<Business> = emptyList(),
    val selected: Filter = Filter.ALL,
    val loading: Boolean = false,
    val error: String? = null
)

data class BusinessDetailUiState(
    val businessId: String = "",
    val businessName: String = "",
    val currentFilter: String = HomeBuyerViewModel.FILTER_ALL,
    val categories: List<String> = emptyList(),
    val products: List<Product> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

sealed class HomeNav {
    data object None : HomeNav()
    data object ToBuyerProfile : HomeNav()
    data class ToBusinessDetail(val business: Business) : HomeNav()
}

class HomeBuyerViewModel(
    private val service: BusinessService = BusinessService(),
    private val categoryService: CategoryService = CategoryService(),
    private val productService: ProductService = ProductService()
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui

    private val _nav = MutableStateFlow<HomeNav>(HomeNav.None)
    val nav: StateFlow<HomeNav> = _nav

    private val _detail = MutableStateFlow(BusinessDetailUiState())
    val detail: StateFlow<BusinessDetailUiState> = _detail

    init { loadBusinesses() }

    private fun loadBusinesses() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            service.getAllBusinesses()
                .onSuccess { list ->
                    val sorted = list.sortedByDescending { it.rating }
                    _ui.update { st ->
                        val filtered = filterBy(st.selected, sorted)
                        st.copy(
                            businessesAll = sorted,
                            businessesFiltered = filtered,
                            loading = false
                        )
                    }
                }
                .onFailure { e ->
                    _ui.update {
                        it.copy(
                            loading = false,
                            error = e.message ?: "Error cargando negocios"
                        )
                    }
                }
        }
    }

    fun onFilterSelected(filter: Filter) {
        _ui.update { st ->
            val filtered = filterBy(filter, st.businessesAll)
            st.copy(selected = filter, businessesFiltered = filtered)
        }
        incrementCategoryCount(filter)
    }

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

    fun onBusinessSelected(business: Business) {
        if (business.id.isBlank()) return
        _nav.value = HomeNav.ToBusinessDetail(business)
    }

    fun onClickProfile() { _nav.value = HomeNav.ToBuyerProfile }
    fun navHandled()     { _nav.value = HomeNav.None }

    // --------- DETALLE DE NEGOCIO (MVVM) ---------

    fun detail_init(businessId: String, businessName: String) {
        _detail.value = BusinessDetailUiState(
            businessId = businessId,
            businessName = businessName,
            currentFilter = FILTER_ALL,
            categories = emptyList(),
            products = emptyList(),
            loading = false,
            error = null
        )
    }

    fun detail_loadProducts(productIds: List<String>) {
        if (productIds.isEmpty()) {
            _detail.update { it.copy(products = emptyList(), loading = false, error = null) }
            return
        }

        viewModelScope.launch {
            _detail.update { it.copy(loading = true, error = null) }

            productService.listByIds(productIds)
                .onSuccess { list ->
                    val cats = list
                        .map { it.category.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                        .sorted()
                    _detail.update {
                        it.copy(
                            products = list,
                            categories = cats,
                            loading = false,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    _detail.update {
                        it.copy(
                            loading = false,
                            error = e.message ?: "Error cargando productos"
                        )
                    }
                }
        }
    }

    fun detail_onFilterSelected(filterTag: String) {
        _detail.update { st ->
            st.copy(currentFilter = filterTag)
        }
    }

    fun detail_errorShown() {
        _detail.update { it.copy(error = null) }
    }


    private fun filterBy(filter: Filter, list: List<Business>): List<Business> =
        when (filter) {
            Filter.ALL -> list
            Filter.FOOD -> list.filter { it.matchesAny("food", "comida", "rest", "snack") }
            Filter.STATIONERY -> list.filter { it.matchesAny("stationery", "papeler", "copias", "impresion", "impresión") }
            Filter.TUTORING -> list.filter { it.matchesAny("tutor", "clases", "curso") }
            Filter.ACCESSORIES -> list.filter { it.matchesAny("accessor", "accesor") }
        }

    private fun Business.matchesAny(vararg keys: String): Boolean =
        categories.any { it.matchesAny(*keys) }

    private fun Category.matchesAny(vararg keys: String): Boolean {
        val n = name.trim().lowercase()
        val i = id.trim().lowercase()
        return keys.any { k -> n.contains(k) || i.contains(k) }
    }

    private fun incrementCategoryCount(filter: Filter) {
        val docId = when (filter) {
            Filter.FOOD        -> "comida"
            Filter.STATIONERY  -> "papeleria"
            Filter.TUTORING    -> "tutorias"
            Filter.ACCESSORIES -> "accesorios"
            Filter.ALL         -> null
        } ?: return

        viewModelScope.launch {
            try {
                categoryService.update(docId, mapOf("count" to FieldValue.increment(1)))
            } catch (_: Exception) {
            }
        }
    }

    fun rateBusiness(businessId: String, newAvg: Float, newCount: Int) {
        if (businessId.isBlank()) return

        viewModelScope.launch {
            service.updateRating(
                businessId = businessId,
                rating = newAvg.toDouble(),
                amountRatings = newCount.toLong()
            ).onSuccess {
                _ui.update { st ->
                    val updatedAll = st.businessesAll.map { b ->
                        if (b.id == businessId) {
                            b.copy(
                                rating = newAvg.toDouble(),
                                amountRatings = newCount.toLong()
                            )
                        } else b
                    }
                    st.copy(
                        businessesAll = updatedAll,
                        businessesFiltered = filterBy(st.selected, updatedAll)
                    )
                }
            }.onFailure { e ->
                _detail.update {
                    it.copy(error = e.message ?: "Error actualizando calificación")
                }
            }
        }
    }

    companion object {
        const val FILTER_ALL = "all"
    }
}