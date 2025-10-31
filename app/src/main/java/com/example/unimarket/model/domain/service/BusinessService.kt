package com.example.unimarket.model.domain.service

import com.example.unimarket.model.domain.entity.Business
import com.example.unimarket.model.domain.entity.Category
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BusinessService(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val businesses = db.collection("businesses")
    private val categoriesCol = db.collection("categories")

    /* =============== CRUD =============== */

    suspend fun getBusiness(businessId: String): Result<Business> = runCatching {
        val snap = businesses.document(req(businessId, "businessId")).get().await()
        if (!snap.exists()) error("Business not found")
        snap.toObject(Business::class.java)!!.copy(id = snap.id)
    }

    suspend fun updateBusiness(
        businessId: String,
        name: String? = null,
        logoUrl: String? = null,
        address: com.example.unimarket.model.domain.entity.Address? = null,
        categories: List<Category>? = null
    ): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to Timestamp.now()
        )
        if (name != null)     updates["name"] = name.trim()
        if (logoUrl != null)  updates["logoUrl"] = logoUrl
        if (address != null)  updates["address"] = address
        if (categories != null) updates["categories"] = categories

        // Si no hay nada para actualizar, no llames a Firestore
        val hasRealUpdates = updates.any { it.key != "updatedAt" }
        if (!hasRealUpdates) return@runCatching

        businesses.document(req(businessId, "businessId")).update(updates as Map<String, Any>).await()
    }

    /* ============ Categorías (lista de Category) ============ */

    /**
     * Agrega una Category completa si NO existe ya por id.
     * Estrategia robusta: leer -> modificar en memoria -> set/update.
     */
    suspend fun addCategoryToBusiness(businessId: String, category: Category): Result<Unit> = runCatching {
        val ref = businesses.document(req(businessId, "businessId"))
        val snap = ref.get().await()
        if (!snap.exists()) error("Business not found")

        val current = snap.toObject(Business::class.java)!!.copy(id = snap.id)
        if (current.categories.any { it.id == category.id }) {
            // ya está, solo toca updatedAt
            ref.update("updatedAt", Timestamp.now()).await()
            return@runCatching
        }

        val updated = current.copy(
            categories = current.categories + category
        )
        ref.set(updated).await()
    }

    /**
     * Lee /categories/{categoryId} y agrega esa Category al business.
     */
    suspend fun addCategoryById(businessId: String, categoryId: String): Result<Unit> = runCatching {
        val catDoc = categoriesCol.document(req(categoryId, "categoryId")).get().await()
        if (!catDoc.exists()) error("Category not found")
        val cat = catDoc.toObject(Category::class.java)!!.copy(id = catDoc.id)
        addCategoryToBusiness(businessId, cat).getOrThrow()
    }

    /**
     * Elimina Category por id (independiente de igualdad exacta del objeto).
     */
    suspend fun removeCategoryFromBusiness(businessId: String, categoryId: String): Result<Unit> = runCatching {
        val ref = businesses.document(req(businessId, "businessId"))
        val snap = ref.get().await()
        if (!snap.exists()) error("Business not found")

        val current = snap.toObject(Business::class.java)!!.copy(id = snap.id)
        val newList = current.categories.filterNot { it.id == req(categoryId, "categoryId") }

        // si no cambió, solo actualiza updatedAt
        if (newList.size == current.categories.size) {
            ref.update("updatedAt", Timestamp.now()).await()
            return@runCatching
        }

        ref.update(
            mapOf(
                "categories" to newList,
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    /* =============== helpers =============== */
    private fun req(v: String?, label: String): String {
        val x = v?.trim().orEmpty()
        require(x.isNotEmpty()) { "$label is empty" }
        return x
    }
}