package com.example.unimarket.controller.auth

import com.example.unimarket.model.domain.service.AuthService
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
    private val auth: AuthService = AuthService(),
    private val uiScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    fun onSignInClicked(email: String, password: String) {
        view.setSubmitting(true)
        uiScope.launch {
            try {
                val res = auth.signIn(email, password)
                res.onSuccess { user ->
                    val session = SessionManager.get()
                    val role = (session?.type ?: user.type).trim().lowercase()

                    when (role) {
                        "buyer" -> view.navigateToBuyer()
                        "deliver", "delivery", "courier", "business" -> view.navigateToCourier()
                        else -> view.showError("Tipo de usuario desconocido: $role")
                    }
                }.onFailure { e ->
                    view.showError(e.message ?: "Error al iniciar sesión")
                }
            } catch (t: Throwable) {
                view.showError(t.message ?: "Error al iniciar sesión")
            } finally {
                view.setSubmitting(false)
            }
        }
    }
}
