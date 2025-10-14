package com.example.unimarket.controller.auth

import com.example.unimarket.model.repository.AuthRepository
import com.example.unimarket.model.entity.SignInResponse

interface SignInViewPort {
    fun setSubmitting(submitting: Boolean)
    fun showError(message: String)
    fun navigateToBuyer()
    fun navigateToCourier()
}

class SignInController(private val view: SignInViewPort, private val repo: AuthRepository) {
    suspend fun onSignInClicked(email: String, pass: String) {
        view.setSubmitting(true)
        try {
            val res: SignInResponse = repo.login(email, pass)
            val token = res.access_token
                ?: throw IllegalStateException("No se recibió token del login.")

            val metaRole = (res.user?.user_metadata?.get("type") as? String)?.trim()?.lowercase()
            val role = metaRole ?: repo.userType(token, email)

            when (role) {
                "buyer" -> view.navigateToBuyer()
                "deliver", "delivery", "courier" -> view.navigateToCourier()
                else -> view.showError("No se encontró el tipo de usuario.")
            }
        } catch (t: Throwable) {
            view.showError(t.message ?: "Error al iniciar sesión")
        } finally {
            view.setSubmitting(false)
        }
    }
}