package com.example.unimarket.controller.auth

import com.example.unimarket.model.entity.SignUpBody
import com.example.unimarket.model.repository.AuthRepository
import com.example.unimarket.model.session.SessionManager
import com.example.unimarket.model.session.UserSession

interface CreateAccountViewPort {
    fun setSubmitting(submitting: Boolean)
    fun toast(message: String)
    fun navigateToStudentCode()
}

class CreateAccountController(
    private val view: CreateAccountViewPort,
    private val repo: AuthRepository
) {
    private val DEFAULT_USER_TYPE = "buyer"

    suspend fun onSignUpClicked(name: String, email: String, pass: String, accepted: Boolean) {
        if (!accepted) {
            view.toast("Debes aceptar la política de privacidad")
            return
        }
        view.setSubmitting(true)
        try {
            val body = SignUpBody(
                email = email,
                password = pass,
                data = mapOf(
                    "name" to name,
                    "type" to DEFAULT_USER_TYPE,
                    "id_type" to "id",
                    "id_number" to ""
                )
            )
            val res = repo.signUp(body)

            val token = res.access_token ?: throw IllegalStateException("No se recibió token en el registro.")
            val metaType = (res.user?.user_metadata?.get("type") as? String)?.trim()?.lowercase()
            val role = metaType ?: DEFAULT_USER_TYPE

            SessionManager.setSession(
                UserSession(
                    email = email,
                    type = role,
                    accessToken = token
                )
            )

            view.toast("Cuenta creada")
            view.navigateToStudentCode()
        } catch (t: Throwable) {
            view.toast("Error: ${t.message}")
        } finally {
            view.setSubmitting(false)
        }
    }
}
