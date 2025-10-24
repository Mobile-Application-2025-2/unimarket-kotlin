package com.example.unimarket.view.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.example.unimarket.R
import com.example.unimarket.controller.auth.CreateAccountController
import com.example.unimarket.controller.auth.CreateAccountViewPort
import com.example.unimarket.databinding.ActivityCreateAccountBinding
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class CreateAccountActivity : AppCompatActivity(), CreateAccountViewPort {

    private lateinit var b: ActivityCreateAccountBinding
    private var canProceed: Boolean = false
    private lateinit var controller: CreateAccountController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        val root = findViewById<NestedScrollView>(R.id.createAccountRoot)
        b = ActivityCreateAccountBinding.bind(root)

        controller = CreateAccountController(this)

        b.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        b.btnOutlook.setOnClickListener { /* TODO: federated sign-in */ }
        b.btnGoogle.setOnClickListener  { /* TODO: Google Sign-In */ }

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
            val name     = b.etName.text?.toString()?.trim().orEmpty()
            val email    = b.etEmail.text?.toString()?.trim().orEmpty()
            val pass     = b.etPassword.text?.toString().orEmpty()
            val accepted = b.cbAccept.isChecked

            lifecycleScope.launch {
                // Firma simple (por defecto type="buyer").
                controller.onSignUpClicked(
                    name = name,
                    email = email,
                    pass = pass,
                    accepted = accepted
                )
            }
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

    override fun setSubmitting(submitting: Boolean) {
        if (submitting) {
            b.btnSignIn.isEnabled = false
            b.btnSignIn.text = getString(R.string.creating_account)
        } else {
            b.btnSignIn.isEnabled = true
            b.btnSignIn.text = getString(R.string.creating_account)
        }
    }

    override fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun navigateToStudentCode() {
        startActivity(Intent(this, StudentCodeActivity::class.java))
        finish()
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}