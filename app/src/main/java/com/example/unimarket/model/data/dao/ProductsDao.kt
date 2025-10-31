package com.example.unimarket.model.data.dao

import com.example.unimarket.model.data.firebase.FirestoreProvider
import com.example.unimarket.model.domain.entity.Product
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await

class ProductsDao {
    private val col = FirestoreProvider.products()

    suspend fun getById(id: String): Product? =
        col.document(id).get().await().toObject(Product::class.java)?.also { it.id = id }

    suspend fun listByCategory(categoryId: String): List<Product> =
        col.whereEqualTo("category", categoryId).get().await().documents.mapNotNull { d ->
            d.toObject(Product::class.java)?.also { it.id = d.id }
        }

    suspend fun listByBusiness(businessUid: String): List<Product> {
        val uid = businessUid.trim()
        if (uid.isEmpty()) return emptyList()

        val seen = mutableSetOf<String>()
        val products = mutableListOf<Product>()

        collectFromDocuments(
            col.whereEqualTo("business", uid).get().await().documents,
            uid,
            seen,
            products
        )
        if (products.isEmpty()) {
            collectFromDocuments(
                col.whereEqualTo("businessId", uid).get().await().documents,
                uid,
                seen,
                products
            )
            collectFromDocuments(
                col.whereEqualTo("businessUid", uid).get().await().documents,
                uid,
                seen,
                products
            )
        }

        if (products.isEmpty()) {
            val nested = FirestoreProvider.businesses()
                .document(uid)
                .collection(FirestoreProvider.COL_PRODUCTS)
            collectFromDocuments(nested.get().await().documents, uid, seen, products)
        }

        if (products.isEmpty()) {
            val businessDoc = FirestoreProvider.businesses().document(uid).get().await()
            val ids = (businessDoc.get("products") as? List<*>)
                ?.mapNotNull { it as? String }
                .orEmpty()
            ids.forEach { productId ->
                if (!seen.add(productId)) return@forEach
                val snapshot = col.document(productId).get().await()
                val base = snapshot.toObject(Product::class.java) ?: return@forEach
                val product = if (base.business.isBlank()) base.copy(business = uid) else base
                product.id = snapshot.id
                products += product
            }
        }

        return products
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

    private fun collectFromDocuments(
        documents: List<DocumentSnapshot>,
        businessUid: String,
        seen: MutableSet<String>,
        into: MutableList<Product>
    ) {
        documents.forEach { document ->
            if (!seen.add(document.id)) return@forEach
            val base = document.toObject(Product::class.java) ?: return@forEach
            val product = if (base.business.isBlank()) base.copy(business = businessUid) else base
            product.id = document.id
            into += product
        }
    }
}
