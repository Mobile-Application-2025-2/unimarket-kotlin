package com.example.unimarket.model.data.serviceAdapter

import com.example.unimarket.model.domain.entity.Order
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class OrderServiceAdapter(
    private val db: FirebaseFirestore
) {

    private val ordersCollection = db.collection("orders")

    /**
     * Crea o sobrescribe una orden.
     * Si order.id está vacío, genera un id de documento nuevo y lo guarda también en el campo "id".
     */
    suspend fun upsertOrder(order: Order): String {
        val docRef = if (order.id.isBlank()) {
            ordersCollection.document()
        } else {
            ordersCollection.document(order.id)
        }

        val data = order.copy(id = if (order.id.isBlank()) docRef.id else order.id)
            .toFirestoreMap()

        docRef.set(data).await()
        return docRef.id
    }

    suspend fun deleteOrder(orderId: String) {
        ordersCollection.document(orderId).delete().await()
    }

    suspend fun getOrder(orderId: String): Order? {
        val snapshot = ordersCollection.document(orderId).get().await()
        return if (snapshot.exists()) Order.fromDocument(snapshot) else null
    }

    suspend fun getOrdersByUser(userId: String): List<Order> {
        val querySnapshot = ordersCollection
            .whereEqualTo("user_id", userId)
            .get()
            .await()

        return querySnapshot.documents.map { Order.fromDocument(it) }
    }

    suspend fun getOrdersByBusiness(businessId: String): List<Order> {
        val querySnapshot = ordersCollection
            .whereEqualTo("business_id", businessId)
            .get()
            .await()

        return querySnapshot.documents.map { Order.fromDocument(it) }
    }
}