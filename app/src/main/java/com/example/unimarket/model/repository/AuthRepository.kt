package com.example.unimarket.model.repository
/*
import com.example.unimarket.model.api.LoginAuthApi
import com.example.unimarket.model.api.SignUpAuthApi
import com.example.unimarket.model.api.UsersApi
import com.example.unimarket.model.entity.SignInBody
import com.example.unimarket.model.entity.SignInResponse
import com.example.unimarket.model.entity.SignUpBody
import com.example.unimarket.model.session.SessionManager
import retrofit2.Response

class AuthRepository(
    private val signUpApi: SignUpAuthApi? = null,
    private val loginAuthApi: LoginAuthApi? = null,
    private val usersApi: UsersApi? = null
) {

    suspend fun signUp(body: SignUpBody): SignInResponse {
        val api = requireNotNull(signUpApi) { "SignUpAuthApi no configurado en AuthRepository" }
        val res = api.signUp(body)
        if (!res.isSuccessful) {
            val msg = res.errorBody()?.string().orEmpty().ifBlank { "Sign up falló (HTTP ${res.code()})" }
            throw IllegalStateException(msg)
        }
        return res.body() ?: throw IllegalStateException("Respuesta vacía en sign up")
    }

    suspend fun signIn(email: String, password: String): SignInResponse {
        val api = requireNotNull(loginAuthApi) { "LoginAuthApi no configurado en AuthRepository" }
        val res = api.signIn(SignInBody(email = email, password = password))
        if (!res.isSuccessful) {
            val msg = res.errorBody()?.string().orEmpty().ifBlank { "Login falló (HTTP ${res.code()})" }
            throw IllegalStateException(msg)
        }
        return res.body() ?: throw IllegalStateException("Respuesta vacía en login")
    }

    suspend fun userType(email: String): String? {
        val api = requireNotNull(usersApi) { "UsersApi no configurado en AuthRepository" }
        val bearer = SessionManager.bearerOrNull() ?: return null
        val r = api.userByEmail(emailEq = "eq.$email", bearer = bearer)
        if (!r.isSuccessful) return null
        return r.body()?.firstOrNull()?.type?.trim()?.lowercase()
    }
}*/