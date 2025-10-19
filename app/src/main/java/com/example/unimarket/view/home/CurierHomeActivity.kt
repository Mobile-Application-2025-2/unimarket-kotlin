package com.example.unimarket.view.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

class CourierHomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val address = intent.getStringExtra(EXTRA_ADDRESS)
            ?: "Cra 1 #1-1, Bogotá" // dirección por defecto

        setContent {
            MaterialTheme {
                CourierHomeScreen(deliveryAddress = address)
            }
        }
    }

    companion object {
        const val EXTRA_ADDRESS = "deliveryAddress"
    }
}
