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
//import com.example.unimarket.view.home.CourierHomeActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity(), SignInViewPort {

    private lateinit var controller: SignInController

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

        val loginApi = AuthApiFactory.login(
            baseUrl = SupaConst.SUPABASE_URL,
            anonKey = SupaConst.SUPABASE_ANON_KEY,
            enableLogging = true
        )
        val usersApi = AuthApiFactory.getUsers(
            baseUrl = SupaConst.SUPABASE_URL,
            anonKey = SupaConst.SUPABASE_ANON_KEY,
            enableLogging = true
        )

        val repo = AuthRepository(
            signUpApi = null,
            loginAuthApi = loginApi,
            usersApi = usersApi
        )
        controller = SignInController(this, repo, lifecycleScope)

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
            if (pass.isEmpty())  { tilPassword.error = getString(R.string.error_password_min8); ok = false } else tilPassword.error = null
            if (!ok) return@setOnClickListener

            controller.onSignInClicked(email, pass)
        }
    }

    override fun setSubmitting(submitting: Boolean) {
        if (submitting) {
            originalBtnText = btnSignIn.text
            btnSignIn.isEnabled = false
            btnSignIn.text = "Signing in..."
        } else {
            btnSignIn.isEnabled = true
            btnSignIn.text = originalBtnText ?: getString(R.string.sign_in)
        }
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun navigateToBuyer() {
        val intent = Intent(this, com.example.unimarket.view.explore.ExploreBuyerActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    override fun navigateToCourier() {
        startActivity(Intent(this, CourierHomeActivity::class.java))
        finish()
    }
}