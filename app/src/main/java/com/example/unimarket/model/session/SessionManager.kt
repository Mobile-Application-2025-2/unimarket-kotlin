package com.example.unimarket.model.session

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import kotlinx.coroutines.tasks.await

data class UserSession(
    val uid: String,
    val email: String,
    val type: String,        // "buyer" | "business" (desde custom claim o users/)
    val idToken: String,     // Firebase ID token (JWT). Se renueva.
    val lastRefreshedAt: Long = System.currentTimeMillis()
)

object SessionManager {
    @Volatile private var current: UserSession? = null

    val isLoggedIn: Boolean get() = current != null
    fun get(): UserSession? = current

    /** Para APIs externas que sí requieren Authorization: Bearer <token> */
    fun bearerOrNull(): String? = current?.idToken?.let { "Bearer $it" }

    fun setSession(s: UserSession) { current = s }
    fun clear() { current = null }

    /**
     * Refresca el ID token si es necesario y devuelve la sesión actualizada.
     * Útil antes de hacer llamadas a servicios externos que usen el JWT.
     */
    suspend fun ensureFreshIdToken(forceRefresh: Boolean = false): UserSession? {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: run { clear(); return null }

        val tokenResult: GetTokenResult = user.getIdToken(forceRefresh).await()
        val idToken = tokenResult.token ?: return current // si Firebase no devuelve token, no cambiamos
        val typeClaim = (tokenResult.claims["type"] as? String).orEmpty()

        // Conserva email/uid previos si ya existen
        val email = user.email.orEmpty()
        val uid = user.uid

        val updated = (current ?: UserSession(
            uid = uid,
            email = email,
            type = typeClaim, // puede venir vacío si aún no seteaste custom claims
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