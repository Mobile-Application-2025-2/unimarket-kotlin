package com.example.unimarket.model.data.dao

import com.example.unimarket.model.data.firebase.FirestoreProvider
import com.example.unimarket.model.domain.entity.Address
import com.example.unimarket.model.domain.entity.Buyer
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class BuyersDao {
    private val db = FirestoreProvider.db
    private val col = FirestoreProvider.buyers()

    suspend fun getById(uid: String): Buyer? =
        col.document(uid).get().await().toObject(Buyer::class.java)?.also { it.id = uid }

    suspend fun create(uid: String, buyer: Buyer) {
        col.document(uid).set(buyer.copy().also { it.id = "" }).await()
    }

    suspend fun update(uid: String, partial: Map<String, Any?>) {
        col.document(uid).update(partial).await()
    }

    /** Agrega una dirección a la lista address[] (array de maps). */
    suspend fun appendAddress(uid: String, address: Address) {
        db.runTransaction { t ->
            val ref = col.document(uid)
            val snap = t.get(ref)
            val current = snap.toObject(Buyer::class.java) ?: Buyer()
            val updated = current.address.toMutableList().apply { add(address) }
            t.update(ref, mapOf("address" to updated))
        }.await()
    }

    /** Agrega un productId al carrito y acumula el precio. */
    suspend fun addToCart(uid: String, productId: String, productPrice: Double) {
        db.runTransaction { t ->
            val ref = col.document(uid)
            val snap = t.get(ref)
            val buyer = snap.toObject(Buyer::class.java) ?: Buyer()
            val newProducts = buyer.cart.products.toMutableList().apply { add(productId) }
            val newPrice = (buyer.cart.price) + productPrice
            t.update(ref, mapOf("cart.products" to newProducts, "cart.price" to newPrice))
        }.await()
    }

    /** Remueve un productId del carrito y descuenta su precio. */
    suspend fun removeFromCart(uid: String, productId: String, productPrice: Double) {
        db.runTransaction { t ->
            val ref = col.document(uid)
            val snap = t.get(ref)
            val buyer = snap.toObject(Buyer::class.java) ?: Buyer()
            val newProducts = buyer.cart.products.toMutableList().apply { remove(productId) }
            val newPrice = (buyer.cart.price - productPrice).coerceAtLeast(0.0)
            t.update(ref, mapOf("cart.products" to newProducts, "cart.price" to newPrice))
        }.await()
    }

    suspend fun clearCart(uid: String) {
        col.document(uid).update(mapOf("cart.products" to emptyList<String>(), "cart.price" to 0.0)).await()
    }
}
