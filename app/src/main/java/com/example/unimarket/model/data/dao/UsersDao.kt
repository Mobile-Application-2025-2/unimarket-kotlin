package com.example.unimarket.model.data.dao

import com.example.unimarket.model.data.firebase.FirestoreProvider
import com.example.unimarket.model.domain.entity.User
import kotlinx.coroutines.tasks.await

class UsersDao {
    private val col = FirestoreProvider.users()

    suspend fun getById(uid: String): User? =
        col.document(uid).get().await().toObject(User::class.java)?.also { it.id = uid }

    suspend fun create(uid: String, user: User) {
        col.document(uid).set(user.copy().also { it.id = "" }).await()
    }

    suspend fun update(uid: String, partial: Map<String, Any?>) {
        col.document(uid).update(partial).await()
    }

    suspend fun delete(uid: String) {
        col.document(uid).delete().await()
    }
}