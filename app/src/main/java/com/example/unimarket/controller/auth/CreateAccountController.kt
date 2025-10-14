package com.example.unimarket.controller.auth

import com.example.unimarket.model.entity.SignUpBody
import com.example.unimarket.model.repository.AuthRepository

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
            repo.signUp(body)
            view.toast("Cuenta creada")
            view.navigateToStudentCode()
        } catch (t: Throwable) {
            view.toast("Error: ${t.message}")
        } finally {
            view.setSubmitting(false)
        }
    }
}