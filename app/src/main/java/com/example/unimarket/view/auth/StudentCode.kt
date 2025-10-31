package com.example.unimarket.view.auth

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.unimarket.databinding.ActivityStudentCodeBinding
import com.example.unimarket.view.home.CourierHomeActivity
import com.example.unimarket.view.home.HomeBuyerActivity
import com.example.unimarket.view.profile.BusinessAccountActivity
import com.example.unimarket.viewmodel.AuthNavDestination
import com.example.unimarket.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class StudentCodeActivity : AppCompatActivity() {

    private lateinit var b: ActivityStudentCodeBinding
    private val viewModel: AuthViewModel by viewModels()

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        viewModel.student_onCameraResult(bitmap)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityStudentCodeBinding.inflate(layoutInflater)
        (b.root.parent as? ViewGroup)?.removeView(b.root)
        setContentView(b.root)

        b.etStudentId.doAfterTextChanged { text ->
            viewModel.student_onInputChanged(text?.toString() ?: "")
        }
        viewModel.student_onInputChanged(b.etStudentId.text?.toString() ?: "")

        b.btnGetStarted.setOnClickListener {
            viewModel.student_onGetStartedClicked()
        }

        b.tilStudentId.setEndIconOnClickListener {
            viewModel.student_onCameraIconClicked()
        }

        observeWelcomeForAutoNavigation()
        observeStudentUi()

        viewModel.welcome_onInit()
    }

    private fun observeWelcomeForAutoNavigation() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.welcome.collect { ui ->
                    when (ui.nav) {
                        AuthNavDestination.ToBuyerHome -> {
                            startActivity(Intent(this@StudentCodeActivity, HomeBuyerActivity::class.java))
                            finish()
                        }
                        AuthNavDestination.ToCourierHome -> {
                            startActivity(Intent(this@StudentCodeActivity, CourierHomeActivity::class.java))
                            finish()
                        }
                        AuthNavDestination.ToBusinessProfile -> {
                            startActivity(Intent(this@StudentCodeActivity, BusinessAccountActivity::class.java))
                            finish()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun observeStudentUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.student.collect { ui ->
                    b.btnGetStarted.isEnabled = ui.canProceed

                    ui.errorMessage?.let { msg ->
                        Toast.makeText(this@StudentCodeActivity, msg, Toast.LENGTH_SHORT).show()
                        viewModel.student_clearNavAndErrors()
                    }

                    if (ui.requestOpenCamera) {
                        cameraLauncher.launch(null)
                        viewModel.student_onCameraHandled()
                    }

                    when (ui.nav) {
                        AuthNavDestination.ToBuyerHome -> {
                            startActivity(Intent(this@StudentCodeActivity, HomeBuyerActivity::class.java))
                            finish()
                            viewModel.student_clearNavAndErrors()
                        }
                        AuthNavDestination.ToCourierHome -> {
                            startActivity(Intent(this@StudentCodeActivity, CourierHomeActivity::class.java))
                            finish()
                            viewModel.student_clearNavAndErrors()
                        }
                        AuthNavDestination.ToBusinessProfile -> {
                            startActivity(Intent(this@StudentCodeActivity, BusinessAccountActivity::class.java))
                            finish()
                            viewModel.student_clearNavAndErrors()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}