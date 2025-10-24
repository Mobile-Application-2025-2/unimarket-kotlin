package com.example.unimarket.controller.auth

import android.graphics.Bitmap
//import com.example.unimarket.model.entity.CameraModel
//import com.example.unimarket.model.entity.CameraResult
interface StudentCodeViewPort {
    fun setProceedEnabled(enabled: Boolean)
    fun showBuyer()
    fun showCourier()
    fun showMessage(message: String)
    fun openCamera()
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

    fun onCameraIconClicked() {
        view.openCamera()
    }

    fun onCameraResult(bitmap: Bitmap?) {
        //when (CameraModel.parseResult(bitmap)) {
        //    CameraResult.Captured -> { /* no-op */ }
        //    CameraResult.Cancelled -> { /* no-op */ }
        //}
    }
}