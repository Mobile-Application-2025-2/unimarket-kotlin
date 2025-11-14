package com.example.unimarket.model.data.dao

import com.example.unimarket.model.data.firebase.FirestoreProvider
import com.example.unimarket.model.domain.entity.Comment
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class CommentsDao {
    private val db = FirestoreProvider.db
    private val col = FirestoreProvider.comments()
    private val products = FirestoreProvider.products()

    suspend fun listByProduct(productId: String): List<Comment> =
        col.whereEqualTo("product", productId).get().await().documents.mapNotNull { d ->
            d.toObject(Comment::class.java)?.also { it.id = d.id }
        }

    suspend fun create(c: Comment): String {
        val newId = db.runTransaction { t ->
            val ref = col.document()
            t.set(ref, c.copy().also { it.id = "" })
            t.update(products.document(c.product), "comments", FieldValue.arrayUnion(ref.id))
            ref.id
        }.await()
        return newId
    }
}