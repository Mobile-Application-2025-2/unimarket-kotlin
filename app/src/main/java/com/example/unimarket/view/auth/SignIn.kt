package com.example.unimarket.view.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.unimarket.R
import com.example.unimarket.SupaConst
import com.example.unimarket.controller.auth.SignInController
import com.example.unimarket.controller.auth.SignInViewPort
import com.example.unimarket.model.api.AuthApiFactory
import com.example.unimarket.model.repository.AuthRepository
import com.example.unimarket.view.explore.ExploreBuyerActivity
import com.example.unimarket.view.home.CourierHomeActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity(), SignInViewPort {

    private var passwordVisible = false
    private lateinit var controller: SignInController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_in)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val btnSignIn = findViewById<MaterialButton>(R.id.btnSignIn)

        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val ivToggle = findViewById<ImageView>(R.id.ivTogglePassword)

        val loginApi = AuthApiFactory.login(SupaConst.SUPABASE_URL, SupaConst.SUPABASE_ANON_KEY, enableLogging = true)
        val usersApi = AuthApiFactory.getUsers(SupaConst.SUPABASE_URL, SupaConst.SUPABASE_ANON_KEY, enableLogging = true)
        val repo = AuthRepository(loginApi, usersApi)
        controller = SignInController(this, repo)

        val closedFromIv = ivToggle.drawable
        val closedFromTil = tilPassword.endIconDrawable
        fun renderPasswordUi() {
            etPassword.transformationMethod =
                if (passwordVisible) null else PasswordTransformationMethod.getInstance()
            etPassword.setSelection(etPassword.text?.length ?: 0)
            if (ivToggle != null) {
                ivToggle.setImageDrawable(
                    if (passwordVisible)
                        ContextCompat.getDrawable(this, R.drawable.closed)
                    else
                        closedFromIv ?: ContextCompat.getDrawable(this, R.drawable.open)
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

        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        tvSignUp.setOnClickListener { startActivity(Intent(this, CreateAccountActivity::class.java)) }

        btnSignIn.setOnClickListener {
            val email = etEmail.text?.toString()?.trim()?.lowercase().orEmpty()
            val pass = etPassword.text?.toString().orEmpty()
            var ok = true
            if (email.isEmpty()) { tilEmail.error = getString(R.string.error_email_invalid); ok = false } else tilEmail.error = null
            if (pass.isEmpty()) { tilPassword.error = getString(R.string.error_password_min8); ok = false } else tilPassword.error = null
            if (!ok) return@setOnClickListener

            lifecycleScope.launch { controller.onSignInClicked(email, pass) }
        }
    }

    override fun setSubmitting(submitting: Boolean) {
        val btnSignIn = findViewById<MaterialButton>(R.id.btnSignIn)
        if (submitting) {
            btnSignIn.isEnabled = false
            btnSignIn.text = "Signing in..."
        } else {
            btnSignIn.isEnabled = true
            btnSignIn.text = getString(R.string.sign_in)
        }
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun navigateToBuyer() {
        startActivity(Intent(this, ExploreBuyerActivity::class.java))
        finish()
    }

    override fun navigateToCourier() {
        startActivity(Intent(this, CourierHomeActivity::class.java))
        finish()
    }
}