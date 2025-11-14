package com.example.unimarket.model.session

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import kotlinx.coroutines.tasks.await

data class UserSession(
    val uid: String,
    val email: String,
    val type: String,
    val idToken: String,
    val lastRefreshedAt: Long = System.currentTimeMillis()
)

object SessionManager {
    @Volatile private var current: UserSession? = null

    val isLoggedIn: Boolean get() = current != null
    fun get(): UserSession? = current

    fun bearerOrNull(): String? = current?.idToken?.let { "Bearer $it" }

    fun setSession(s: UserSession) { current = s }
    fun clear() { current = null }


    suspend fun ensureFreshIdToken(forceRefresh: Boolean = false): UserSession? {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: run { clear(); return null }

        val tokenResult: GetTokenResult = user.getIdToken(forceRefresh).await()
        val idToken = tokenResult.token ?: return current
        val typeClaim = (tokenResult.claims["type"] as? String).orEmpty()

        val email = user.email.orEmpty()
        val uid = user.uid

        val updated = (current ?: UserSession(
            uid = uid,
            email = email,
            type = typeClaim,
            idToken = idToken
        )).copy(
            uid = uid,
            email = email,
            type = if (typeClaim.isNotBlank()) typeClaim else (current?.type ?: ""),
            idToken = idToken,
            lastRefreshedAt = System.currentTimeMillis()
        )
        current = updated
        return updated
    }
}