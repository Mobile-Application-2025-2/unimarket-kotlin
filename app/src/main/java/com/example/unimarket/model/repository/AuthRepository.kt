package com.example.unimarket.model.repository

import com.example.unimarket.model.entity.SignInBody
import com.example.unimarket.model.entity.SignInResponse
import com.example.unimarket.model.entity.SignUpBody
import com.example.unimarket.model.api.SignUpAuthApi
import com.example.unimarket.model.api.LoginAuthApi
import com.example.unimarket.model.api.UsersApi

class AuthRepository(private val createApi: SignUpAuthApi?, private val loginApi: LoginAuthApi?, private val usersApi: UsersApi?) {

    constructor(createApi: SignUpAuthApi) : this(createApi, null, null)
    constructor(loginApi: LoginAuthApi, usersApi: UsersApi) : this(null, loginApi, usersApi)

    suspend fun signUp(body: SignUpBody) {
        val api: SignUpAuthApi = requireNotNull(createApi) { "SignUpAuthApi no configurado (usa el ctor de signup)" }
        val res = api.signUp(body)
        if (!res.isSuccessful) {
            val msg = res.errorBody()?.string().orEmpty()
            throw IllegalStateException(msg.ifBlank { "Sign-up falló (HTTP ${res.code()})" })
        }
    }

    suspend fun login(email: String, password: String): SignInResponse {
        val api = requireNotNull(loginApi) { "LoginAuthApi no configurado" }
        val res = api.signIn(SignInBody(email, password))
        if (!res.isSuccessful) {
            val msg = res.errorBody()?.string().orEmpty()
            throw IllegalStateException(msg.ifBlank { "Credenciales inválidas (HTTP ${res.code()})" })
        }
        return res.body() ?: throw IllegalStateException("Respuesta vacía del login.")
    }

    suspend fun userType(accessToken: String, email: String): String? {
        val api: UsersApi = requireNotNull(usersApi) { "UsersApi no configurado (usa el ctor de login)" }
        val r = usersApi.userByEmail(emailEq = "eq.$email", bearer = "Bearer $accessToken")
        if (!r.isSuccessful) return null
        return r.body()?.firstOrNull()?.type?.trim()?.lowercase()
    }
}