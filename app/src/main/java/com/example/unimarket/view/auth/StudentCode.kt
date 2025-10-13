package com.example.unimarket.view.auth

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.view.platform.ComposeView
import com.example.unimarket.databinding.ActivityStudentCodeBinding

import com.example.unimarket.view.explore.ExploreBuyerScreen
import com.example.unimarket.view.home.CourierHomeScreen

class StudentCodeActivity : AppCompatActivity() {

    private lateinit var b: ActivityStudentCodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityStudentCodeBinding.inflate(layoutInflater)

        // FIX: por si la vista llega con padre (pasa a veces al recrear/preview)
        (b.root.parent as? ViewGroup)?.removeView(b.root)

        setContentView(b.root)

        b.etStudentId.doAfterTextChanged { refreshState() }
        refreshState()

        b.btnGetStarted.setOnClickListener {
            if (!b.btnGetStarted.isEnabled) return@setOnClickListener

            val role = b.etStudentId.text?.toString()?.trim()?.lowercase().orEmpty()
            when (role) {
                "buyer" -> showCompose { ExploreBuyerScreen() }
                "deliver", "courier", "driver" -> showCompose { CourierHomeScreen() }
                else -> Toast.makeText(
                    this,
                    "Escribe buyer o deliver (courier) para continuar.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun refreshState() {
        b.btnGetStarted.isEnabled = !b.etStudentId.text?.toString()?.trim().isNullOrEmpty()
    }

    /** Reemplaza TODO el contenido de la Activity con un ComposeView nuevo. */
    private inline fun showCompose(crossinline content: @Composable () -> Unit) {
        val composeView = ComposeView(this).apply {
            setContent { MaterialTheme { content() } }
        }
        setContentView(composeView)
    }
}