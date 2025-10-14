package com.example.unimarket.controller.auth

import com.example.unimarket.model.session.SessionManager

interface WelcomeViewPort {
    fun playIntroOverlay()
    fun navigateToSignUp()
    fun navigateToSignIn()
    fun navigateToBuyer()
    fun navigateToCourier()
}

class WelcomePageController(
    private val view: WelcomeViewPort
) {

    fun onInit() {
        view.playIntroOverlay()
    }

    fun checkExistingSession() {
        if (!SessionManager.isLoggedIn) return
        when (SessionManager.get()!!.type.trim().lowercase()) {
            "buyer" -> view.navigateToBuyer()
            "deliver", "delivery", "courier" -> view.navigateToCourier()
            else -> { /* si el tipo no es reconocido, te quedas en welcome */ }
        }
    }

    fun onClickSignUp() = view.navigateToSignUp()
    fun onClickSignIn() = view.navigateToSignIn()
}