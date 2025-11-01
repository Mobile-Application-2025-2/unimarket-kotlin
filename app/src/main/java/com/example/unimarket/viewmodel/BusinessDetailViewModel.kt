package com.example.unimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.unimarket.R
import com.example.unimarket.model.domain.entity.Business
import com.example.unimarket.model.domain.entity.Product
import com.example.unimarket.model.domain.service.BusinessService
import com.example.unimarket.model.domain.service.ProductService
import com.example.unimarket.view.business.BusinessProductUi
import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class BusinessDetailViewModel(
    private val businessId: String,
    private val businessService: BusinessService = BusinessService(),
    private val productService: ProductService = ProductService()
) : ViewModel() {

    private val _ui = MutableStateFlow(BusinessDetailUiState())
    val ui: StateFlow<BusinessDetailUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }

            val business = businessService.getBusiness(businessId).getOrElse { error ->
                _ui.update { current ->
                    current.copy(
                        loading = false,
                        error = error.message ?: "No fue posible cargar el negocio"
                    )
                }
                return@launch
            }

            val products = productService.listByBusiness(businessId).getOrElse { error ->
                _ui.update { current ->
                    current.copy(
                        loading = false,
                        business = business,
                        error = error.message ?: "No fue posible cargar los productos"
                    )
                }
                return@launch
            }

            val catalog = buildCatalog(products, business.rating)
            val selected = catalog.tabs.firstOrNull()?.id

            _ui.update {
                it.copy(
                    loading = false,
                    business = business,
                    tabs = catalog.tabs,
                    selectedTabId = selected,
                    productsByTab = catalog.productsByTab,
                    error = null
                )
            }
        }
    }

    fun onCategorySelected(categoryId: String) {
        if (categoryId == _ui.value.selectedTabId) return
        _ui.update { it.copy(selectedTabId = categoryId) }
    }

    private fun buildCatalog(products: List<Product>, defaultRating: Double): Catalog {
        val tabs = listOf(
            BusinessCategoryTab(DISCOUNTS, R.string.business_detail_category_discounts),
            BusinessCategoryTab(POPULAR, R.string.business_detail_category_popular),
            BusinessCategoryTab(COMBO_15, R.string.business_detail_category_combo_15),
            BusinessCategoryTab(COMBO_30, R.string.business_detail_category_combo_30),
            BusinessCategoryTab(OTHERS, R.string.business_detail_category_others)
        )

        val buckets = tabs.associate { it.id to mutableListOf<BusinessProductUi>() }.toMutableMap()
        val allItems = mutableListOf<BusinessProductUi>()

        products.forEach { product ->
            val ui = product.toUi(defaultRating)
            allItems.add(ui)

            val key = resolveCategory(product)
            buckets.getValue(key).add(ui)

            if (ui.rating >= 4.5 && key != POPULAR) {
                buckets.getValue(POPULAR).add(ui)
            }
            if (looksLikeDiscount(product) && key != DISCOUNTS) {
                buckets.getValue(DISCOUNTS).add(ui)
            }
        }

        if (buckets.getValue(OTHERS).isEmpty()) {
            buckets[OTHERS] = allItems.toMutableList()
        }

        if (buckets.getValue(POPULAR).isEmpty()) {
            val topRated = allItems.sortedByDescending { it.rating }.take(6)
            buckets[POPULAR] = topRated.toMutableList()
        }

        if (buckets.getValue(DISCOUNTS).isEmpty()) {
            val cheapest = allItems.sortedBy { it.price }.take(6)
            buckets[DISCOUNTS] = cheapest.toMutableList()
        }

        val finalMap = buckets.mapValues { (_, list) ->
            list.distinctBy { item ->
                if (item.id.isNotBlank()) item.id else "${item.name}_${item.price}".lowercase(Locale.getDefault())
            }
        }

        return Catalog(tabs, finalMap)
    }

    private fun resolveCategory(product: Product): String {
        val haystack = buildList {
            add(product.category)
            add(product.name)
            add(product.description)
        }.joinToString(" ").lowercase(Locale.getDefault())

        return when {
            haystack.contains("combo") && haystack.contains("30") -> COMBO_30
            haystack.contains("combo") && haystack.contains("15") -> COMBO_15
            looksLikeDiscount(product) -> DISCOUNTS
            haystack.contains("popu") || product.rating >= 4.5 -> POPULAR
            else -> OTHERS
        }
    }

    private fun looksLikeDiscount(product: Product): Boolean {
        val haystack = buildList {
            add(product.category)
            add(product.name)
            add(product.description)
        }.joinToString(" ").lowercase(Locale.getDefault())
        return haystack.contains("desc") || haystack.contains("promo") || haystack.contains("oferta")
    }

    private fun Product.toUi(fallbackRating: Double): BusinessProductUi {
        val safeId = id.ifBlank {
            "${name}_${category}_${price}".lowercase(Locale.getDefault())
        }
        val ratingValue = if (rating > 0) rating else if (fallbackRating > 0) fallbackRating else 4.0
        return BusinessProductUi(
            id = safeId,
            name = name.ifBlank { "Producto" },
            price = price,
            rating = ratingValue,
            imageUrl = image
        )
    }

    data class Catalog(
        val tabs: List<BusinessCategoryTab>,
        val productsByTab: Map<String, List<BusinessProductUi>>
    )

    companion object {
        private const val DISCOUNTS = "discounts"
        private const val POPULAR = "popular"
        private const val COMBO_15 = "combo_15"
        private const val COMBO_30 = "combo_30"
        private const val OTHERS = "others"
    }
}

data class BusinessDetailUiState(
    val loading: Boolean = true,
    val business: Business? = null,
    val tabs: List<BusinessCategoryTab> = emptyList(),
    val selectedTabId: String? = null,
    val productsByTab: Map<String, List<BusinessProductUi>> = emptyMap(),
    val error: String? = null
)

data class BusinessCategoryTab(
    val id: String,
    @StringRes val labelRes: Int
)

class BusinessDetailViewModelFactory(
    private val businessId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BusinessDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BusinessDetailViewModel(businessId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
