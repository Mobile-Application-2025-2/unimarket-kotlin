package com.example.unimarket.ui.auth

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
import com.example.unimarket.ui.explore.ExploreBuyerActivity
import com.example.unimarket.ui.home.CourierHomeActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

// ---------- Retrofit models & APIs ----------

@JsonClass(generateAdapter = true)
data class SignInBody(val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class SignInRespUser(
    val email: String?,
    val user_metadata: Map<String, Any>?
)

@JsonClass(generateAdapter = true)
data class SignInResponse(
    val access_token: String?,
    val token_type: String?,
    val user: SignInRespUser?
)

interface AuthApi {
    @Headers("Content-Type: application/json")
    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(@Body body: SignInBody): Response<SignInResponse>
}

@JsonClass(generateAdapter = true)
data class UserRow(val email: String?, val type: String?)

interface RestApi {
    @GET("rest/v1/users")
    suspend fun userByEmail(
        @Query("email") emailEq: String,                  // pásalo como "eq.tu@email.com"
        @Query("select") select: String = "type,email",
        @Header("Range") range: String = "0-0"
    ): Response<List<UserRow>>
}

// ---------- Builders ----------

private fun buildMoshi(): Moshi =
    Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

private fun buildAuthApi(): AuthApi {
    val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val header = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .addHeader("apikey", SupaConst.SUPABASE_ANON_KEY)
            .build()
        chain.proceed(req)
    }
    val client = OkHttpClient.Builder()
        .addInterceptor(header)
        .addInterceptor(logging)
        .build()

    return Retrofit.Builder()
        .baseUrl(SupaConst.SUPABASE_URL) // debe terminar con /
        .addConverterFactory(MoshiConverterFactory.create(buildMoshi()))
        .client(client)
        .build()
        .create(AuthApi::class.java)
}

private fun buildRestApi(): RestApi {
    val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val header = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .addHeader("apikey", SupaConst.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${SupaConst.SUPABASE_ANON_KEY}")
            .build()
        chain.proceed(req)
    }
    val client = OkHttpClient.Builder()
        .addInterceptor(header)
        .addInterceptor(logging)
        .build()

    return Retrofit.Builder()
        .baseUrl(SupaConst.SUPABASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(buildMoshi()))
        .client(client)
        .build()
        .create(RestApi::class.java)
}

// ---------- Activity ----------

class LoginActivity : AppCompatActivity() {

    private var passwordVisible = false
    private val authApi: AuthApi by lazy { buildAuthApi() }
    private val restApi: RestApi by lazy { buildRestApi() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_in)

        val btnBack   = findViewById<ImageButton>(R.id.btnBack)
        val tvSignUp  = findViewById<TextView>(R.id.tvSignUp)
        val btnSignIn = findViewById<MaterialButton>(R.id.btnSignIn)

        val tilEmail    = findViewById<TextInputLayout>(R.id.tilEmail)
        val etEmail     = findViewById<TextInputEditText>(R.id.etEmail)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val etPassword  = findViewById<TextInputEditText>(R.id.etPassword)
        val ivToggle    = findViewById<ImageView>(R.id.ivTogglePassword)

        // --- Toggle password (igual que tenías) ---
        val closedFromIv  = ivToggle.drawable
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

        // Navegación básica
        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        tvSignUp.setOnClickListener { startActivity(Intent(this, CreateAccountActivity::class.java)) }

        // Sign In
        btnSignIn.setOnClickListener {
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val pass  = etPassword.text?.toString().orEmpty()

            var ok = true
            if (email.isEmpty()) { tilEmail.error = getString(R.string.error_email_invalid); ok = false } else tilEmail.error = null
            if (pass.isEmpty())  { tilPassword.error = getString(R.string.error_password_min8); ok = false } else tilPassword.error = null
            if (!ok) return@setOnClickListener

            doSignIn(email, pass, btnSignIn)
        }
    }

    private fun doSignIn(email: String, pass: String, button: MaterialButton) {
        val old = button.text
        button.isEnabled = false
        button.text = getString(R.string.signing_in)

        lifecycleScope.launch {
            try {
                val res = authApi.signIn(SignInBody(email, pass))
                if (!res.isSuccessful) {
                    val msg = res.errorBody()?.string().orEmpty().ifBlank { "Credenciales inválidas (${res.code()})" }
                    Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_LONG).show()
                    return@launch
                }

                // 1) intentar desde user_metadata
                val metaType = (res.body()?.user?.user_metadata?.get("type") as? String)?.lowercase()

                // 2) si no viene, leer de tabla users
                val finalType = metaType ?: fetchTypeFromTable(email)

                when (finalType) {
                    "buyer" -> {
                        startActivity(Intent(this@LoginActivity, ExploreBuyerActivity::class.java))
                        finish()
                    }
                    "deliver", "delivery", "courier" -> {
                        startActivity(Intent(this@LoginActivity, CourierHomeActivity::class.java))
                        finish()
                    }
                    else -> {
                        Toast.makeText(this@LoginActivity, "No se encontró el tipo de usuario.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (t: Throwable) {
                Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
            } finally {
                button.isEnabled = true
                button.text = old
            }
        }
    }

    private suspend fun fetchTypeFromTable(email: String): String? {
        return try {
            val r = restApi.userByEmail("eq.$email")
            if (r.isSuccessful) r.body()?.firstOrNull()?.type?.lowercase() else null
        } catch (_: Throwable) { null }
    }
}