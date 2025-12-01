package com.example.unimarket.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.model.domain.entity.Product
import com.example.unimarket.model.domain.service.ProductService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val loading: Boolean = false,
    val error: String? = null,

    val productId: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val price: Double = 0.0,
    val rating: Double = 0.0,
    val ratingCount: Long = 0L,
    val time: String = "20 min",

    val quantity: Int = 1
)

class ProductDetailViewModel(
    application: Application,
    private val productService: ProductService
) : AndroidViewModel(application) {

    // constructor secundario para el factory por defecto
    constructor(application: Application) : this(
        application,
        ProductService()
    )

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState

    fun loadProduct(productId: String) {
        if (productId.isBlank()) {
            _uiState.update {
                it.copy(error = "Producto inválido")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }

            productService.getById(productId)
                .onSuccess { product: Product ->
                    _uiState.update { st ->
                        st.copy(
                            loading = false,
                            error = null,
                            productId = product.id,
                            name = product.name,
                            description = product.description,
                            imageUrl = product.image,
                            price = product.price,
                            rating = product.rating
                        )
                    }
                }
            productService.getRatingMeta(productId)
                .onSuccess { (avg, count) ->
                    _uiState.update { st ->
                        st.copy(
                            rating      = avg,
                            ratingCount = count
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = e.message ?: "Error cargando producto"
                        )
                    }
                }
        }
    }

    fun incrementQuantity() {
        _uiState.update { st ->
            val newQty = (st.quantity + 1).coerceAtMost(20) // límite arbitrario
            st.copy(quantity = newQty)
        }
    }

    fun decrementQuantity() {
        _uiState.update { st ->
            val newQty = (st.quantity - 1).coerceAtLeast(1)
            st.copy(quantity = newQty)
        }
    }

    fun rateProduct(newAvg: Float, newCount: Int) {
        val id = _uiState.value.productId
        if (id.isBlank()) return

        viewModelScope.launch {
            productService.updateRating(
                productId = id,
                newAvg    = newAvg.toDouble(),
                newCount  = newCount.toLong()
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        rating      = newAvg.toDouble(),
                        ratingCount = newCount.toLong()
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(error = e.message ?: "Error actualizando calificación")
                }
            }
        }
    }

    fun errorShown() {
        _uiState.update { it.copy(error = null) }
    }
}