package com.example.unimarket.controller.auth

import com.example.unimarket.model.session.SessionManager

interface WelcomePageViewPort {
    fun playIntroOverlay()
    fun navigateToCreateAccount()
    fun navigateToLogin()
    fun navigateToBuyer()
    fun navigateToCourier()
}

class WelcomePageController(
    private val view: WelcomePageViewPort
) {
    fun onInit() {
        val s = SessionManager.get()
        if (s != null) {
            when (s.type.lowercase()) {
                "buyer" -> view.navigateToBuyer()
                "deliver", "delivery", "courier" -> view.navigateToCourier()
                else -> view.playIntroOverlay()
            }
        } else {
            view.playIntroOverlay()
        }
    }

    fun onClickSignUp()  = view.navigateToCreateAccount()
    fun onClickLogin()   = view.navigateToLogin()
}