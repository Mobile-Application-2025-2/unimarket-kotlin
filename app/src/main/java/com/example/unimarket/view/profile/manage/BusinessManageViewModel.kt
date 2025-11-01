package com.example.unimarket.view.profile.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.model.data.firebase.FirebaseAuthProvider
import com.example.unimarket.model.domain.entity.Business
import com.example.unimarket.model.domain.entity.Product
import com.example.unimarket.model.domain.service.BusinessService
import com.example.unimarket.model.domain.service.ProductService
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val priceFormat: NumberFormat = NumberFormat.getNumberInstance(Locale("es", "CO")).apply {
    maximumFractionDigits = 0
    minimumFractionDigits = 0
}

data class CategoryOption(
    val id: String,
    val label: String
)

data class BusinessOwnerProductItem(
    val id: String,
    val name: String,
    val categoryLabel: String,
    val categoryId: String,
    val description: String,
    val priceLabel: String,
    val priceValue: Double,
    val ratingLabel: String,
    val ratingValue: Double,
    val imageUrl: String
)

data class BusinessManageUiState(
    val loading: Boolean = false,
    val productsLoading: Boolean = false,
    val businessId: String = "",
    val businessName: String = "",
    val description: String = "",
    val heroImageUrl: String = "",
    val logoUrl: String = "",
    val statusLabel: String = "",
    val isOpen: Boolean = true,
    val categories: List<CategoryOption> = emptyList(),
    val products: List<BusinessOwnerProductItem> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

data class ProductEditorData(
    val businessId: String,
    val productId: String?,
    val name: String,
    val description: String,
    val imageUrl: String,
    val price: Double,
    val categoryId: String,
    val categoryLabel: String
)

data class ProductEditorInput(
    val businessId: String,
    val productId: String?,
    val name: String,
    val description: String,
    val imageUrl: String,
    val priceText: String,
    val categoryLabel: String,
    val categoryId: String?
)

data class BusinessInfoEditorData(
    val businessId: String,
    val name: String,
    val description: String,
    val status: String,
    val isOpen: Boolean,
    val logoUrl: String,
    val bannerUrl: String
)

data class BusinessInfoInput(
    val businessId: String,
    val name: String,
    val description: String,
    val status: String,
    val isOpen: Boolean,
    val logoUrl: String,
    val bannerUrl: String
)

class BusinessManageViewModel(
    private val businessService: BusinessService = BusinessService(),
    private val productService: ProductService = ProductService()
) : ViewModel() {

    private val _ui = MutableStateFlow(BusinessManageUiState(loading = true))
    val ui: StateFlow<BusinessManageUiState> = _ui.asStateFlow()

    private var currentBusiness: Business? = null

    fun load() {
        val uid = FirebaseAuthProvider.auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _ui.update {
                it.copy(
                    loading = false,
                    error = "Inicia sesión para administrar tu negocio"
                )
            }
            return
        }

        if (_ui.value.loading && _ui.value.businessId == uid) return

        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, businessId = uid) }

            val businessResult = businessService.getBusiness(uid)
            val business = businessResult.getOrElse { error ->
                _ui.update { it.copy(loading = false, error = error.message ?: DEFAULT_ERROR) }
                return@launch
            }

            currentBusiness = business
            val productsResult = productService.listByBusiness(uid)
            val products = productsResult.getOrElse { error ->
                _ui.update {
                    buildState(business, emptyList()).copy(
                        loading = false,
                        error = error.message ?: DEFAULT_ERROR
                    )
                }
                return@launch
            }

            _ui.update { buildState(business, products).copy(loading = false) }
        }
    }

    fun refreshProducts() {
        val businessId = currentBusiness?.id
        if (businessId.isNullOrBlank()) return
        viewModelScope.launch {
            _ui.update { it.copy(productsLoading = true) }
            val productsResult = productService.listByBusiness(businessId)
            val products = productsResult.getOrElse { error ->
                _ui.update {
                    it.copy(
                        productsLoading = false,
                        error = error.message ?: DEFAULT_ERROR
                    )
                }
                return@launch
            }
            val business = currentBusiness ?: return@launch
            _ui.update {
                buildState(business, products).copy(
                    loading = false,
                    productsLoading = false,
                    message = it.message
                )
            }
        }
    }

    fun consumeError() {
        _ui.update { it.copy(error = null) }
    }

    fun consumeMessage() {
        _ui.update { it.copy(message = null) }
    }

    fun prepareBusinessInfo(): BusinessInfoEditorData? {
        val business = currentBusiness ?: return null
        return BusinessInfoEditorData(
            businessId = business.id,
            name = business.name,
            description = business.description,
            status = business.status.ifBlank { statusLabelFor(business) },
            isOpen = business.isOpen,
            logoUrl = business.logo,
            bannerUrl = business.banner
        )
    }

    suspend fun prepareProductEditor(productId: String?): Result<ProductEditorData> {
        val business = currentBusiness ?: return Result.failure(IllegalStateException("Negocio no cargado"))
        val id = productId.orEmpty()
        return if (id.isBlank()) {
            Result.success(
                ProductEditorData(
                    businessId = business.id,
                    productId = null,
                    name = "",
                    description = "",
                    imageUrl = "",
                    price = 0.0,
                    categoryId = "",
                    categoryLabel = business.categories.firstOrNull()?.name.orEmpty()
                )
            )
        } else {
            productService.getById(id).map { product ->
                ProductEditorData(
                    businessId = business.id,
                    productId = product.id,
                    name = product.name,
                    description = product.description,
                    imageUrl = product.image,
                    price = product.price,
                    categoryId = product.category,
                    categoryLabel = product.categoryLabel.ifBlank { product.category }
                )
            }
        }
    }

    fun saveBusinessInfo(input: BusinessInfoInput) {
        if (input.businessId.isBlank()) return
        viewModelScope.launch {
            _ui.update { it.copy(loading = true) }
            val result = businessService.updateBusiness(
                businessId = input.businessId,
                name = input.name,
                description = input.description,
                status = input.status,
                isOpen = input.isOpen,
                logoUrl = input.logoUrl,
                bannerUrl = input.bannerUrl
            )
            result.onFailure { error ->
                _ui.update {
                    it.copy(
                        loading = false,
                        error = error.message ?: DEFAULT_ERROR
                    )
                }
            }
            result.onSuccess {
                val refreshed = businessService.getBusiness(input.businessId).getOrNull()
                if (refreshed != null) {
                    currentBusiness = refreshed
                    val products = productService.listByBusiness(input.businessId).getOrElse { emptyList() }
                    _ui.update {
                        buildState(refreshed, products).copy(
                            loading = false,
                            message = "Información actualizada correctamente"
                        )
                    }
                } else {
                    _ui.update { it.copy(loading = false) }
                }
            }
        }
    }

    fun saveProduct(input: ProductEditorInput) {
        if (input.businessId.isBlank()) return
        viewModelScope.launch {
            _ui.update { it.copy(productsLoading = true) }
            val sanitizedName = input.name.trim()
            val sanitizedDescription = input.description.trim()
            val sanitizedImage = input.imageUrl.trim()
            val price = parsePrice(input.priceText)
            val categoryLabel = input.categoryLabel.trim().ifBlank { input.categoryId.orEmpty() }
            val categoryId = input.categoryId?.takeIf { it.isNotBlank() }
                ?: slugify(categoryLabel.ifBlank { sanitizedName })

            if (input.productId.isNullOrBlank()) {
                val product = Product(
                    name = sanitizedName,
                    price = price,
                    description = sanitizedDescription,
                    category = categoryId,
                    categoryLabel = categoryLabel,
                    business = input.businessId,
                    image = sanitizedImage
                )
                val result = productService.createProduct(product)
                result.onFailure { error ->
                    _ui.update {
                        it.copy(
                            productsLoading = false,
                            error = error.message ?: DEFAULT_ERROR
                        )
                    }
                    return@launch
                }
            } else {
                val baseProduct = productService.getById(input.productId).getOrElse { error ->
                    _ui.update {
                        it.copy(
                            productsLoading = false,
                            error = error.message ?: DEFAULT_ERROR
                        )
                    }
                    return@launch
                }
                val updated = baseProduct.copy(
                    name = sanitizedName,
                    price = price,
                    description = sanitizedDescription,
                    category = categoryId,
                    categoryLabel = categoryLabel,
                    business = input.businessId,
                    image = sanitizedImage
                )
                val result = productService.updateProduct(input.productId, updated)
                result.onFailure { error ->
                    _ui.update {
                        it.copy(
                            productsLoading = false,
                            error = error.message ?: DEFAULT_ERROR
                        )
                    }
                    return@launch
                }
            }

            val products = productService.listByBusiness(input.businessId).getOrElse { emptyList() }
            val business = currentBusiness
            if (business != null) {
                _ui.update {
                    buildState(business, products).copy(
                        loading = false,
                        productsLoading = false,
                        message = if (input.productId.isNullOrBlank()) {
                            "Producto creado"
                        } else {
                            "Producto actualizado"
                        }
                    )
                }
            } else {
                _ui.update { it.copy(productsLoading = false) }
            }
        }
    }

    private fun buildState(business: Business, products: List<Product>): BusinessManageUiState {
        val hero = chooseHero(business, products)
        val categories = business.categories
            .mapNotNull { cat ->
                val label = cat.name.ifBlank { cat.id }
                val id = cat.id.ifBlank { slugify(label) }
                if (label.isBlank()) null else CategoryOption(id = id, label = label)
            }
        val productItems = products.map { it.toOwnerItem() }
        return BusinessManageUiState(
            loading = _ui.value.loading,
            productsLoading = _ui.value.productsLoading,
            businessId = business.id,
            businessName = business.name,
            description = business.description,
            heroImageUrl = hero,
            logoUrl = business.logo,
            statusLabel = statusLabelFor(business),
            isOpen = business.isOpen,
            categories = categories,
            products = productItems,
            error = _ui.value.error,
            message = _ui.value.message
        )
    }

    private fun chooseHero(business: Business, products: List<Product>): String {
        if (business.banner.isNotBlank()) return business.banner
        if (business.logo.isNotBlank()) return business.logo
        return products.firstOrNull { it.image.isNotBlank() }?.image.orEmpty()
    }

    private fun statusLabelFor(business: Business): String {
        if (business.status.isNotBlank()) return business.status
        return if (business.isOpen) DEFAULT_STATUS_OPEN else DEFAULT_STATUS_CLOSED
    }

    private fun Product.toOwnerItem(): BusinessOwnerProductItem {
        val label = categoryLabel.ifBlank { category }
        val formattedPrice = formatPrice(price)
        val formattedRating = if (rating <= 0.0) "4.0" else String.format(Locale.getDefault(), "%.1f", rating)
        return BusinessOwnerProductItem(
            id = id,
            name = name.ifBlank { "Producto" },
            categoryLabel = label,
            categoryId = category,
            description = description,
            priceLabel = formattedPrice,
            priceValue = price,
            ratingLabel = formattedRating,
            ratingValue = rating,
            imageUrl = image
        )
    }

    private fun parsePrice(raw: String): Double {
        val clean = raw.replace("[^0-9,.-]".toRegex(), "").trim()
        if (clean.isEmpty()) return 0.0
        val normalized = if (clean.count { it == ',' } == 1 && clean.count { it == '.' } == 0) {
            clean.replace(',', '.')
        } else {
            clean.replace(",", "")
        }
        return normalized.toDoubleOrNull() ?: 0.0
    }

    private fun formatPrice(value: Double): String {
        return try {
            "${'$'}${priceFormat.format(value)}"
        } catch (t: Throwable) {
            "${'$'}${String.format(Locale.getDefault(), "%.0f", value)}"
        }
    }

    private fun slugify(raw: String): String {
        val base = raw.lowercase(Locale.getDefault()).trim()
        if (base.isEmpty()) return "categoria"
        return base.replace("[^a-z0-9]+".toRegex(), "-").trim('-')
    }

    companion object {
        private const val DEFAULT_ERROR = "Algo salió mal"
        private const val DEFAULT_STATUS_OPEN = "Abierto"
        private const val DEFAULT_STATUS_CLOSED = "Cerrado"
    }
}