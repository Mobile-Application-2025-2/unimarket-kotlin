package com.example.unimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.model.data.serviceAdapter.OrderServiceAdapter
import com.example.unimarket.model.data.serviceAdapter.ProductsServiceAdapter
import com.example.unimarket.model.domain.entity.Cart
import com.example.unimarket.model.domain.entity.CartItem
import com.example.unimarket.model.domain.service.CartService
import com.example.unimarket.view.profile.CartItemUi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CartViewModel : ViewModel() {

    private val cartService: CartService
    private val userId: String

    private var currentCart: Cart? = null

    private val _items = MutableStateFlow<List<CartItemUi>>(emptyList())
    val items: StateFlow<List<CartItemUi>> = _items

    private val _total = MutableStateFlow(0.0)
    val total: StateFlow<Double> = _total

    // Flujo de error para mostrar mensajes en la UI (por ejemplo, sin internet)
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        val auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid.orEmpty()

        val firestore = FirebaseFirestore.getInstance()
        val orderAdapter = OrderServiceAdapter(firestore)
        val productsService = ProductsServiceAdapter()

        cartService = CartService(orderAdapter, productsService)

        // Cargar carrito desde cache si ya existe
        if (userId.isNotBlank()) {
            val cached = CartService.getCachedCart(userId)
            updateCart(cached)
        }
    }

    /**
     * Agrega [quantity] unidades del producto al carrito.
     * [quantity] se fuerza a ser al menos 1.
     */
    fun addProduct(productId: String, quantity: Int = 1) {
        if (userId.isBlank()) return

        val safeQty = quantity.coerceAtLeast(1)

        viewModelScope.launch {
            val updated = cartService.addProductToCart(
                userId = userId,
                productId = productId,
                quantity = safeQty
            )
            updateCart(updated)
        }
    }

    fun removeProduct(productId: String) {
        if (userId.isBlank()) return

        val updated = cartService.removeProductFromCart(
            userId = userId,
            productId = productId
        )
        updateCart(updated)
    }

    fun increaseQuantity(productId: String) {
        if (userId.isBlank()) return

        val updated = cartService.changeQuantity(
            userId = userId,
            productId = productId,
            delta = +1
        )
        updateCart(updated)
    }

    fun decreaseQuantity(productId: String) {
        if (userId.isBlank()) return

        val updated = cartService.changeQuantity(
            userId = userId,
            productId = productId,
            delta = -1
        )
        updateCart(updated)
    }

    fun checkout(paymentMethod: String) {
        val cartSnapshot = currentCart ?: return
        if (userId.isBlank()) return

        viewModelScope.launch {
            try {
                // Concurrencia: enviar órdenes + limpiar cache en paralelo
                coroutineScope {
                    val sendJob = async(Dispatchers.IO) {
                        cartService.sendOrdersFromCart(
                            cart = cartSnapshot,
                            paymentMethod = paymentMethod
                        )
                    }

                    val clearCacheJob = async(Dispatchers.Default) {
                        cartService.clearCart(userId)
                    }

                    sendJob.await()
                    clearCacheJob.await()
                }

                // Si todo salió bien, limpiar carrito en UI
                updateCart(null)
            } catch (e: Exception) {
                _error.value =
                    "No se pudo completar el pedido. Verifica tu conexión a internet."
            }
        }
    }

    fun errorShown() {
        _error.value = null
    }

    private fun updateCart(cart: Cart?) {
        currentCart = cart

        if (cart == null) {
            _items.value = emptyList()
            _total.value = 0.0
            return
        }

        _items.value = cart.products.map { it.toUi() }
        _total.value = cart.finalPrice
    }

    private fun CartItem.toUi(): CartItemUi =
        CartItemUi(
            id       = productId,
            name     = name,
            price    = unitPrice,
            quantity = quantity,
            imageUrl = imageUrl
        )
}