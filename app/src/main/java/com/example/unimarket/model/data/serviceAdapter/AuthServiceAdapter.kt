package com.example.unimarket.model.data.serviceAdapter

import com.example.unimarket.model.data.firebase.FirebaseAuthProvider
import com.example.unimarket.model.data.firebase.FirestoreProvider
import com.example.unimarket.model.domain.entity.User
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.tasks.await

class AuthServiceAdapter {
    private val auth = FirebaseAuthProvider.auth
    private val usersCol = FirestoreProvider.users()

    suspend fun signUp(user: User, password: String): Result<User> = runCatching {
        val res: AuthResult = auth.createUserWithEmailAndPassword(user.email, password).await()
        val uid = res.user?.uid ?: error("No UID from FirebaseAuth")

        res.user?.sendEmailVerification()?.await()

        val data = mapOf(
            "email" to user.email,
            "name" to user.name,
            "idType" to user.idType,
            "idNumber" to user.idNumber,
            "type" to user.type.lowercase(),
            "onboardingCompleted" to (user.onboardingCompleted),
            "studentCode" to user.studentCode
        )
        usersCol.document(uid).set(data).await()
        user.copy(id = uid)
    }

    suspend fun signIn(email: String, password: String): Result<User> = runCatching {
        val res = auth.signInWithEmailAndPassword(email, password).await()
        val uid = res.user?.uid ?: error("No UID from FirebaseAuth")
        val snap = usersCol.document(uid).get().await()
        val model = snap.toObject(User::class.java) ?: error("User profile missing")
        model.id = uid
        model
    }

    fun signOut() = auth.signOut()

    suspend fun currentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        val snap = usersCol.document(uid).get().await()
        val model = snap.toObject(User::class.java) ?: return null
        model.id = uid
        return model
    }
}