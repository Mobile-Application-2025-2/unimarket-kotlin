package com.example.unimarket.model.domain.service

import android.util.Log
import com.example.unimarket.model.data.local.dao.BusinessLocalDao
import com.example.unimarket.model.data.local.entity.BusinessLocalEntity
import com.example.unimarket.model.domain.entity.Address
import com.example.unimarket.model.domain.entity.Business
import com.example.unimarket.model.domain.entity.Category
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BusinessService(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val businesses = db.collection("businesses")
    private val categoriesCol = db.collection("categories")

    suspend fun getBusiness(businessId: String): Result<Business> = runCatching {
        val snap = businesses.document(req(businessId, "businessId")).get().await()
        if (!snap.exists()) error("Business not found")
        snap.toBusinessSafe() ?: error("Malformed business document: ${snap.id}")
    }

    suspend fun getAllBusinesses(): Result<List<Business>> = runCatching {
        val snaps = businesses.get().await()
        snaps.documents.mapNotNull { d ->
            try {
                d.toBusinessSafe()
            } catch (t: Throwable) {
                Log.e("BusinessService", "toBusinessSafe failed for ${d.id}", t)
                null
            }
        }
    }

    suspend fun getAllAndPersist(localDao: BusinessLocalDao?): Result<List<Business>> = runCatching {
        val items = getAllBusinesses().getOrThrow()

        localDao?.let { dao ->
            val entities = items.map { b ->
                BusinessLocalEntity(
                    id = b.id,
                    name = b.name,
                    logoUrl = b.logo.ifBlank { null },
                    categoryNames = b.categories.joinToString(",") { it.name }
                )
            }
            dao.clear()
            dao.upsertAll(entities)
        }

        items
    }

    suspend fun updateBusiness(
        businessId: String,
        name: String? = null,
        logoUrl: String? = null,
        address: Address? = null,
        categories: List<Category>? = null
    ): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to Timestamp.now()
        )
        if (name != null)        updates["name"] = name.trim()
        if (logoUrl != null)     updates["logoUrl"] = logoUrl
        if (address != null)     updates["address"] = address
        if (categories != null)  updates["categories"] = categories

        val hasRealUpdates = updates.any { it.key != "updatedAt" }
        if (!hasRealUpdates) return@runCatching

        businesses.document(req(businessId, "businessId"))
            .update(updates as Map<String, Any>)
            .await()
    }

    suspend fun updateRating(
        businessId: String,
        rating: Double,
        amountRatings: Long
    ): Result<Unit> = runCatching {
        val updates = mapOf(
            "rating" to rating,
            "amountRatings" to amountRatings,
            "updatedAt" to Timestamp.now()
        )

        businesses.document(req(businessId, "businessId"))
            .update(updates)
            .await()
    }

    suspend fun addCategoryToBusiness(businessId: String, category: Category): Result<Unit> = runCatching {
        val ref = businesses.document(req(businessId, "businessId"))
        val snap = ref.get().await()
        if (!snap.exists()) error("Business not found")

        val current = snap.toBusinessSafe()!!.copy(id = snap.id)
        if (current.categories.any { it.id == category.id && it.name == category.name }) {
            ref.update("updatedAt", Timestamp.now()).await()
            return@runCatching
        }

        val newList = current.categories + category
        ref.update(
            mapOf(
                "categories" to newList,
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    suspend fun addCategoryById(businessId: String, categoryId: String): Result<Unit> = runCatching {
        val catDoc = categoriesCol.document(req(categoryId, "categoryId")).get().await()
        if (!catDoc.exists()) error("Category not found")
        val cat = catDoc.toObject(Category::class.java)!!.copy(id = catDoc.id)
        addCategoryToBusiness(businessId, cat).getOrThrow()
    }

    suspend fun removeCategoryFromBusiness(businessId: String, categoryId: String): Result<Unit> = runCatching {
        val ref = businesses.document(req(businessId, "businessId"))
        val snap = ref.get().await()
        if (!snap.exists()) error("Business not found")

        val current = snap.toBusinessSafe()!!.copy(id = snap.id)
        val newList = current.categories.filterNot { it.id == req(categoryId, "categoryId") }

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


    private fun req(v: String?, label: String): String {
        val x = v?.trim().orEmpty()
        require(x.isNotEmpty()) { "$label is empty" }
        return x
    }
}


private fun Any?.asAddress(): Address {
    val m = this as? Map<*, *> ?: return Address()
    val direccion = (m["direccion"] as? String)?.trim().orEmpty()
    return Address(direccion = direccion)
}
private fun Any?.asCategoryListFlexible(): List<Category> {
    val raw = this as? List<*> ?: return emptyList()
    return raw.mapNotNull { item ->
        when (item) {
            is String -> {
                val name = item.trim()
                if (name.isEmpty()) null else Category(name = name)
            }
            is Map<*, *> -> {
                val id   = (item["id"] as? String)?.trim().orEmpty()
                val name = (item["name"] as? String)?.trim().orEmpty()
                if (id.isEmpty() && name.isEmpty()) null else Category(id = id, name = name)
            }
            else -> null
        }
    }
}

private fun Any?.asStringList(): List<String> =
    (this as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

private fun Any?.asDouble(): Double = when (val v = this) {
    is Number -> v.toDouble()
    is String -> v.toDoubleOrNull() ?: 0.0
    else      -> 0.0
}

private fun Any?.asLong(): Long = when (val v = this) {
    is Number -> v.toLong()
    is String -> v.toLongOrNull() ?: 0L
    else      -> 0L
}

private fun DocumentSnapshot.toBusinessSafe(): Business? {
    val data = data ?: return null

    val name   = (data["name"] as? String)?.trim().orEmpty()
    val rating = data["rating"].asDouble()

    val amountRatings = (data["amountRatings"] ?: data["amount_ratings"]).asLong()

    val products = data["products"].asStringList()
    val logo     = (data["logo"] as? String)?.trim()
        ?: (data["logoUrl"] as? String)?.trim().orEmpty()

    val addressAny    = data["address"]
    val categoriesAny = data["categories"]

    val address    = addressAny.asAddress()
    val categories = categoriesAny.asCategoryListFlexible()

    return Business(
        id            = id,
        name          = name,
        address       = address,
        rating        = rating,
        amountRatings = amountRatings,
        products      = products,
        logo          = logo,
        categories    = categories
    )
}