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
import com.example.unimarket.viewmodel.AuthNavDestination
import com.example.unimarket.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

/**
 * Ahora este screen actúa como “puente” tras el registro:
 * - Al abrirse, consulta el rol desde la sesión (via welcome_onInit()) y navega automáticamente.
 * - Mantengo la lógica de cámara (por si la usas luego), pero ya NO requiere escribir "buyer/deliver".
 */
class StudentCodeActivity : AppCompatActivity() {

    private lateinit var b: ActivityStudentCodeBinding
    private val viewModel: AuthViewModel by viewModels()

    // Cámara (se mantiene por si la necesitas después)
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        viewModel.student_onCameraResult(bitmap)
        // (futuro: preview / upload)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityStudentCodeBinding.inflate(layoutInflater)
        (b.root.parent as? ViewGroup)?.removeView(b.root)
        setContentView(b.root)

        // Ya NO obligamos a escribir nada: oculto el botón si quieres, o lo dejo habilitado.
        // Aquí lo dejo visible y usable por si decides hacer otra cosa con este screen.
        b.etStudentId.doAfterTextChanged { text ->
            // Normaliza, aunque no lo usamos para navegar ya.
            viewModel.student_onInputChanged(text?.toString() ?: "")
        }
        viewModel.student_onInputChanged(b.etStudentId.text?.toString() ?: "")

        b.btnGetStarted.setOnClickListener {
            // Si lo presionan, puedes seguir soportando la navegación manual:
            viewModel.student_onGetStartedClicked()
        }

        // Icono de cámara
        b.tilStudentId.setEndIconOnClickListener {
            viewModel.student_onCameraIconClicked()
        }

        // 1) Observa el flujo "welcome" para navegación automática por rol real
        observeWelcomeForAutoNavigation()

        // 2) Observa el flujo "student" para cámara / errores (opcional)
        observeStudentUi()

        // Dispara la verificación de sesión+rol en el VM
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
                            // No limpiamos welcome.nav aquí, este screen muere
                        }
                        AuthNavDestination.ToCourierHome -> {
                            startActivity(Intent(this@StudentCodeActivity, CourierHomeActivity::class.java))
                            finish()
                        }
                        else -> {
                            // None: aún sin decisión (p. ej., animación/intro o sesión nula).
                            // Si acabas de crear cuenta, normalmente debería resolver a uno de los anteriores.
                        }
                    }
                }
            }
        }
    }

    private fun observeStudentUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.student.collect { ui ->
                    // Habilita/Deshabilita botón (aunque ya no sea necesario)
                    b.btnGetStarted.isEnabled = ui.canProceed

                    ui.errorMessage?.let { msg ->
                        Toast.makeText(this@StudentCodeActivity, msg, Toast.LENGTH_SHORT).show()
                        viewModel.student_clearNavAndErrors()
                    }

                    if (ui.requestOpenCamera) {
                        cameraLauncher.launch(null)
                        viewModel.student_onCameraHandled()
                    }

                    // Si alguien usa el botón y el VM decide navegar manualmente:
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
                        else -> Unit
                    }
                }
            }
        }
    }
}