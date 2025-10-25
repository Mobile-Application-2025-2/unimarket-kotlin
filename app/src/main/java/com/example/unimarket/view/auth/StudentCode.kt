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
import com.example.unimarket.view.explore.ExploreBuyerActivity
import com.example.unimarket.view.home.CourierHomeActivity
import com.example.unimarket.viewmodel.AuthNavDestination
import com.example.unimarket.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

// Nota: ya no implementa StudentCodeViewPort ni usa StudentCodeController.
class StudentCodeActivity : AppCompatActivity() {

    private lateinit var b: ActivityStudentCodeBinding

    private val viewModel: AuthViewModel by viewModels()

    // launcher de cámara, que ahora notifica al VM
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        viewModel.student_onCameraResult(bitmap)
        // si quieres futura lógica adicional con la foto (preview, upload), irá acá.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityStudentCodeBinding.inflate(layoutInflater)
        (b.root.parent as? ViewGroup)?.removeView(b.root)
        setContentView(b.root)

        // ==========================
        // INPUT LISTENER → VM
        // ==========================
        b.etStudentId.doAfterTextChanged { text ->
            viewModel.student_onInputChanged(text?.toString() ?: "")
        }

        // inicializar el estado de proceed según el texto actual (puede venir vacío)
        viewModel.student_onInputChanged(b.etStudentId.text?.toString() ?: "")

        // ==========================
        // BOTÓN "GET STARTED"
        // ==========================
        b.btnGetStarted.setOnClickListener {
            // Antes: controller.onGetStartedClicked(raw)
            // Ahora: el VM decide el destino y lo expone en student.nav
            viewModel.student_onGetStartedClicked()
        }

        // ==========================
        // ICONO DE CÁMARA EN EL TEXTINPUT
        // ==========================
        b.tilStudentId.setEndIconOnClickListener {
            // Antes: controller.onCameraIconClicked() → view.openCamera()
            // Ahora: le avisamos al VM que el user pidió cámara.
            viewModel.student_onCameraIconClicked()
        }

        // ==========================
        // OBSERVAR EL STATEFLOW DEL VM
        // ==========================
        observeStudentState()
    }

    private fun observeStudentState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.student.collect { ui ->

                    // 1. Habilitar botón según canProceed
                    b.btnGetStarted.isEnabled = ui.canProceed

                    // 2. Mensaje de error puntual (equivalente a showMessage(message))
                    ui.errorMessage?.let { msg ->
                        Toast.makeText(this@StudentCodeActivity, msg, Toast.LENGTH_SHORT).show()
                        // limpiamos error y nav después de usarlo
                        viewModel.student_clearNavAndErrors()
                    }

                    // 3. ¿El VM está pidiendo que abramos la cámara?
                    if (ui.requestOpenCamera) {
                        // Lanzamos la cámara
                        cameraLauncher.launch(null)
                        // Avisamos al VM que ya lo hicimos para bajar el flag
                        viewModel.student_onCameraHandled()
                    }

                    // 4. Navegación según lo que interpretó el VM
                    when (ui.nav) {
                        AuthNavDestination.ToBuyerHome -> {
                            startActivity(
                                Intent(
                                    this@StudentCodeActivity,
                                    ExploreBuyerActivity::class.java
                                )
                            )
                            // limpiamos estado para no repetir
                            viewModel.student_clearNavAndErrors()
                        }

                        AuthNavDestination.ToCourierHome -> {
                            startActivity(
                                Intent(
                                    this@StudentCodeActivity,
                                    CourierHomeActivity::class.java
                                )
                            )
                            // si quieres cerrar esta Activity:
                            finish()
                            viewModel.student_clearNavAndErrors()
                        }

                        else -> {
                            // AuthNavDestination.None -> nada
                        }
                    }
                }
            }
        }
    }
}