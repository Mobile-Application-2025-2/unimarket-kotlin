package com.example.unimarket.model.data.serviceAdapter

import com.example.unimarket.model.data.firebase.FirestoreProvider
import com.example.unimarket.model.domain.entity.Product
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await

class ProductsServiceAdapter {
    private val col = FirestoreProvider.products()

    suspend fun getById(id: String): Product? =
        col.document(id).get().await().toProduct()
    suspend fun listByCategory(categoryId: String): List<Product> =
        col.whereEqualTo("category", categoryId).get().await().documents.mapNotNull { it.toProduct() }


    suspend fun listByBusiness(businessUid: String): List<Product> =
        col.whereEqualTo("business", businessUid).get().await().documents.mapNotNull { it.toProduct() }

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

    private fun DocumentSnapshot.toProduct(): Product? {
        if (!exists()) return null
        val name = getString("name").orEmpty()
        val description = getString("description").orEmpty()
        val category = getString("category").orEmpty()
        val business = getString("business").orEmpty()
        val image = getString("image").orEmpty()
        val price = get("price").toDoubleOrZero()
        val rating = get("rating").toDoubleOrZero(default = 4.0)
        val comments = when (val raw = get("comments")) {
            is List<*> -> raw.mapNotNull { it?.toString() }
            else -> emptyList()
        }

        return Product(
            name = name,
            price = price,
            description = description,
            category = category,
            business = business,
            rating = rating,
            comments = comments,
            image = image
        ).also { it.id = id }
    }

    private fun Any?.toDoubleOrZero(default: Double = 0.0): Double = when (this) {
        is Number -> toDouble()
        is String -> this.cleanNumericString().toDoubleOrNull() ?: default
        else -> default
    }

    private fun String.cleanNumericString(): String {
        val trimmed = trim()
        if (trimmed.isEmpty()) return ""
        val normalized = trimmed.replace("[^0-9,.-]".toRegex(), "")
        val commaCount = normalized.count { it == ',' }
        val dotCount = normalized.count { it == '.' }
        return when {
            commaCount == 1 && dotCount == 0 -> normalized.replace(',', '.')
            commaCount > 1 && dotCount == 0 -> normalized.replace(",", "")
            dotCount > 1 && commaCount == 0 -> normalized.replace(".", "")
            commaCount == 1 && dotCount == 1 -> {
                if (normalized.lastIndexOf(',') > normalized.lastIndexOf('.')) {
                    normalized.replace(".", "").replace(',', '.')
                } else {
                    normalized.replace(",", "")
                }
            }
            else -> normalized
        }
    }
}