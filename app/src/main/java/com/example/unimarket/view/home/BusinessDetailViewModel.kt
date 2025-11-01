package com.example.unimarket.view.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.model.domain.entity.Business
import com.example.unimarket.model.domain.entity.Product
import com.example.unimarket.model.domain.service.BusinessService
import com.example.unimarket.model.domain.service.ProductService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.LinkedHashMap
import java.util.Locale

private val priceFormat: NumberFormat = NumberFormat.getNumberInstance(Locale("es", "CO")).apply {
    maximumFractionDigits = 0
    minimumFractionDigits = 0
}

data class BusinessProductItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val price: String,
    val rating: String,
    val imageUrl: String,
    val categoryId: String,
    val categoryLabel: String
)

data class ProductFilterOption(
    val id: String,
    val label: String
) {
    companion object {
        const val ALL_ID = "all"
    }
}

data class BusinessDetailUiState(
    val loading: Boolean = false,
    val businessId: String = "",
    val businessName: String = "",
    val categoriesText: String = "",
    val description: String = "",
    val ratingText: String = "",
    val ratingCountText: String = "",
    val heroImageUrl: String = "",
    val logoUrl: String = "",
    val filters: List<ProductFilterOption> = emptyList(),
    val selectedFilterId: String = ProductFilterOption.ALL_ID,
    val productsAll: List<BusinessProductItem> = emptyList(),
    val productsFiltered: List<BusinessProductItem> = emptyList(),
    val error: String? = null
)

class BusinessDetailViewModel(
    private val businessService: BusinessService = BusinessService(),
    private val productService: ProductService = ProductService()
) : ViewModel() {

    private val _ui = MutableStateFlow(BusinessDetailUiState())
    val ui: StateFlow<BusinessDetailUiState> = _ui.asStateFlow()

    fun load(businessId: String) {
        val trimmed = businessId.trim()
        if (trimmed.isEmpty()) {
            _ui.update { it.copy(error = "Negocio inválido") }
            return
        }

        if (_ui.value.loading && _ui.value.businessId == trimmed) return

        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, businessId = trimmed) }

            val businessResult = businessService.getBusiness(trimmed)
            val business = businessResult.getOrElse { error ->
                _ui.update { it.copy(loading = false, error = error.message ?: "Error cargando negocio") }
                return@launch
            }

            val productsResult = productService.listByBusiness(trimmed)
            val products = productsResult.getOrElse { error ->
                _ui.update { state ->
                    state.copy(
                        loading = false,
                        businessName = business.name.ifBlank { "Negocio universitario" },
                        categoriesText = buildCategoriesText(business, emptyList()),
                        description = buildDescription(business),
                        ratingText = formatRating(business.rating),
                        ratingCountText = estimateReviewsText(business, emptyList()),
                        heroImageUrl = pickHeroImage(business, emptyList()),
                        logoUrl = business.logo,
                        filters = listOf(ProductFilterOption(ProductFilterOption.ALL_ID, LABEL_ALL)),
                        selectedFilterId = ProductFilterOption.ALL_ID,
                        productsAll = emptyList(),
                        productsFiltered = emptyList(),
                        error = error.message ?: "Error cargando productos"
                    )
                }
                return@launch
            }

            val items = products.map { it.toUi(business) }
            val filters = buildFilters(items, business)
            val selectedFilter = filters.firstOrNull()?.id ?: ProductFilterOption.ALL_ID
            val filtered = applyFilter(selectedFilter, items)

            _ui.update {
                it.copy(
                    loading = false,
                    businessName = business.name.ifBlank { "Negocio universitario" },
                    categoriesText = buildCategoriesText(business, items),
                    description = buildDescription(business),
                    ratingText = formatRating(business.rating),
                    ratingCountText = estimateReviewsText(business, products),
                    heroImageUrl = pickHeroImage(business, items),
                    logoUrl = business.logo.ifBlank { pickHeroImage(business, items) },
                    filters = filters,
                    selectedFilterId = selectedFilter,
                    productsAll = items,
                    productsFiltered = filtered,
                    error = null
                )
            }
        }
    }

    fun onFilterSelected(filterId: String) {
        val target = filterId.ifBlank { ProductFilterOption.ALL_ID }
        val current = _ui.value
        if (current.selectedFilterId == target && current.productsFiltered.isNotEmpty()) return

        val filtered = applyFilter(target, current.productsAll)
        _ui.update { it.copy(selectedFilterId = target, productsFiltered = filtered) }
    }

    fun onErrorConsumed() {
        _ui.update { it.copy(error = null) }
    }

    private fun applyFilter(filterId: String, products: List<BusinessProductItem>): List<BusinessProductItem> {
        if (filterId == ProductFilterOption.ALL_ID) return products
        return products.filter { it.categoryId.equals(filterId, ignoreCase = true) }
    }

    private fun buildFilters(
        items: List<BusinessProductItem>,
        business: Business
    ): List<ProductFilterOption> {
        val result = mutableListOf(ProductFilterOption(ProductFilterOption.ALL_ID, LABEL_ALL))
        val categories = LinkedHashMap<String, String>()
        items.forEach { item ->
            val key = item.categoryId.ifBlank { item.categoryLabel }
            val label = item.categoryLabel.ifBlank { item.subtitle }
            if (key.isNotBlank() && !categories.containsKey(key)) {
                categories[key] = label
            }
        }
        if (categories.isEmpty()) {
            business.categories.forEach { cat ->
                val key = cat.id.ifBlank { cat.name }
                val label = cat.name.ifBlank { cat.id }
                if (key.isNotBlank() && !categories.containsKey(key)) {
                    categories[key] = beautifyLabel(label)
                }
            }
        }
        categories.forEach { (id, label) ->
            result.add(ProductFilterOption(id, beautifyLabel(label)))
        }
        return result
    }

    private fun buildCategoriesText(business: Business, items: List<BusinessProductItem>): String {
        val fromBusiness = business.categories.mapNotNull { cat ->
            val label = cat.name.ifBlank { cat.id }
            label.takeIf { it.isNotBlank() }
        }
        if (fromBusiness.isNotEmpty()) {
            return fromBusiness.joinToString(" · ") { beautifyLabel(it) }
        }
        val fromProducts = items.mapNotNull { item -> item.categoryLabel.takeIf { label -> label.isNotBlank() } }
        if (fromProducts.isNotEmpty()) {
            return fromProducts.distinct().joinToString(" · ") { beautifyLabel(it) }
        }
        return ""
    }

    private fun buildDescription(business: Business): String {
        val direccion = business.address.direccion.ifBlank { "" }
        return direccion.ifBlank { "Disfruta los productos de la comunidad universitaria" }
    }

    private fun estimateReviewsText(business: Business, products: List<Product>): String {
        val fromProducts = products.takeIf { it.isNotEmpty() }?.size ?: 0
        val baseline = business.products.size.takeIf { it > 0 } ?: fromProducts
        val estimated = when {
            baseline <= 0 -> 0
            baseline < 5 -> 20
            baseline < 10 -> 60
            else -> baseline * 12
        }
        return if (estimated > 0) "(${estimated}+)" else "(0)"
    }

    private fun formatRating(value: Double): String {
        if (value <= 0.0) return "4.0"
        return String.format(Locale.getDefault(), "%.1f", value)
    }

    private fun pickHeroImage(business: Business, items: List<BusinessProductItem>): String {
        val businessLogo = business.logo.trim()
        if (businessLogo.isNotEmpty()) return businessLogo
        return items.firstOrNull { it.imageUrl.isNotBlank() }?.imageUrl.orEmpty()
    }

    private fun Product.toUi(business: Business): BusinessProductItem {
        val categoryLabel = beautifyLabel(category)
        val priceText = formatPrice(price)
        val ratingText = formatRating(rating)
        return BusinessProductItem(
            id = id,
            name = name.ifBlank { "Producto" },
            subtitle = categoryLabel.ifBlank { beautifyLabel(business.name) },
            price = priceText,
            rating = ratingText,
            imageUrl = image.trim(),
            categoryId = category.ifBlank { categoryLabel.lowercase(Locale.getDefault()) },
            categoryLabel = categoryLabel
        )
    }

    private fun formatPrice(value: Double): String {
        return try {
            val formatted = priceFormat.format(value)
            "$$formatted"
        } catch (t: Throwable) {
            "$${"%.0f".format(value)}"
        }
    }

    private fun beautifyLabel(raw: String): String {
        val clean = raw.replace('_', ' ').replace('-', ' ').trim()
        if (clean.isEmpty()) return ""
        return clean.split(Regex("\\s+")).joinToString(" ") { word ->
            word.lowercase(Locale.getDefault()).replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }
        }
    }

    companion object {
        private const val LABEL_ALL = "Todos"
    }
}