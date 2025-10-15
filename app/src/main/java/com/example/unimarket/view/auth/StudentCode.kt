package com.example.unimarket.view.auth

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.example.unimarket.databinding.ActivityStudentCodeBinding

import com.example.unimarket.view.explore.ExploreBuyerScreen
import com.example.unimarket.view.home.CourierHomeScreen
import com.example.unimarket.controller.auth.StudentCodeController
import com.example.unimarket.controller.auth.StudentCodeViewPort

class StudentCodeActivity : AppCompatActivity(), StudentCodeViewPort {

    private lateinit var b: ActivityStudentCodeBinding
    private lateinit var controller: StudentCodeController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityStudentCodeBinding.inflate(layoutInflater)
        (b.root.parent as? ViewGroup)?.removeView(b.root)

        setContentView(b.root)

        controller = StudentCodeController(this)

         b.etStudentId.doAfterTextChanged { controller.onInputChanged(it?.toString()) }
        controller.onInputChanged(b.etStudentId.text?.toString())

        b.btnGetStarted.setOnClickListener {
            if (!b.btnGetStarted.isEnabled) return@setOnClickListener

            controller.onGetStartedClicked(b.etStudentId.text?.toString())
        }
    }

    override fun setProceedEnabled(enabled: Boolean) {
        b.btnGetStarted.isEnabled = enabled
    }

    override fun showBuyer() {
        /*showCompose { ExploreBuyerScreen() }*/
    }

    override fun showCourier() {
        val address = "Calle 26 # 68D-43, Bogotá"
        showCompose { CourierHomeScreen(deliveryAddress = address) }
    }

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private inline fun showCompose(crossinline content: @Composable () -> Unit) {
        val composeView = ComposeView(this).apply {
            setContent { MaterialTheme { content() } }
        }
        setContentView(composeView)
    }
}