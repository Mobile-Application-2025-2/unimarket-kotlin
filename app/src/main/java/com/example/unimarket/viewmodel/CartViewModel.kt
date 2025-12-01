// com/example/unimarket/viewmodel/CartViewModel.kt
package com.example.unimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.R
import com.example.unimarket.model.data.orders.OrderFirestoreAdapter
import com.example.unimarket.model.data.serviceAdapter.ProductsServiceAdapter
import com.example.unimarket.model.domain.entity.Cart
import com.example.unimarket.model.domain.entity.CartItem
import com.example.unimarket.model.domain.service.CartService
import com.example.unimarket.view.profile.CartItemUi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    init {
        val auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid.orEmpty()

        val firestore = FirebaseFirestore.getInstance()
        val orderAdapter = OrderFirestoreAdapter(firestore)
        val productsService = ProductsServiceAdapter()

        cartService = CartService(orderAdapter, productsService)

        // Cargar carrito desde cache si ya existe
        if (userId.isNotBlank()) {
            val cached = CartService.getCachedCart(userId)
            updateCart(cached)
        }
    }

    fun addProduct(productId: String, quantity: Int = 1) {
        if (userId.isBlank()) return

        viewModelScope.launch {
            val updated = cartService.addProductToCart(
                userId = userId,
                productId = productId,
                quantity = quantity
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
        if (userId.isBlank()) return

        viewModelScope.launch {
            cartService.checkoutCart(
                userId = userId,
                paymentMethod = paymentMethod
            )
            // El servicio ya limpió el cache; aquí limpias la UI
            updateCart(null)
        }
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
            id = productId,
            name = name,
            price = unitPrice,
            quantity = quantity,
            imageResId = R.drawable.ic_launcher_foreground
        )
}