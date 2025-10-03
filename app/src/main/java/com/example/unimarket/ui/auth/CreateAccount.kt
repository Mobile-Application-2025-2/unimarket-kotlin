package com.example.unimarket.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.unimarket.R
import com.example.unimarket.databinding.ActivityCreateAccountBinding
import com.google.android.material.textfield.TextInputLayout
import androidx.core.widget.NestedScrollView
import com.example.unimarket.ui.auth.StudentCodeActivity

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var b: ActivityCreateAccountBinding
    private var canProceed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_account)
        val root = findViewById<NestedScrollView>(R.id.createAccountRoot)
        b = ActivityCreateAccountBinding.bind(root)

        b.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        b.btnOutlook.setOnClickListener { /* TODO social */ }
        b.btnGoogle.setOnClickListener  { /* TODO social */ }

        b.etName.doAfterTextChanged     { refreshState() }
        b.etEmail.doAfterTextChanged    { refreshState() }
        b.etPassword.doAfterTextChanged { refreshState() }
        b.cbAccept.setOnCheckedChangeListener { _, _ -> refreshState() }

        refreshState()

        b.btnSignIn.setOnClickListener {
            if (!canProceed) {
                showInlineErrors()
                Toast.makeText(this, getString(R.string.complete_the_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, StudentCodeActivity::class.java))
        }
    }

    private fun refreshState() {
        val name  = b.etName.text?.toString()?.trim().orEmpty()
        val email = b.etEmail.text?.toString()?.trim().orEmpty()
        val pass  = b.etPassword.text?.toString().orEmpty()

        val validName  = name.length >= 3
        val validEmail = EMAIL_REGEX.matches(email)
        val validPass  = pass.length >= 8
        val accepted   = b.cbAccept.isChecked

        b.tilName.setEndIconVisibleCompat(validName)
        b.tilEmail.setEndIconVisibleCompat(validEmail)

        canProceed = validName && validEmail && validPass && accepted

        // el botón siempre clickeable; solo feedback visual
        b.btnSignIn.isEnabled = true
        b.btnSignIn.alpha = if (canProceed) 1f else 0.6f

        if (validName)  b.tilName.error = null
        if (validEmail) b.tilEmail.error = null
        if (validPass)  b.tilPassword.error = null
    }

    private fun showInlineErrors() {
        val name  = b.etName.text?.toString()?.trim().orEmpty()
        val email = b.etEmail.text?.toString()?.trim().orEmpty()
        val pass  = b.etPassword.text?.toString().orEmpty()

        if (name.length < 3) {
            b.tilName.error = getString(R.string.error_name_min3)
            b.etName.requestFocus()
        } else b.tilName.error = null

        if (!EMAIL_REGEX.matches(email)) {
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
        try { isEndIconVisible = visible } catch (_: Throwable) { }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
