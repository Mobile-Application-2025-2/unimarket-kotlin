package com.example.unimarket.model.domain.entity

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

data class Order(
    val id: String = "",
    val businessId: String = "",
    val userId: String = "",
    val products: List<String> = emptyList(), // ids de productos
    val units: List<Int> = emptyList(),       // cantidades en el mismo orden
    val paymentMethod: String = "",
    val date: Date? = null
) {

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "id"             to id,
        "business_id"    to businessId,
        "user_id"        to userId,
        "products"       to products,
        "units"          to units,
        "payment_method" to paymentMethod,
        "date"           to (date?.let { Timestamp(it) })
    )

    companion object {
        fun fromDocument(doc: DocumentSnapshot): Order {
            val ts = doc.getTimestamp("date")
            return Order(
                id            = doc.getString("id") ?: doc.id,
                businessId    = doc.getString("business_id") ?: "",
                userId        = doc.getString("user_id") ?: "",
                products      = doc.get("products") as? List<String> ?: emptyList(),
                units         = (doc.get("units") as? List<Long>)?.map { it.toInt() } ?: emptyList(),
                paymentMethod = doc.getString("payment_method") ?: "",
                date          = ts?.toDate()
            )
        }
    }
}