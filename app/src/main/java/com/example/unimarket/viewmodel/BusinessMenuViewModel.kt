package com.example.unimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.model.domain.entity.Business
import com.example.unimarket.model.domain.entity.Product
import com.example.unimarket.model.domain.service.BusinessService
import com.example.unimarket.model.domain.service.ProductService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class BusinessMenuUiState(
    val isLoading: Boolean = true,
    val business: Business? = null,
    val categories: List<MenuCategory> = emptyList(),
    val sections: List<MenuSection> = emptyList(),
    val selectedCategoryKey: String = BusinessMenuViewModel.DEFAULT_CATEGORY_ALL_KEY,
    val selectedCategoryLabel: String = BusinessMenuViewModel.DEFAULT_CATEGORY_ALL_LABEL,
    val reviewCount: Int = 0,
    val error: String? = null
)

data class MenuCategory(
    val key: String,
    val label: String
)

data class MenuSection(
    val key: String,
    val title: String,
    val products: List<Product>
)

class BusinessMenuViewModel(
    private val businessService: BusinessService = BusinessService(),
    private val productService: ProductService = ProductService()
) : ViewModel() {

    private val _ui = MutableStateFlow(BusinessMenuUiState())
    val ui: StateFlow<BusinessMenuUiState> = _ui

    private var cachedCategories: List<MenuCategory> = emptyList()
    private var cachedSections: List<MenuSection> = emptyList()

    fun loadBusiness(businessId: String) {
        if (businessId.isBlank()) {
            _ui.update {
                it.copy(
                    isLoading = false,
                    error = "Business id no es válido"
                )
            }
            return
        }

        _ui.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val businessResult = businessService.getBusiness(businessId)
            if (businessResult.isFailure) {
                _ui.update {
                    it.copy(
                        isLoading = false,
                        error = businessResult.exceptionOrNull()?.message
                            ?: "No fue posible cargar el negocio"
                    )
                }
                return@launch
            }

            val business = businessResult.getOrThrow()

            val productsResult = productService.listByBusiness(businessId)
            if (productsResult.isFailure) {
                cachedCategories = emptyList()
                cachedSections = emptyList()

                _ui.update {
                    it.copy(
                        isLoading = false,
                        business = business,
                        categories = emptyList(),
                        sections = emptyList(),
                        selectedCategoryKey = DEFAULT_CATEGORY_ALL_KEY,
                        selectedCategoryLabel = DEFAULT_CATEGORY_ALL_LABEL,
                        reviewCount = 0,
                        error = productsResult.exceptionOrNull()?.message
                            ?: "No fue posible cargar los productos"
                    )
                }
                return@launch
            }

            val products = productsResult.getOrThrow()

            val categories = buildCategories(business, products)
            val sections = buildSections(products, categories)
            val availableCategories = categories.filter { category ->
                sections.any { it.key.equals(category.key, ignoreCase = true) }
            }

            cachedCategories = availableCategories
            cachedSections = sections

            val selectedKey = sections.firstOrNull()?.key
                ?: availableCategories.firstOrNull()?.key
                ?: DEFAULT_CATEGORY_ALL_KEY
            val selectedLabel = resolveCategoryLabel(selectedKey, availableCategories)
            val reviewCount = products.sumOf { it.comments.size }

            _ui.update {
                it.copy(
                    isLoading = false,
                    business = business,
                    categories = availableCategories,
                    sections = sections,
                    selectedCategoryKey = selectedKey,
                    selectedCategoryLabel = selectedLabel,
                    reviewCount = reviewCount,
                    error = null
                )
            }
        }
    }

    fun onCategorySelected(key: String) {
        val safeKey = cachedSections.firstOrNull { it.key.equals(key, ignoreCase = true) }?.key
            ?: cachedSections.firstOrNull()?.key
            ?: cachedCategories.firstOrNull()?.key
            ?: DEFAULT_CATEGORY_ALL_KEY

        if (safeKey.equals(_ui.value.selectedCategoryKey, ignoreCase = true)) return

        val selectedLabel = resolveCategoryLabel(safeKey, cachedCategories)

        _ui.update {
            it.copy(
                selectedCategoryKey = safeKey,
                selectedCategoryLabel = selectedLabel
            )
        }
    }

    private fun buildCategories(
        business: Business,
        products: List<Product>
    ): List<MenuCategory> {
        if (products.isEmpty()) return emptyList()

        val businessCategories = business.categories
        val uniqueKeys = products.mapNotNull { product ->
            product.category.trim().takeIf { it.isNotEmpty() }
        }.distinctBy { it.lowercase(Locale.getDefault()) }

        val dynamic = uniqueKeys.map { key ->
            val labelFromBusiness = businessCategories.firstOrNull { category ->
                val idKey = category.id.trim()
                val nameKey = category.name.trim()
                key.equals(idKey, ignoreCase = true) || key.equals(nameKey, ignoreCase = true)
            }?.name?.trim().takeUnless { it.isNullOrEmpty() }

            val label = labelFromBusiness ?: key.toCategoryLabel()
            MenuCategory(key, label)
        }.sortedBy { it.label.lowercase(Locale.getDefault()) }

        val categories = mutableListOf<MenuCategory>()
        categories += MenuCategory(DEFAULT_CATEGORY_ALL_KEY, DEFAULT_CATEGORY_ALL_LABEL)
        categories += MenuCategory(DEFAULT_CATEGORY_POPULAR_KEY, DEFAULT_CATEGORY_POPULAR_LABEL)
        categories += dynamic

        return categories
    }

    private fun buildSections(
        products: List<Product>,
        categories: List<MenuCategory>
    ): List<MenuSection> {
        if (products.isEmpty()) return emptyList()

        val sections = mutableListOf<MenuSection>()

        val deals = products.sortedBy { it.price }.take(8)
        if (deals.isNotEmpty()) {
            sections += MenuSection(
                key = DEFAULT_CATEGORY_ALL_KEY,
                title = DEFAULT_CATEGORY_ALL_LABEL,
                products = deals
            )
        }

        val popular = products.sortedByDescending { it.rating }.take(8)
        if (popular.isNotEmpty()) {
            sections += MenuSection(
                key = DEFAULT_CATEGORY_POPULAR_KEY,
                title = DEFAULT_CATEGORY_POPULAR_LABEL,
                products = popular
            )
        }

        val byCategory = products.groupBy { it.category.trim() }

        categories.forEach { category ->
            if (category.key == DEFAULT_CATEGORY_ALL_KEY || category.key == DEFAULT_CATEGORY_POPULAR_KEY) return@forEach
            val items = byCategory.entries
                .firstOrNull { entry -> entry.key.equals(category.key, ignoreCase = true) }
                ?.value
                ?.filter { it.category.isNotBlank() }
                ?.sortedBy { it.name.lowercase(Locale.getDefault()) }
                .orEmpty()
            if (items.isNotEmpty()) {
                sections += MenuSection(
                    key = category.key,
                    title = category.label,
                    products = items
                )
            }
        }

        return sections
    }

    private fun resolveCategoryLabel(
        key: String?,
        categories: List<MenuCategory>
    ): String {
        if (key.isNullOrEmpty()) return DEFAULT_CATEGORY_ALL_LABEL
        val fromList = categories.firstOrNull { category ->
            category.key.equals(key, ignoreCase = true)
        }
        if (fromList != null) return fromList.label

        return when (key) {
            DEFAULT_CATEGORY_ALL_KEY -> DEFAULT_CATEGORY_ALL_LABEL
            DEFAULT_CATEGORY_POPULAR_KEY -> DEFAULT_CATEGORY_POPULAR_LABEL
            else -> key.toCategoryLabel()
        }
    }

    private fun String.toCategoryLabel(): String {
        if (isBlank()) return DEFAULT_CATEGORY_ALL_LABEL
        val raw = replace("_", " ").trim()
        if (raw.isEmpty()) return DEFAULT_CATEGORY_ALL_LABEL
        return raw.lowercase(Locale.getDefault())
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                }
            }
    }

    companion object {
        const val DEFAULT_CATEGORY_ALL_KEY = "menu_all"
        const val DEFAULT_CATEGORY_POPULAR_KEY = "menu_popular"

        const val DEFAULT_CATEGORY_ALL_LABEL = "Descuentos"
        const val DEFAULT_CATEGORY_POPULAR_LABEL = "Populares"
    }
}