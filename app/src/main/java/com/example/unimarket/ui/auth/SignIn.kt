package com.example.unimarket.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.unimarket.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_in)

        val btnBack   = findViewById<ImageButton>(R.id.btnBack)
        val tvSignUp  = findViewById<TextView>(R.id.tvSignUp)
        val btnSignIn = findViewById<MaterialButton>(R.id.btnSignIn)

        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val etPassword  = findViewById<TextInputEditText>(R.id.etPassword)
        val ivToggle    = findViewById<ImageView>(R.id.ivTogglePassword)

        val closedFromIv  = ivToggle?.drawable
        val closedFromTil = tilPassword?.endIconDrawable

        fun renderPasswordUi() {
            etPassword?.transformationMethod =
                if (passwordVisible) null else PasswordTransformationMethod.getInstance()
            etPassword?.setSelection(etPassword?.text?.length ?: 0)

            if (ivToggle != null) {
                ivToggle.setImageDrawable(
                    if (passwordVisible)
                        ContextCompat.getDrawable(this, R.drawable.closed)
                    else
                        closedFromIv ?: ContextCompat.getDrawable(this, R.drawable.open)
                )
                ivToggle.imageTintList = null
            } else if (tilPassword != null) {
                tilPassword.endIconMode = TextInputLayout.END_ICON_CUSTOM
                tilPassword.setEndIconTintList(null)
                tilPassword.endIconDrawable =
                    if (passwordVisible)
                        ContextCompat.getDrawable(this, R.drawable.open)
                    else
                        closedFromTil ?: ContextCompat.getDrawable(this, R.drawable.closed)
            }
        }

        ivToggle?.setOnClickListener {
            passwordVisible = !passwordVisible
            renderPasswordUi()
        }
        if (ivToggle == null && tilPassword != null) {
            tilPassword.setEndIconOnClickListener {
                passwordVisible = !passwordVisible
                renderPasswordUi()
            }
        }

        passwordVisible = false
        renderPasswordUi()

        btnBack?.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        tvSignUp?.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }
        btnSignIn?.setOnClickListener {
            // TODO: aquí la lógica real de login
        }
    }
}