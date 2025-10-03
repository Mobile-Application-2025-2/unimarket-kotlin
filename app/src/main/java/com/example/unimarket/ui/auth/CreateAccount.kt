package com.example.unimarket.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.unimarket.databinding.ActivityCreateAccountBinding
import com.google.android.material.textfield.TextInputLayout
import android.view.ViewGroup

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var b: ActivityCreateAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCreateAccountBinding.inflate(layoutInflater)
        (b.root.parent as? ViewGroup)?.removeView(b.root)

        setContentView(b.root)

        // ---- Navegación back
        b.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // ---- Clicks de los “botones” sociales (solo UI; sin lógica)
        b.btnOutlook.setOnClickListener { /* TODO: flujo social más adelante */ }
        b.btnGoogle.setOnClickListener  { /* TODO: flujo social más adelante */ }

        // ---- Reactividad: cuando el usuario escribe o marca el checkbox, reevaluamos
        b.etName.doAfterTextChanged   { refreshState() }
        b.etEmail.doAfterTextChanged  { refreshState() }
        b.etPassword.doAfterTextChanged { refreshState() }
        b.cbAccept.setOnCheckedChangeListener { _, _ -> refreshState() }

        // Estado inicial
        refreshState()

        // ---- Acción principal (solo front por ahora)
        b.btnSignIn.setOnClickListener {
            if (!b.btnSignIn.isEnabled) return@setOnClickListener
            val name  = b.etName.text?.toString()?.trim().orEmpty()
            val email = b.etEmail.text?.toString()?.trim().orEmpty()
            val pass  = b.etPassword.text?.toString().orEmpty()

            // Aquí conectarás ViewModel/backend; por ahora, feedback simple:
            Toast.makeText(this, "Crear cuenta: $name / $email", Toast.LENGTH_SHORT).show()
            // TODO: navega a la siguiente pantalla
        }
    }

    private fun refreshState() {
        val name  = b.etName.text?.toString()?.trim().orEmpty()
        val email = b.etEmail.text?.toString()?.trim().orEmpty()
        val pass  = b.etPassword.text?.toString().orEmpty()

        val validName  = name.length >= 3
        val validEmail = EMAIL_REGEX.matches(email)
        val validPass  = pass.length >= 8

        // Muestra/oculta el check de fin de campo (tu XML ya define endIconMode y drawable)
        b.tilName.setEndIconVisibleCompat(validName)
        b.tilEmail.setEndIconVisibleCompat(validEmail)

        b.btnSignIn.isEnabled = validName && validEmail && validPass && b.cbAccept.isChecked
    }

    private fun TextInputLayout.setEndIconVisibleCompat(visible: Boolean) {
        // Material Components 1.6+ tiene isEndIconVisible; si tu versión no lo expone,
        // igual funciona porque el endIcon ya está ahí; no pasa nada si no cambia.
        try {
            this.isEndIconVisible = visible
        } catch (_: Throwable) { /* ignora si la versión no lo soporta */ }
    }

    companion object {
        private val EMAIL_REGEX =
            Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
    }
}