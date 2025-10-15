package com.example.unimarket.controller.auth

import com.example.unimarket.model.repository.AuthRepository
import com.example.unimarket.model.session.SessionManager
import com.example.unimarket.model.session.UserSession
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

    fun onSignInClicked(email: String, password: String) {
        view.setSubmitting(true)
        uiScope.launch {
            try {
                val res = repo.signIn(email, password)
                val token = res.access_token ?: throw IllegalStateException("No se recibió token del login.")

                val metaType = (res.user?.user_metadata?.get("type") as? String)?.trim()?.lowercase()
                val role = metaType ?: repo.userType(email)

                if (role.isNullOrBlank()) {
                    view.showError("No se encontró el tipo de usuario.")
                    return@launch
                }

                SessionManager.setSession(
                    UserSession(
                        email = email,
                        type = role,
                        accessToken = token
                    )
                )

                when (role) {
                    "buyer" -> view.navigateToBuyer()
                    "deliver", "delivery", "courier" -> view.navigateToCourier()
                    else -> view.showError("Tipo de usuario desconocido: $role")
                }
            } catch (t: Throwable) {
                view.showError(t.message ?: "Error al iniciar sesión")
            } finally {
                view.setSubmitting(false)
            }
        }
    }
}