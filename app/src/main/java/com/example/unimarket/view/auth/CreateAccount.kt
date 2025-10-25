package com.example.unimarket.view.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.unimarket.R
import com.example.unimarket.databinding.ActivityCreateAccountBinding
import com.example.unimarket.viewmodel.AuthNavDestination
import com.example.unimarket.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import com.google.android.material.textfield.TextInputLayout

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var b: ActivityCreateAccountBinding

    // ViewModel compartido que ya tiene la lógica de signup
    private val viewModel: AuthViewModel by viewModels()

    // Lo usamos solo para habilitar/deshabilitar el botón y los iconos inline
    // sin esperar a que el usuario presione "Sign In".
    private var canProceed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_account)
        val root = findViewById<NestedScrollView>(R.id.createAccountRoot)
        b = ActivityCreateAccountBinding.bind(root)

        // Botón back
        b.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Botones federados (aún TODO)
        b.btnOutlook.setOnClickListener { /* TODO: federated sign-in */ }
        b.btnGoogle.setOnClickListener  { /* TODO: Google Sign-In */ }

        // =====================================
        // INPUT LISTENERS -> ViewModel
        // =====================================

        b.etName.doAfterTextChanged { text ->
            viewModel.create_onNameChanged(text.toString())
            refreshLocalValidation()
        }

        b.etEmail.doAfterTextChanged { text ->
            viewModel.create_onEmailChanged(text.toString())
            refreshLocalValidation()
        }

        b.etPassword.doAfterTextChanged { text ->
            viewModel.create_onPasswordChanged(text.toString())
            refreshLocalValidation()
        }

        b.cbAccept.setOnCheckedChangeListener { _, isChecked ->
            viewModel.create_onPolicyToggled(isChecked)
            refreshLocalValidation()
        }

        // Estado inicial de los controles
        refreshLocalValidation()

        // =====================================
        // CLICK "Sign In" (aquí realmente es crear cuenta)
        // =====================================

        b.btnSignIn.setOnClickListener {
            if (!canProceed) {
                // Mostrar errores inline locales (como en tu versión original)
                showInlineErrors()
                Toast.makeText(
                    this,
                    getString(R.string.complete_the_fields),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Llamamos al ViewModel para que ejecute el signUp real.
            // create_submit ya ejecuta coroutine interna y actualiza el estado.
            viewModel.create_submit(
                // ahora mismo asumimos usuario "buyer" por defecto,
                // igual que tu controlador hacía.
                type = "buyer"
            )
        }

        // =====================================
        // OBSERVAR EL STATEFLOW DEL VIEWMODEL
        // =====================================

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.create.collect { ui ->

                    // 1. Errores de validación desde el ViewModel
                    //    (cuando el usuario ya intentó enviar y falló reglas)
                    b.tilName.error = ui.nameError
                    b.tilEmail.error = ui.emailError
                    b.tilPassword.error = ui.passwordError
                    // policyError no es un TextInputLayout, lo manejamos con Toast
                    ui.acceptedPolicyError?.let { policyMsg ->
                        Toast.makeText(this@CreateAccountActivity, policyMsg, Toast.LENGTH_SHORT)
                            .show()
                    }

                    // 2. Loading / botón estado de envío
                    if (ui.isSubmitting) {
                        b.btnSignIn.isEnabled = false
                        b.btnSignIn.text = getString(R.string.creating_account)
                        b.btnSignIn.alpha = 0.6f
                    } else {
                        // volvemos a usar nuestra validación local para habilitar/deshabilitar
                        refreshLocalValidation()
                        b.btnSignIn.text = getString(R.string.action_sign_up)
                    }

                    // 3. Mensaje tipo toastMessage (éxito o error del signup)
                    ui.toastMessage?.let { msg ->
                        Toast.makeText(this@CreateAccountActivity, msg, Toast.LENGTH_LONG).show()
                        // Limpiamos el mensaje en el VM para que no se repita
                        viewModel.create_clearNavAndToast()
                    }

                    // 4. Navegación posterior al signup
                    when (ui.nav) {
                        AuthNavDestination.ToStudentCode -> {
                            startActivity(
                                Intent(
                                    this@CreateAccountActivity,
                                    StudentCodeActivity::class.java
                                )
                            )
                            finish()
                            viewModel.create_clearNavAndToast()
                        }

                        else -> {
                            // nada
                        }
                    }
                }
            }
        }
    }

    /**
     * Esta función hace la validación rápida que tú hacías en refreshState():
     * - nombre >= 3
     * - email válido por regex
     * - pass >= 8
     * - cbAccept checkeado
     *
     * Actualiza:
     *   - iconos de check en Name/Email
     *   - alpha del botón
     *   - canProceed (bandera local)
     *
     * Nota: esto es puramente visual / UX rápida mientras escribe.
     * La validación definitiva igual se hace en el ViewModel al llamar create_submit().
     */
    private fun refreshLocalValidation() {
        val name  = b.etName.text?.toString()?.trim().orEmpty()
        val email = b.etEmail.text?.toString()?.trim().orEmpty()
        val pass  = b.etPassword.text?.toString().orEmpty()
        val accepted = b.cbAccept.isChecked

        val validName  = name.length >= 3
        val validEmail = EMAIL_REGEX.matches(email)
        val validPass  = pass.length >= 8

        canProceed = validName && validEmail && validPass && accepted

        // Muestra el ícono verde de check (si falla el try, lo ignora)
        b.tilName.setEndIconVisibleCompat(validName)
        b.tilEmail.setEndIconVisibleCompat(validEmail)

        // Alpha visual del botón (pero no lo deshabilitamos del todo;
        // eso lo hace el VM cuando está isSubmitting)
        b.btnSignIn.isEnabled = true
        b.btnSignIn.alpha = if (canProceed) 1f else 0.6f

        // Limpieza de errores inline mientras el usuario arregla cosas
        if (validName)  b.tilName.error = null
        if (validEmail) b.tilEmail.error = null
        if (validPass)  b.tilPassword.error = null
    }

    /**
     * Esto replica showInlineErrors() original:
     * si el usuario intenta enviar pero no cumple las reglas,
     * se marcan errores y se hace focus en el primero que falle.
     */
    private fun showInlineErrors() {
        val name  = b.etName.text?.toString()?.trim().orEmpty()
        val email = b.etEmail.text?.toString()?.trim().orEmpty()
        val pass  = b.etPassword.text?.toString().orEmpty()

        if (name.length < 3) {
            b.tilName.error = getString(R.string.error_name_min3)
            b.etName.requestFocus()
        } else {
            b.tilName.error = null
        }

        if (!EMAIL_REGEX.matches(email)) {
            b.tilEmail.error = getString(R.string.error_email_invalid)
            if (b.tilName.error == null) b.etEmail.requestFocus()
        } else {
            b.tilEmail.error = null
        }

        if (pass.length < 8) {
            b.tilPassword.error = getString(R.string.error_password_min8)
            if (b.tilName.error == null && b.tilEmail.error == null) {
                b.etPassword.requestFocus()
            }
        } else {
            b.tilPassword.error = null
        }

        if (!b.cbAccept.isChecked) {
            Toast.makeText(this, getString(R.string.accept_privacy), Toast.LENGTH_SHORT).show()
        }
    }

    private fun TextInputLayout.setEndIconVisibleCompat(visible: Boolean) {
        try {
            isEndIconVisible = visible
        } catch (_: Throwable) {
            // Algunos dispositivos viejos rompen esto, así que lo protegiste con try/catch. Lo dejo igual.
        }
    }

    companion object {
        // Igual que tu versión original
        private val EMAIL_REGEX =
            Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}