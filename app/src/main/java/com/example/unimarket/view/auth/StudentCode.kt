package com.example.unimarket.view.auth

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityStudentCodeBinding.inflate(layoutInflater)
        (b.root.parent as? ViewGroup)?.removeView(b.root)
        setContentView(b.root)

        // Init del flujo (define rol y primer paso)
        viewModel.student_init()

        // Inputs -> VM
        b.etStudentId.doAfterTextChanged { text ->
            viewModel.student_onInputChanged(text?.toString().orEmpty())
        }

        // CTA
        b.btnGetStarted.setOnClickListener { viewModel.student_next() }

        // End icon (cámara del layout). Mantener por compatibilidad:
        b.tilStudentId.setEndIconOnClickListener { viewModel.student_onCameraIconClicked() }

        observeStudentUi()
    }

    private fun observeStudentUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.student.collect { ui ->
                    // Bind dinámico de textos del paso actual (usar ids reales del XML)
                    b.title.text = ui.title
                    b.subtitle.text = ui.subtitle
                    b.tilStudentId.hint = ui.hint
                    b.btnGetStarted.text = ui.cta

                    // Sincroniza el texto si cambió por estado
                    val current = b.etStudentId.text?.toString().orEmpty()
                    if (current != ui.textValue) {
                        b.etStudentId.setText(ui.textValue)
                        b.etStudentId.setSelection(b.etStudentId.text?.length ?: 0)
                    }

                    // Habilitar/Deshabilitar CTA según validación
                    b.btnGetStarted.isEnabled = ui.canProceed

                    // Mensaje de error (one-shot)
                    ui.errorMessage?.let { msg ->
                        Toast.makeText(this@StudentCodeActivity, msg, Toast.LENGTH_SHORT).show()
                        viewModel.student_clearNavAndErrors()
                    }

                    // Navegación final
                    when (ui.nav) {
                        AuthNavDestination.ToBuyerHome -> {
                            startActivity(Intent(this@StudentCodeActivity, HomeBuyerActivity::class.java))
                            finish()
                            viewModel.student_clearNavAndErrors()
                        }
                        AuthNavDestination.ToBusinessProfile -> {
                            startActivity(Intent(this@StudentCodeActivity, BusinessAccountActivity::class.java))
                            finish()
                            viewModel.student_clearNavAndErrors()
                        }
                        AuthNavDestination.ToCourierHome -> {
                            startActivity(Intent(this@StudentCodeActivity, CourierHomeActivity::class.java))
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