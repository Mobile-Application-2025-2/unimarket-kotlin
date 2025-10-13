package com.example.unimarket.view.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.example.unimarket.SupaConst
import com.example.unimarket.R
import com.example.unimarket.databinding.ActivityCreateAccountBinding
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class SignUpBody(
    val email: String,
    val password: String,
    val data: Map<String, String>
)

interface AuthApi {
    @Headers("Content-Type: application/json")
    @POST("auth/v1/signup")
    suspend fun signUp(@Body body: SignUpBody): Response<Unit>
}

private fun buildAuthApi(): AuthApi {
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val apikeyInterceptor = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .addHeader("apikey", SupaConst.SUPABASE_ANON_KEY)
            .build()
        chain.proceed(req)
    }

    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    val client = OkHttpClient.Builder()
        .addInterceptor(apikeyInterceptor)
        .addInterceptor(logging)
        .build()

    return Retrofit.Builder()
        .baseUrl(SupaConst.SUPABASE_URL) // debe terminar en /
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(client)
        .build()
        .create(AuthApi::class.java)
}
class CreateAccountActivity : AppCompatActivity() {

    private lateinit var b: ActivityCreateAccountBinding
    private var canProceed: Boolean = false
    private val authApi: AuthApi by lazy { buildAuthApi() }

    private val DEFAULT_USER_TYPE = "buyer"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)
        val root = findViewById<NestedScrollView>(R.id.createAccountRoot)
        b = ActivityCreateAccountBinding.bind(root)

        b.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        b.btnOutlook.setOnClickListener { /* TODO */ }
        b.btnGoogle.setOnClickListener  { /* TODO  */ }

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
            doSignUp()
        }
    }

    private fun doSignUp() {
        val name  = b.etName.text?.toString()?.trim().orEmpty()
        val email = b.etEmail.text?.toString()?.trim().orEmpty()
        val pass  = b.etPassword.text?.toString().orEmpty()

        // feedback UI
        val oldText = b.btnSignIn.text
        b.btnSignIn.isEnabled = false
        b.btnSignIn.text = getString(R.string.creating_account)

        lifecycleScope.launch {
            try {
                val body = SignUpBody(
                    email = email,
                    password = pass,
                    data = mapOf(
                        "name" to name,
                        "type" to DEFAULT_USER_TYPE,
                        "id_type" to "id",
                        "id_number" to ""
                    )
                )
                val res = authApi.signUp(body)

                if (res.isSuccessful) {

                    Toast.makeText(this@CreateAccountActivity, getString(R.string.account_created), Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@CreateAccountActivity, StudentCodeActivity::class.java))
                    finish()
                } else {
                    val err = res.errorBody()?.string().orEmpty()
                    val msg = err.ifBlank { "Sign-up falló (HTTP ${res.code()})" }
                    Toast.makeText(this@CreateAccountActivity, msg, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                android.util.Log.d("NET", "BASE_URL='${SupaConst.SUPABASE_URL}' length=${SupaConst.SUPABASE_URL.length}")
                Toast.makeText(this@CreateAccountActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
            } finally {
                b.btnSignIn.isEnabled = true
                b.btnSignIn.text = oldText
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

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
