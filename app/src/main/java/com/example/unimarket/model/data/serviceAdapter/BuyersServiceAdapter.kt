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
}
