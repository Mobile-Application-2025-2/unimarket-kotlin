package com.example.unimarket.view.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.unimarket.R
import com.example.unimarket.databinding.ActivityCreateAccountBinding
import com.example.unimarket.viewmodel.AuthNavDestination
import com.example.unimarket.viewmodel.AuthViewModel
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var b: ActivityCreateAccountBinding
    private val viewModel: AuthViewModel by viewModels()
    private var canProceed: Boolean = false

    // Estado del ojo
    private var isPasswordVisible = false

    // ---- Dropdown: tipos de cuenta ----
    private data class AccountType(val id: String, val label: String)
    private val accountTypes = listOf(
        AccountType(id = "buyer",    label = "Buyer"),
        AccountType(id = "business", label = "Business")
    )
    private var selectedTypeId: String = "buyer" // default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_account)
        val root = findViewById<androidx.core.widget.NestedScrollView>(R.id.createAccountRoot)
        b = com.example.unimarket.databinding.ActivityCreateAccountBinding.bind(root)

        // Límite 50 chars + validación local
        attachMaxLengthBehavior(b.etName)
        attachMaxLengthBehavior(b.etEmail)
        attachMaxLengthBehavior(b.etPassword)

        // Restaurar tipo si venimos de recreación
        selectedTypeId = (savedInstanceState?.getString(STATE_TYPE_ID) ?: "buyer").lowercase()

        // Back
        b.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Botones federados (placeholder)
        b.btnOutlook.setOnClickListener { showFeatureUnavailableToast() }
        b.btnGoogle.setOnClickListener  { showFeatureUnavailableToast() }

        // Inputs -> VM (esto persiste el borrador en DataStore mediante el VM)
        b.etName.doAfterTextChanged {
            val v = it?.toString()?.trim()?.lowercase().orEmpty()
            viewModel.create_onNameChanged(v)
            if (v.length >= 3) b.tilName.error = null
            refreshLocalValidation()
        }
        b.etEmail.doAfterTextChanged {
            val v = it?.toString()?.trim()?.lowercase().orEmpty()
            viewModel.create_onEmailChanged(v)
            if (EMAIL_REGEX.matches(v)) b.tilEmail.error = null
            refreshLocalValidation()
        }
        b.etPassword.doAfterTextChanged {
            val v = it?.toString().orEmpty()
            viewModel.create_onPasswordChanged(v)
            if (v.length >= 8) b.tilPassword.error = null
            refreshLocalValidation()
        }
        b.cbAccept.setOnCheckedChangeListener { _, checked ->
            viewModel.create_onPolicyToggled(checked)
            refreshLocalValidation()
        }

        // Ojo de contraseña + dropdown
        setupPasswordToggle()
        setupAccountTypeDropdown()

        // Estado inicial
        refreshLocalValidation()

        // Crear cuenta
        b.btnSignIn.setOnClickListener {
            val nameNorm  = b.etName.text?.toString()?.trim().orEmpty()
            val emailNorm = b.etEmail.text?.toString()?.trim()?.lowercase().orEmpty()
            val pass      = b.etPassword.text?.toString().orEmpty()
            val accepted  = b.cbAccept.isChecked

            val ok = nameNorm.length >= 3 &&
                    EMAIL_REGEX.matches(emailNorm) &&
                    pass.length >= 8 &&
                    accepted

            if (!ok) {
                showInlineErrors()
                android.widget.Toast.makeText(
                    this, getString(R.string.complete_the_fields), android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (!isOnline()) {
                // 🚫 Sin internet: muestra aviso y NO intenta crear la cuenta
                com.google.android.material.snackbar.Snackbar
                    .make(root, "No hay conexión. No se puede crear la cuenta.", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                    .setAction("OK") { /* no-op */ }
                    .show()
                return@setOnClickListener
            }

            // Online → flujo normal
            viewModel.create_submit(type = selectedTypeId.lowercase())
        }

        // Observa estado
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.create.collect { ui ->
                    val nameNow  = b.etName.text?.toString()?.trim().orEmpty()
                    val emailNow = b.etEmail.text?.toString()?.trim()?.lowercase().orEmpty()
                    val passNow  = b.etPassword.text?.toString().orEmpty()

                    val validName  = nameNow.length >= 3
                    val validEmail = EMAIL_REGEX.matches(emailNow)
                    val validPass  = passNow.length >= 8

                    b.tilName.error     = if (!validName)  ui.nameError else null
                    b.tilEmail.error    = if (!validEmail) ui.emailError else null
                    b.tilPassword.error = if (!validPass)  ui.passwordError else null

                    ui.acceptedPolicyError?.let {
                        android.widget.Toast.makeText(this@CreateAccountActivity, it, android.widget.Toast.LENGTH_SHORT).show()
                    }

                    if (ui.isSubmitting) {
                        b.btnSignIn.isEnabled = false
                        b.btnSignIn.text = getString(R.string.creating_account)
                        b.btnSignIn.alpha = 0.6f
                    } else {
                        refreshLocalValidation()
                        b.btnSignIn.text = getString(R.string.action_sign_up)
                    }

                    ui.toastMessage?.let { msg ->
                        android.widget.Toast.makeText(this@CreateAccountActivity, msg, android.widget.Toast.LENGTH_LONG).show()
                        viewModel.create_clearNavAndToast()
                    }

                    when (ui.nav) {
                        com.example.unimarket.viewmodel.AuthNavDestination.ToLogin -> {
                            startActivity(Intent(this@CreateAccountActivity, com.example.unimarket.view.auth.LoginActivity::class.java))
                            finish()
                            viewModel.create_clearNavAndToast()
                        }
                        com.example.unimarket.viewmodel.AuthNavDestination.ToStudentCode -> {
                            startActivity(Intent(this@CreateAccountActivity, com.example.unimarket.view.auth.StudentCodeActivity::class.java))
                            finish()
                            viewModel.create_clearNavAndToast()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    // --- Límite 50 caracteres + toast ---
    private fun attachMaxLengthBehavior(editText: EditText) {
        val prevFilters = editText.filters
        editText.filters = prevFilters + InputFilter.LengthFilter(MAX_LENGTH)

        editText.addTextChangedListener(object : TextWatcher {
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

    // --- Ojo de contraseña con imágenes closed/open ---
    private fun setupPasswordToggle() {
        b.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        b.etPassword.setSelection(b.etPassword.text?.length ?: 0)

        b.ivTogglePassword.setImageResource(R.drawable.closed)

        b.ivTogglePassword.setOnClickListener {
            val start = b.etPassword.selectionStart
            val end = b.etPassword.selectionEnd

            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                b.etPassword.transformationMethod = null
                val openId = resources.getIdentifier("open", "drawable", packageName)
                if (openId != 0) b.ivTogglePassword.setImageResource(openId)
                else b.ivTogglePassword.setImageResource(R.drawable.closed)
            } else {
                b.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                b.ivTogglePassword.setImageResource(R.drawable.closed)
            }
            b.etPassword.setSelection(start, end)
        }
    }

    // --- Dropdown account type ---
    private fun setupAccountTypeDropdown() {
        val labels = accountTypes.map { it.label }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
        (b.actAccountType as MaterialAutoCompleteTextView).setAdapter(adapter)

        val initial = accountTypes.firstOrNull { it.id == selectedTypeId } ?: accountTypes.first()
        b.actAccountType.setText(initial.label, false)

        b.actAccountType.setOnItemClickListener { _, _, position, _ ->
            val chosen = accountTypes[position]
            selectedTypeId = chosen.id.lowercase()
        }

        b.tilAccountType.setOnClickListener {
            (b.actAccountType as MaterialAutoCompleteTextView).showDropDown()
        }
    }

    private fun refreshLocalValidation() {
        val nameNorm  = normalize(b.etName.text?.toString())
        val emailNorm = normalize(b.etEmail.text?.toString())
        val pass      = b.etPassword.text?.toString().orEmpty()
        val accepted  = b.cbAccept.isChecked

        val validName  = nameNorm.length >= 3
        val validEmail = EMAIL_REGEX.matches(emailNorm)
        val validPass  = pass.length >= 8

        canProceed = validName && validEmail && validPass && accepted

        b.tilName.setEndIconVisibleCompat(validName)
        b.tilEmail.setEndIconVisibleCompat(validEmail)

        b.btnSignIn.isEnabled = true
        b.btnSignIn.alpha = if (canProceed) 1f else 0.6f
    }

    private fun showInlineErrors() {
        val nameNorm  = normalize(b.etName.text?.toString())
        val emailNorm = normalize(b.etEmail.text?.toString())
        val pass      = b.etPassword.text?.toString().orEmpty()

        if (nameNorm.length < 3) {
            b.tilName.error = getString(R.string.error_name_min3)
            b.etName.requestFocus()
        } else b.tilName.error = null

        if (!EMAIL_REGEX.matches(emailNorm)) {
            b.tilEmail.error = getString(R.string.error_email_invalid)
            if (b.tilName.error == null) b.etEmail.requestFocus()
        } else b.tilEmail.error = null

        if (pass.length < 8) {
            b.tilPassword.error = getString(R.string.error_password_min8)
            if (b.tilName.error == null && b.tilEmail.error == null) b.etPassword.requestFocus()
        } else b.tilPassword.error = null

        if (!b.cbAccept.isChecked) {
            Toast.makeText(this, getString(R.string.accept_privacy), Toast.LENGTH_SHORT).show()
        }
    }

    private fun TextInputLayout.setEndIconVisibleCompat(visible: Boolean) {
        try { isEndIconVisible = visible } catch (_: Throwable) { /* no-op */ }
    }

    private fun normalize(raw: String?): String = raw?.trim()?.lowercase().orEmpty()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_TYPE_ID, selectedTypeId.lowercase())
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private const val STATE_TYPE_ID = "state_type_id"
        private const val MAX_LENGTH = 50
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

    private fun isOnline(): Boolean {
        val cm = getSystemService(android.net.ConnectivityManager::class.java)
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}