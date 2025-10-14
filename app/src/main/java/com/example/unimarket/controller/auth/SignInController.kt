package com.example.unimarket.controller.auth

import com.example.unimarket.model.repository.AuthRepository
import com.example.unimarket.model.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface SignInViewPort {
    fun setSubmitting(submitting: Boolean)
    fun showError(message: String)
    fun navigateToBuyer()
    fun navigateToCourier()
}

class SignInController(
    private val view: SignInViewPort,
    private val repo: AuthRepository,
    private val uiScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    fun onSignIn(email: String, password: String) {
        view.setSubmitting(true)
        uiScope.launch {
            try {
                val result = repo.signInAndStoreSession(email.trim().lowercase(), password)
                result.onSuccess { session ->
                    when (session.type.trim().lowercase()) {
                        "buyer" -> view.navigateToBuyer()
                        "deliver", "delivery", "courier" -> view.navigateToCourier()
                        else -> view.showError("Tipo de usuario desconocido: ${session.type}")
                    }
                }.onFailure { e ->
                    view.showError(e.message ?: "Error de inicio de sesión")
                }
            } catch (t: Throwable) {
                view.showError(t.message ?: "Error inesperado al iniciar sesión")
            } finally {
                view.setSubmitting(false)
            }
        }
    }
}