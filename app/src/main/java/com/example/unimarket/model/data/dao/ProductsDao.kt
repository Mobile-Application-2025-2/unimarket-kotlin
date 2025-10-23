package com.example.unimarket.model.data.dao

import com.example.unimarket.model.data.firebase.FirestoreProvider
import com.example.unimarket.model.domain.entity.Product
import kotlinx.coroutines.tasks.await

class ProductsDao {
    private val col = FirestoreProvider.products()

    suspend fun getById(id: String): Product? =
        col.document(id).get().await().toObject(Product::class.java)?.also { it.id = id }

    suspend fun listByCategory(categoryId: String): List<Product> =
        col.whereEqualTo("category", categoryId).get().await().documents.mapNotNull { d ->
            d.toObject(Product::class.java)?.also { it.id = d.id }
        }

    suspend fun listByBusiness(businessUid: String): List<Product> =
        col.whereEqualTo("business", businessUid).get().await().documents.mapNotNull { d ->
            d.toObject(Product::class.java)?.also { it.id = d.id }
        }

    suspend fun create(p: Product): String {
        val ref = col.document()
        ref.set(p.copy().also { it.id = "" }).await()
        return ref.id
    }

    suspend fun update(id: String, newData: Product) {
        col.document(id).set(newData.copy().also { it.id = "" }).await()
    }

    suspend fun delete(id: String) {
        col.document(id).delete().await()
    }

    suspend fun updatePartial(id: String, partial: Map<String, Any?>) {
        col.document(id).update(partial).await()
    }
}
