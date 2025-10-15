package com.example.unimarket.model.entity

import android.graphics.Bitmap

sealed class CameraResult {
    data object Captured : CameraResult()
    data object Cancelled : CameraResult()
}

object CameraModel {
    fun parseResult(bitmap: Bitmap?): CameraResult =
        if (bitmap != null) CameraResult.Captured else CameraResult.Cancelled
}