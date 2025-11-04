package com.example.unimarket.model.domain.service

import com.example.unimarket.model.data.dao.BuyersDao
import com.example.unimarket.model.data.dao.ProductsDao
import com.example.unimarket.model.data.firebase.FirebaseAuthProvider
import com.example.unimarket.model.data.firebase.FirestoreProvider
import com.example.unimarket.model.domain.validation.Validators.requireNotBlank
import kotlinx.coroutines.tasks.await

class CartService(
    private val buyersDao: BuyersDao = BuyersDao(),
    private val productsDao: ProductsDao = ProductsDao()
) {
    suspend fun addProduct(productId: String): Result<Unit> = runCatching {
        requireNotBlank(productId, "productId")
        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("Not authenticated")
        val product = productsDao.getById(productId) ?: error("Product not found")
        buyersDao.addToCart(uid, productId, product.price, product.business /* o businessId */)
    }

    suspend fun removeProduct(productId: String): Result<Unit> = runCatching {
        requireNotBlank(productId, "productId")
        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("Not authenticated")
        val product = productsDao.getById(productId) ?: error("Product not found")
        buyersDao.removeFromCart(uid, productId, product.price)
    }

    suspend fun clear(): Result<Unit> = runCatching {
        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("Not authenticated")
        buyersDao.clearCart(uid)
    }
    suspend fun checkout(): Result<String> = runCatching {
        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("Not authenticated")
        val buyer = BuyersDao().getById(uid) ?: error("Buyer not found")
        val cart = buyer.cart
        require(cart.products.isNotEmpty()) { "Carrito vacío" }
        require(cart.business.isNotBlank()) { "Carrito sin negocio" }
        val lines = cart.products.map { (productId, qty) ->
            val p = productsDao.getById(productId) ?: error("Producto no existe: $productId")
            mapOf(
                "productId" to productId,
                "unitPrice" to p.price,
                "quantity" to qty,
                "lineTotal" to p.price * qty
            )
        }
        val total = lines.sumOf { (it["lineTotal"] as Double) }

        val purchaseData = hashMapOf(
            "userId" to uid,
            "business" to cart.business,
            "total" to total,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "lines" to lines
        )
        val ref = FirestoreProvider.db.collection("purchases").add(purchaseData).await()
        buyersDao.clearCart(uid)
        ref.id
    }
}

