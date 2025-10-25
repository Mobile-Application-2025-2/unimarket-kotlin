package com.example.unimarket.view.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.unimarket.R
import com.example.unimarket.view.explore.ExploreBuyerActivity
import com.example.unimarket.view.home.CourierHomeActivity
import com.example.unimarket.viewmodel.AuthNavDestination
import com.example.unimarket.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import android.text.Editable
import android.text.TextWatcher

class LoginActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var btnBack: ImageButton
    private lateinit var tvSignUp: TextView
    private lateinit var btnSignIn: MaterialButton

    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var ivToggle: ImageView

    private var passwordVisible = false
    private var originalBtnText: CharSequence? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_in)

        btnBack = findViewById(R.id.btnBack)
        tvSignUp = findViewById(R.id.tvSignUp)
        btnSignIn = findViewById(R.id.btnSignIn)
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etPassword = findViewById(R.id.etPassword)
        ivToggle = findViewById(R.id.ivTogglePassword)

        // ======================================
        // TOGGLE PASSWORD VISIBILITY (local UI)
        // ======================================
        val closedFromIv = ivToggle.drawable
        val closedFromTil = tilPassword.endIconDrawable

        fun renderPasswordUi() {
            // Mostrar u ocultar caracteres
            etPassword.transformationMethod =
                if (passwordVisible) null else PasswordTransformationMethod.getInstance()

            // Mover el cursor al final después de cambiar el transformationMethod
            etPassword.setSelection(etPassword.text?.length ?: 0)

            // Actualizar ícono (usaste drawables "open"/"closed")
            if (ivToggle != null) {
                ivToggle.setImageDrawable(
                    if (passwordVisible)
                        ContextCompat.getDrawable(this, R.drawable.closed)
                    else
                        closedFromIv ?: ContextCompat.getDrawable(this, R.drawable.open)
                )
                ivToggle.imageTintList = null
            } else {
                // fallback al endIcon del TextInputLayout
                tilPassword.endIconMode = TextInputLayout.END_ICON_CUSTOM
                tilPassword.setEndIconTintList(null)
                tilPassword.endIconDrawable =
                    if (passwordVisible)
                        ContextCompat.getDrawable(this, R.drawable.open)
                    else
                        closedFromTil ?: ContextCompat.getDrawable(this, R.drawable.closed)
            }
        }

        ivToggle.setOnClickListener {
            passwordVisible = !passwordVisible
            renderPasswordUi()
        }
        tilPassword.setEndIconOnClickListener {
            passwordVisible = !passwordVisible
            renderPasswordUi()
        }
        renderPasswordUi()

        // ======================================
        // INPUT LISTENERS -> VIEWMODEL
        // ======================================
        // En tu versión vieja validabas localmente antes de llamar controller.onSignInClicked.
        // Ahora cada cambio se lo contamos al VM, y el VM hará la validación final al hacer submit.
        etEmail.doAfterTextChangedCompat { text ->
            viewModel.signIn_onEmailChanged(text)
        }

        etPassword.doAfterTextChangedCompat { text ->
            viewModel.signIn_onPasswordChanged(text)
        }

        // ======================================
        // BOTONES
        // ======================================
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        tvSignUp.setOnClickListener {
            // Igual que antes: ir a crear cuenta
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }

        btnSignIn.setOnClickListener {
            // Antes: validabas tú y llamabas controller.onSignInClicked(email, pass)
            // Ahora: el VM ya tiene email/pass en su estado y hace submit interno.
            viewModel.signIn_submit()
        }

        // ======================================
        // OBSERVAR EL STATEFLOW DEL VIEWMODEL (SignInUiState)
        // ======================================
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.signIn.collect { ui ->

                    // 1. errores de campo
                    tilEmail.error = ui.emailError
                    tilPassword.error = ui.passwordError

                    // 2. loading / estado del botón
                    if (ui.isSubmitting) {
                        if (originalBtnText == null) {
                            originalBtnText = btnSignIn.text
                        }
                        btnSignIn.isEnabled = false
                        btnSignIn.text = getString(R.string.signing_in)
                    } else {
                        btnSignIn.isEnabled = true
                        btnSignIn.text = originalBtnText ?: getString(R.string.sign_in)
                    }

                    // 3. error global (por ej. credenciales inválidas o tipo de usuario desconocido)
                    if (ui.errorMessage != null) {
                        Toast.makeText(this@LoginActivity, ui.errorMessage, Toast.LENGTH_LONG)
                            .show()

                        // IMPORTANTE:
                        // signIn_clearNavAndErrors() limpia tanto errorMessage como nav=NONE
                        // después de que ya reaccionamos. Esto evita repetir el toast tras rotaciones.
                        viewModel.signIn_clearNavAndErrors()
                    }

                    // 4. navegación según el rol
                    when (ui.nav) {
                        AuthNavDestination.ToBuyerHome -> {
                            val intent = Intent(
                                this@LoginActivity,
                                ExploreBuyerActivity::class.java
                            ).apply {
                                addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                )
                            }
                            startActivity(intent)

                            // limpiamos nav y errores para no repetir
                            viewModel.signIn_clearNavAndErrors()
                        }

                        AuthNavDestination.ToCourierHome -> {
                            val intent = Intent(
                                this@LoginActivity,
                                CourierHomeActivity::class.java
                            )
                            startActivity(intent)
                            finish()

                            viewModel.signIn_clearNavAndErrors()
                        }

                        else -> {
                            // AuthNavDestination.None -> no navegamos
                        }
                    }
                }
            }
        }
    }

    /**
     * doAfterTextChanged es una extension de core-ktx para EditText.
     * En tu código original no la estabas usando en LoginActivity, solo en CreateAccount.
     * Vamos a darle una extensión local segura para no repetir null checks.
     */
    private fun TextInputEditText.doAfterTextChangedCompat(block: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // no-op
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                // no-op
            }

            override fun afterTextChanged(s: Editable?) {
                block(s?.toString() ?: "")
            }
        })
    }
}