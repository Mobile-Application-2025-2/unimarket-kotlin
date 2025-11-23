package com.example.unimarket.model.data.serviceAdapter

import com.example.unimarket.model.data.firebase.FirestoreProvider
import com.example.unimarket.model.domain.entity.Address
import com.example.unimarket.model.domain.entity.Buyer
import kotlinx.coroutines.tasks.await

class BuyersServiceAdapter {
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
    suspend fun appendAddress(uid: String, address: Address) {
        db.runTransaction { t ->
            val ref = col.document(uid)
            val snap = t.get(ref)
            val current = snap.toObject(Buyer::class.java) ?: Buyer()
            val updated = current.address.toMutableList().apply { add(address) }
            t.update(ref, mapOf("address" to updated))
        }.await()
    }
    suspend fun addToCart(uid: String, productId: String, unitPrice: Double, businessId: String) {
        val col = FirestoreProvider.buyers()
        val db = FirestoreProvider.db
        db.runTransaction { t ->
            val ref = col.document(uid)
            val snap = t.get(ref)
            val buyer = snap.toObject(Buyer::class.java) ?: Buyer()
            val cart = buyer.cart

            val sameBiz = cart.business.isBlank() || cart.business == businessId
            val baseItems = if (sameBiz) cart.products.toMutableMap() else mutableMapOf()
            val basePrice = if (sameBiz) cart.price else 0.0

            val newQty = (baseItems[productId] ?: 0) + 1
            baseItems[productId] = newQty

            val next = mapOf(
                "cart.items" to baseItems.toMap(),
                "cart.price" to (basePrice + unitPrice),
                "cart.business" to (if (cart.business.isBlank() || !sameBiz) businessId else cart.business)
            )
            t.update(ref, next)
        }.await()
    }
    suspend fun removeFromCart(uid: String, productId: String, unitPrice: Double) {
        val col = FirestoreProvider.buyers()
        val db = FirestoreProvider.db
        db.runTransaction { t ->
            val ref = col.document(uid)
            val snap = t.get(ref)
            val buyer = snap.toObject(Buyer::class.java) ?: Buyer()
            val items = buyer.cart.products.toMutableMap()
            val qty = (items[productId] ?: 0) - 1
            if (qty <= 0) items.remove(productId) else items[productId] = qty

            val newPrice = (buyer.cart.price - unitPrice).coerceAtLeast(0.0)
            val newBiz = if (items.isEmpty()) "" else buyer.cart.business

            t.update(ref, mapOf(
                "cart.items" to items.toMap(),
                "cart.price" to newPrice,
                "cart.business" to newBiz
            ))
        }.await()
    }

    suspend fun clearCart(uid: String) {
        FirestoreProvider.buyers().document(uid).update(
            mapOf(
                "cart.items" to emptyMap<String, Int>(),
                "cart.price" to 0.0,
                "cart.business" to ""
            )
        ).await()
    }
}
