package com.example.unimarket.controller.auth

import com.example.unimarket.model.domain.entity.Address
import com.example.unimarket.model.domain.entity.User
import com.example.unimarket.model.domain.service.AuthService
import com.example.unimarket.model.session.SessionManager

interface CreateAccountViewPort {
    fun setSubmitting(submitting: Boolean)
    fun toast(message: String)
    fun navigateToStudentCode()
}

class CreateAccountController(
    private val view: CreateAccountViewPort,
    private val auth: AuthService = AuthService()
) {
    private val DEFAULT_USER_TYPE = "buyer" // buyer | business

    /**
     * - Si type == "business": businessName es obligatorio (logo / address opcionales).
     * - Si type == "buyer": buyerAddresses es opcional (puedes pasar vacío).
     */
    suspend fun onSignUpClicked(
        name: String,
        email: String,
        pass: String,
        accepted: Boolean,
        // Campos user:
        idType: String = "id",
        idNumber: String = "",
        type: String = DEFAULT_USER_TYPE,
        // Business (opcionales):
        businessName: String? = null,
        businessLogo: String? = null,
        businessAddress: Address? = null,
        // Buyer (opcional):
        buyerAddresses: List<Address>? = null
    ) {
        if (!accepted) {
            view.toast("Debes aceptar la política de privacidad")
            return
        }

        view.setSubmitting(true)
        try {
            val user = User(
                email = email,
                name = name,
                idType = idType,
                idNumber = idNumber,
                type = type.lowercase().trim()
            )

            val result = auth.signUp(
                user = user,
                password = pass,
                businessName = businessName,
                businessLogo = businessLogo,
                businessAddress = businessAddress,
                buyerAddresses = buyerAddresses
            )

            result.onSuccess { created ->
                // En este punto el AuthService ya hizo login y actualizó el SessionManager.
                val session = SessionManager.get()
                val role = session?.type ?: created.type
                view.toast("Cuenta creada (${role})")
                view.navigateToStudentCode()
            }.onFailure { e ->
                view.toast("Error: ${e.message}")
            }
        } catch (t: Throwable) {
            view.toast("Error: ${t.message}")
        } finally {
            view.setSubmitting(false)
        }
    }
}