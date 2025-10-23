package com.example.unimarket.model.domain.service

import com.example.unimarket.model.data.dao.BuyersDao
import com.example.unimarket.model.data.dao.ProductsDao
import com.example.unimarket.model.data.firebase.FirebaseAuthProvider
import com.example.unimarket.model.domain.validation.Validators.parsePriceToDouble
import com.example.unimarket.model.domain.validation.Validators.requireNotBlank

class CartService(
    private val buyersDao: BuyersDao = BuyersDao(),
    private val productsDao: ProductsDao = ProductsDao()
) {
    /** Agrega un productId al carrito del buyer autenticado y actualiza price acumulado. */
    suspend fun addProduct(productId: String): Result<Unit> = runCatching {
        requireNotBlank(productId, "productId")
        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("Not authenticated")
        val product = productsDao.getById(productId) ?: error("Product not found")
        val price = parsePriceToDouble(product.price)
        buyersDao.addToCart(uid, productId, price)
    }

    /** Remueve un productId y descuenta su precio (no cae por debajo de 0). */
    suspend fun removeProduct(productId: String): Result<Unit> = runCatching {
        requireNotBlank(productId, "productId")
        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("Not authenticated")
        val product = productsDao.getById(productId) ?: error("Product not found")
        val price = parsePriceToDouble(product.price)
        buyersDao.removeFromCart(uid, productId, price)
    }

    suspend fun clear(): Result<Unit> = runCatching {
        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("Not authenticated")
        buyersDao.clearCart(uid)
    }
}
