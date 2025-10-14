package com.example.unimarket.controller.auth

interface StudentCodeViewPort {
    fun setProceedEnabled(enabled: Boolean)
    fun showBuyer()
    fun showCourier()
    fun showMessage(message: String)
}

class StudentCodeController(
    private val view: StudentCodeViewPort
) {
    fun onInputChanged(text: String?) {
        val enabled = !text?.trim().isNullOrEmpty()
        view.setProceedEnabled(enabled)
    }

    fun onGetStartedClicked(raw: String?) {
        val role = raw?.trim()?.lowercase().orEmpty()
        when (role) {
            "buyer" -> view.showBuyer()
            "deliver", "courier", "driver", "delivery" -> view.showCourier()
            else -> view.showMessage("Escribe buyer o deliver (courier) para continuar.")
        }
    }
}