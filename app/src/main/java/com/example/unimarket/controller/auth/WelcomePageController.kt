package com.example.unimarket.controller.auth

interface WelcomePageViewPort {
    fun playIntroOverlay()
    fun navigateToSignUp()
    fun navigateToLogin()
}

class WelcomePageController(
    private val view: WelcomePageViewPort
) {
    fun onInit() {
        view.playIntroOverlay()
    }
    fun onSignUpClicked() {
        view.navigateToSignUp()
    }
    fun onLoginClicked() {
        view.navigateToLogin()
    }
}