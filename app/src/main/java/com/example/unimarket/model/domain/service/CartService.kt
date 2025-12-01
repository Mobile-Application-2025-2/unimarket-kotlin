package com.example.unimarket.model.domain.service

import android.util.LruCache
import com.example.unimarket.model.data.orders.OrderFirestoreAdapter
import com.example.unimarket.model.data.serviceAdapter.ProductsServiceAdapter
import com.example.unimarket.model.domain.entity.Cart
import com.example.unimarket.model.domain.entity.CartItem
import com.example.unimarket.model.domain.entity.Order
import com.example.unimarket.model.domain.entity.Product
import java.util.Date

class CartService(
    private val orderAdapter: OrderFirestoreAdapter,
    private val productsService: ProductsServiceAdapter
) {

    companion object {
        // Cache LRU de carritos por usuario
        private val cartCache = LruCache<String, Cart>(20)

        fun getCachedCart(userId: String): Cart? = cartCache.get(userId)

        private fun saveCart(userId: String, cart: Cart?) {
            if (cart == null) {
                cartCache.remove(userId)
            } else {
                cartCache.put(userId, cart)
            }
        }
    }

    /**
     * Agrega un producto al carrito del usuario (en cache).
     * Si no existe carrito, crea uno nuevo.
     */
    suspend fun addProductToCart(
        userId: String,
        productId: String,
        quantity: Int = 1
    ): Cart {
        val current = getCachedCart(userId)

        val product: Product = productsService.getById(productId)
            ?: error("Producto $productId no encontrado en Firestore")

        val items = current?.products?.toMutableList() ?: mutableListOf()

        val index = items.indexOfFirst { it.productId == productId }
        if (index >= 0) {
            val old = items[index]
            items[index] = old.copy(quantity = old.quantity + quantity)
        } else {
            items.add(
                CartItem(
                    productId = productId,
                    name = product.name,
                    unitPrice = product.price,
                    quantity = quantity
                )
            )
        }

        val updated = Cart.fromProducts(
            userId = userId,
            products = items
        )
        saveCart(userId, updated)
        return updated
    }

    /**
     * Elimina completamente un producto del carrito del usuario.
     */
    fun removeProductFromCart(
        userId: String,
        productId: String
    ): Cart? {
        val current = getCachedCart(userId) ?: return null

        val newItems = current.products.filter { it.productId != productId }
        val updated =
            if (newItems.isEmpty()) null
            else Cart.fromProducts(userId = userId, products = newItems)

        saveCart(userId, updated)
        return updated
    }

    /**
     * Cambia la cantidad de un producto en ±delta.
     * Si la cantidad llega a 0 o menos, lo elimina del carrito.
     */
    fun changeQuantity(
        userId: String,
        productId: String,
        delta: Int
    ): Cart? {
        val current = getCachedCart(userId) ?: return null

        val items = current.products.toMutableList()
        val index = items.indexOfFirst { it.productId == productId }
        if (index < 0) return current

        val item = items[index]
        val newQty = item.quantity + delta

        if (newQty <= 0) {
            items.removeAt(index)
        } else {
            items[index] = item.copy(quantity = newQty)
        }

        val updated =
            if (items.isEmpty()) null
            else Cart.fromProducts(userId = userId, products = items)

        saveCart(userId, updated)
        return updated
    }

    /**
     * Checkout del carrito del usuario:
     * - Lee el carrito desde cache.
     * - Separa productos por negocio.
     * - Crea las Order en Firestore.
     * - Limpia el carrito en cache.
     */
    suspend fun checkoutCart(
        userId: String,
        paymentMethod: String
    ) {
        val cart = getCachedCart(userId) ?: return

        val productById = loadProductsForCart(cart)

        val itemsByBusiness = cart.products.groupBy { item ->
            val product = productById[item.productId]
                ?: error("Producto ${item.productId} no encontrado al hacer checkout")
            product.business
        }

        for ((businessId, items) in itemsByBusiness) {
            val order = Order(
                id            = "",
                businessId    = businessId,
                userId        = cart.userId,
                products      = items.map { it.productId },
                units         = items.map { it.quantity },
                paymentMethod = paymentMethod,
                date          = Date()
            )

            orderAdapter.upsertOrder(order)
        }

        // Limpia carrito del cache tras el checkout
        saveCart(userId, null)
    }

    private suspend fun loadProductsForCart(cart: Cart): Map<String, Product> {
        val ids = cart.products.map { it.productId }.distinct()
        val result = mutableMapOf<String, Product>()

        for (id in ids) {
            val product = productsService.getById(id)
            if (product != null) {
                result[id] = product
            }
        }
        return result
    }
}