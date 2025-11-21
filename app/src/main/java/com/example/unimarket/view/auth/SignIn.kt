package com.example.unimarket.view.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.example.unimarket.view.profile.BusinessAccountActivity
import com.example.unimarket.viewmodel.AuthNavDestination
import com.example.unimarket.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import com.example.unimarket.workers.PrefetchBusinessesWorker

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
        val btnOutlook = findViewById<MaterialCardView>(R.id.btnOutlook)
        val btnGoogle  = findViewById<MaterialCardView>(R.id.btnGoogle)

        // Límite 50 caracteres
        attachMaxLengthBehavior(etEmail)
        attachMaxLengthBehavior(etPassword)

        btnOutlook.setOnClickListener { showFeatureUnavailableToast() }
        btnGoogle.setOnClickListener  { showFeatureUnavailableToast() }

        // Toggle password
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
        ivToggle.setOnClickListener { passwordVisible = !passwordVisible; renderPasswordUi() }
        tilPassword.setEndIconOnClickListener { passwordVisible = !passwordVisible; renderPasswordUi() }
        renderPasswordUi()

        // Inputs -> VM + limpieza de helpers
        etEmail.doAfterTextChangedCompat { value ->
            viewModel.signIn_onEmailChanged(value)
            if (EMAIL_REGEX.matches(value.trim())) {
                tilEmail.error = null
            }
        }
        etPassword.doAfterTextChangedCompat { value ->
            viewModel.signIn_onPasswordChanged(value)
            if (value.length >= 8) {
                tilPassword.error = null
            }
        }

        // Botones
        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        tvSignUp.setOnClickListener { startActivity(Intent(this, CreateAccountActivity::class.java)) }
        btnSignIn.setOnClickListener { viewModel.signIn_submit() }

        // Observer
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.signIn.collect { ui ->
                    val emailNow = etEmail.text?.toString().orEmpty().trim()
                    val passNow  = etPassword.text?.toString().orEmpty()

                    val validEmail = EMAIL_REGEX.matches(emailNow)
                    val validPass  = passNow.length >= 8

                    // Solo mostramos errores del VM si el campo sigue inválido
                    tilEmail.error = if (!validEmail) ui.emailError else null
                    tilPassword.error = if (!validPass) ui.passwordError else null

                    // Loading
                    if (ui.isSubmitting) {
                        if (originalBtnText == null) originalBtnText = btnSignIn.text
                        btnSignIn.isEnabled = false
                        btnSignIn.text = getString(R.string.signing_in)
                    } else {
                        btnSignIn.isEnabled = true
                        btnSignIn.text = originalBtnText ?: getString(R.string.sign_in)
                    }

                    // Error global
                    ui.errorMessage?.let {
                        Toast.makeText(this@LoginActivity, it, Toast.LENGTH_LONG).show()
                        viewModel.signIn_clearNavAndErrors()
                    }

                    // Navegación
                    when (ui.nav) {
                        AuthNavDestination.ToBuyerHome -> {
                            PrefetchBusinessesWorker.enqueue(applicationContext, replace = true)
                            val intent = Intent(this@LoginActivity, HomeBuyerActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                            finish()
                            viewModel.signIn_clearNavAndErrors()
                        }
                        AuthNavDestination.ToBusinessProfile -> {
                            PrefetchBusinessesWorker.enqueue(applicationContext, replace = true)
                            val intent = Intent(this@LoginActivity, BusinessAccountActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                            finish()
                            viewModel.signIn_clearNavAndErrors()
                        }
                        AuthNavDestination.ToStudentCode -> {
                            val intent = Intent(this@LoginActivity, StudentCodeActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                            finish()
                            viewModel.signIn_clearNavAndErrors()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    /** afterTextChanged segura */
    private fun TextInputEditText.doAfterTextChangedCompat(block: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { block(s?.toString() ?: "") }
        })
    }

    // --- Límite 50 caracteres + toast ---
    private fun attachMaxLengthBehavior(field: TextInputEditText) {
        val prevFilters = field.filters
        field.filters = prevFilters + InputFilter.LengthFilter(MAX_LENGTH)

        field.addTextChangedListener(object : TextWatcher {
            private var lastLength = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val newLen = s?.length ?: 0
                if (newLen == MAX_LENGTH && newLen > lastLength) {
                    showMaxLengthToast()
                }
                lastLength = newLen
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ---- Toasts personalizados ----

    private fun showFeatureUnavailableToast() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.parseColor("#FFFFFF"))
        }

        val iconView = ImageView(this).apply {
            setImageResource(R.drawable.personajesingup)
            val size = dp(20)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                rightMargin = dp(8)
            }
        }

        val textView = TextView(this).apply {
            text = "Esta opción aún no está habilitada"
            setTextColor(Color.BLACK)
            textSize = 14f
        }

        container.addView(iconView)
        container.addView(textView)

        Toast(this).apply {
            duration = Toast.LENGTH_SHORT
            view = container
            show()
        }
    }

    private fun showMaxLengthToast() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.parseColor("#FFFFFF"))
        }

        val iconView = ImageView(this).apply {
            setImageResource(R.drawable.personajesingup)
            val size = dp(20)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                rightMargin = dp(8)
            }
        }

        val textView = TextView(this).apply {
            text = "No puedes ingresar más de 50 caracteres"
            setTextColor(Color.BLACK)
            textSize = 14f
        }

        container.addView(iconView)
        container.addView(textView)

        Toast(this).apply {
            duration = Toast.LENGTH_SHORT
            view = container
            show()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val MAX_LENGTH = 50
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}   