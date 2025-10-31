package com.example.unimarket.view.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.unimarket.view.home.HomeBuyerActivity
import com.example.unimarket.view.home.CourierHomeActivity
import com.example.unimarket.viewmodel.AuthNavDestination
import com.example.unimarket.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

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

        // =============== Toggle password visibility ===============
        val closedFromIv = ivToggle.drawable
        val closedFromTil = tilPassword.endIconDrawable

        fun renderPasswordUi() {
            etPassword.transformationMethod =
                if (passwordVisible) null else PasswordTransformationMethod.getInstance()
            etPassword.setSelection(etPassword.text?.length ?: 0)

            if (ivToggle != null) {
                ivToggle.setImageDrawable(
                    if (passwordVisible)
                        ContextCompat.getDrawable(this, R.drawable.open)
                    else
                        closedFromIv ?: ContextCompat.getDrawable(this, R.drawable.closed)
                )
                ivToggle.imageTintList = null
            } else {
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

        // ================= Inputs -> ViewModel =================
        etEmail.doAfterTextChangedCompat { text ->
            viewModel.signIn_onEmailChanged(text)
        }
        etPassword.doAfterTextChangedCompat { text ->
            viewModel.signIn_onPasswordChanged(text)
        }

        // =================== Botones ===========================
        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }
        btnSignIn.setOnClickListener {
            viewModel.signIn_submit()
        }

        // ============== Observer del estado de SignIn ==========
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.signIn.collect { ui ->

                    // 1) errores de campo
                    tilEmail.error = ui.emailError
                    tilPassword.error = ui.passwordError

                    // 2) loading / botón
                    if (ui.isSubmitting) {
                        if (originalBtnText == null) originalBtnText = btnSignIn.text
                        btnSignIn.isEnabled = false
                        btnSignIn.text = getString(R.string.signing_in)
                    } else {
                        btnSignIn.isEnabled = true
                        btnSignIn.text = originalBtnText ?: getString(R.string.sign_in)
                    }

                    // 3) error global
                    if (ui.errorMessage != null) {
                        Toast.makeText(this@LoginActivity, ui.errorMessage, Toast.LENGTH_LONG).show()
                        viewModel.signIn_clearNavAndErrors()
                    }

                    // 4) navegación
                    when (ui.nav) {
                        AuthNavDestination.ToBuyerHome -> {
                            val intent = Intent(this@LoginActivity, HomeBuyerActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                            // si quieres cerrar este screen:
                            // finish()
                            viewModel.signIn_clearNavAndErrors()
                        }
                        AuthNavDestination.ToCourierHome -> {
                            // Si tu VM mapea "deliver/courier" aquí, navega:
                            val intent = Intent(this@LoginActivity, CourierHomeActivity::class.java)
                            startActivity(intent)
                            finish()
                            viewModel.signIn_clearNavAndErrors()
                        }
                        else -> {
                            // None: no navegamos (si luego quieres distinguir "business", ajustamos el VM)
                        }
                    }
                }
            }
        }
    }

    /** Extensión local para afterTextChanged segura */
    private fun TextInputEditText.doAfterTextChangedCompat(block: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                block(s?.toString() ?: "")
            }
        })
    }
}