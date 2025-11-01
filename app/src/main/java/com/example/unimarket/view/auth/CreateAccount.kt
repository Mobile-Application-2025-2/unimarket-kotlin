package com.example.unimarket.view.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.widget.ArrayAdapter
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
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.MaterialAutoCompleteTextView
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
        val root = findViewById<NestedScrollView>(R.id.createAccountRoot)
        b = ActivityCreateAccountBinding.bind(root)

        // Restaurar selección si hay estado previo
        selectedTypeId = (savedInstanceState?.getString(STATE_TYPE_ID) ?: "buyer").lowercase()

        // Back
        b.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Botones federados (TODO)
        b.btnOutlook.setOnClickListener { /* TODO: federated sign-in */ }
        b.btnGoogle.setOnClickListener  { /* TODO: Google Sign-In */ }

        // Inputs -> VM (normalizados)
        b.etName.doAfterTextChanged {
            val nameNorm = normalize(it?.toString())
            viewModel.create_onNameChanged(nameNorm)
            refreshLocalValidation()
        }
        b.etEmail.doAfterTextChanged {
            val emailNorm = normalize(it?.toString())
            viewModel.create_onEmailChanged(emailNorm)
            refreshLocalValidation()
        }
        b.etPassword.doAfterTextChanged {
            // la contraseña NO se normaliza
            viewModel.create_onPasswordChanged(it?.toString().orEmpty())
            refreshLocalValidation()
        }
        b.cbAccept.setOnCheckedChangeListener { _, checked ->
            viewModel.create_onPolicyToggled(checked)
            refreshLocalValidation()
        }

        // Ojo de contraseña
        setupPasswordToggle()

        // Dropdown de tipo de cuenta (id ya viene en minúsculas)
        setupAccountTypeDropdown()

        // Estado inicial de controles
        refreshLocalValidation()

        // Crear cuenta (envía el type normalizado)
        b.btnSignIn.setOnClickListener {
            if (!canProceed) {
                showInlineErrors()
                Toast.makeText(this, getString(R.string.complete_the_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.create_submit(type = selectedTypeId.lowercase())
        }

        // Observa estado del VM
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.create.collect { ui ->
                    // Errores de validación del VM
                    b.tilName.error = ui.nameError
                    b.tilEmail.error = ui.emailError
                    b.tilPassword.error = ui.passwordError
                    ui.acceptedPolicyError?.let {
                        Toast.makeText(this@CreateAccountActivity, it, Toast.LENGTH_SHORT).show()
                    }

                    // Loading / botón
                    if (ui.isSubmitting) {
                        b.btnSignIn.isEnabled = false
                        b.btnSignIn.text = getString(R.string.creating_account)
                        b.btnSignIn.alpha = 0.6f
                    } else {
                        refreshLocalValidation()
                        b.btnSignIn.text = getString(R.string.action_sign_up)
                    }

                    // Toast one-shot
                    ui.toastMessage?.let { msg ->
                        Toast.makeText(this@CreateAccountActivity, msg, Toast.LENGTH_LONG).show()
                        viewModel.create_clearNavAndToast()
                    }

                    // Navegación
                    when (ui.nav) {
                        AuthNavDestination.ToStudentCode -> {
                            startActivity(Intent(this@CreateAccountActivity, StudentCodeActivity::class.java))
                            finish()
                            viewModel.create_clearNavAndToast()
                        }
                        else -> Unit
                    }
                }
            }
        }
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

        // Setear texto inicial según selectedTypeId
        val initial = accountTypes.firstOrNull { it.id == selectedTypeId } ?: accountTypes.first()
        b.actAccountType.setText(initial.label, false)

        // Cambios de selección
        b.actAccountType.setOnItemClickListener { _, _, position, _ ->
            val chosen = accountTypes[position]
            selectedTypeId = chosen.id.lowercase()
            // Si quisieras avisar al VM inmediatamente:
            // viewModel.create_onAccountTypeChanged(selectedTypeId)
        }

        // Abrir menú al tocar el contenedor también
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

        if (validName)  b.tilName.error = null
        if (validEmail) b.tilEmail.error = null
        if (validPass)  b.tilPassword.error = null
    }

    private fun showInlineErrors() {
        val nameNorm  = normalize(b.etName.text?.toString())
        val emailNorm = normalize(b.etEmail.text?.toString())
        val pass      = b.etPassword.text?.toString().orEmpty()

        if (nameNorm.length < 3) { b.tilName.error = getString(R.string.error_name_min3); b.etName.requestFocus() } else b.tilName.error = null
        if (!EMAIL_REGEX.matches(emailNorm)) { b.tilEmail.error = getString(R.string.error_email_invalid); if (b.tilName.error == null) b.etEmail.requestFocus() } else b.tilEmail.error = null
        if (pass.length < 8) { b.tilPassword.error = getString(R.string.error_password_min8); if (b.tilName.error == null && b.tilEmail.error == null) b.etPassword.requestFocus() } else b.tilPassword.error = null
        if (!b.cbAccept.isChecked) Toast.makeText(this, getString(R.string.accept_privacy), Toast.LENGTH_SHORT).show()
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
    }
}