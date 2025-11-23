package com.example.unimarket.model.data.serviceAdapter

import com.example.unimarket.model.data.firebase.FirestoreProvider
import com.example.unimarket.model.domain.entity.Category
import kotlinx.coroutines.tasks.await

class CategoriesServiceAdapter {
    private val col = FirestoreProvider.categories()

    suspend fun listAll(): List<Category> =
        col.get().await().documents.mapNotNull { doc ->
            doc.toObject(Category::class.java)?.also { it.id = doc.id }
        }

    suspend fun getById(id: String): Category? =
        col.document(id).get().await().toObject(Category::class.java)?.also { it.id = id }

    suspend fun create(category: Category): String {
        val ref = if (category.id.isBlank()) col.document() else col.document(category.id)
        ref.set(category.copy().also { it.id = "" }).await()
        return ref.id
    }

    suspend fun update(id: String, partial: Map<String, Any?>) {
        col.document(id).update(partial).await()
    }
}