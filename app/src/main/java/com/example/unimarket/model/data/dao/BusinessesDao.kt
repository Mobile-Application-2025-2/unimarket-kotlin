package com.example.unimarket.model.data.dao

import com.example.unimarket.model.data.firebase.FirestoreProvider
import com.example.unimarket.model.domain.entity.Address
import com.example.unimarket.model.domain.entity.Business
import kotlinx.coroutines.tasks.await

class BusinessesDao {
    private val col = FirestoreProvider.businesses()

    suspend fun getById(uid: String): Business? =
        col.document(uid).get().await().toObject(Business::class.java)?.also { it.id = uid }

    suspend fun create(uid: String, business: Business) {
        col.document(uid).set(business.copy().also { it.id = "" }).await()
    }

    suspend fun update(uid: String, partial: Map<String, Any?>) {
        col.document(uid).update(partial).await()
    }

    suspend fun setAddress(uid: String, address: Address) {
        col.document(uid).update(mapOf("address" to address)).await()
    }
}
