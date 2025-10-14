package com.example.unimarket.model.repository

import com.example.unimarket.model.api.LoginAuthApi
import com.example.unimarket.model.api.UsersApi
import com.example.unimarket.model.entity.SignInBody
import com.example.unimarket.model.entity.UserRow
import com.example.unimarket.model.entity.SignInResponse
import com.example.unimarket.model.session.SessionManager
import com.example.unimarket.model.session.UserSession

class AuthRepository(
    private val loginApi: LoginAuthApi,
    private val usersApi: UsersApi
) {
    suspend fun signInAndStoreSession(email: String, pass: String): Result<UserSession> {
        val res = loginApi.signIn(SignInBody(email, pass))
        if (!res.isSuccessful) {
            val code = res.code()
            val err  = res.errorBody()?.string().orEmpty()
            return Result.failure(IllegalStateException(err.ifBlank { "Credenciales inválidas ($code)" }))
        }

        val body: SignInResponse = res.body() ?: return Result.failure(IllegalStateException("Respuesta vacía"))
        val token = body.access_token ?: return Result.failure(IllegalStateException("Sin token en login"))
        val metaType = (body.user?.user_metadata?.get("type") as? String)

        val finalType = metaType ?: run {
            val r = usersApi.userByEmail(emailEq = "eq.${email.lowercase()}")
            if (r.isSuccessful) r.body()?.firstOrNull()?.type?.trim()?.lowercase() else null
        }

        val normalizedType = finalType?.trim()?.lowercase() ?: return Result.failure(IllegalStateException("No se encontró el tipo de usuario"))
        val session = UserSession(email = email.lowercase(), type = normalizedType, accessToken = token)

        SessionManager.setSession(session)
        return Result.success(session)
    }

    fun signOut() {
        SessionManager.clear()
    }
}