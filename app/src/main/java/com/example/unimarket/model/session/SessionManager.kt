package com.example.unimarket.model.session

data class UserSession(
    val email: String,
    val type: String,       
    val accessToken: String 
)

object SessionManager {
    @Volatile private var current: UserSession? = null

    val isLoggedIn: Boolean get() = current != null
    fun get(): UserSession? = current

    fun bearerOrNull(): String? = current?.let { s ->
        if (s.accessToken.startsWith("Bearer ", ignoreCase = true)) s.accessToken
        else "Bearer ${s.accessToken}"
    }

    fun setSession(s: UserSession) { current = s }
    fun clear() { current = null }
}