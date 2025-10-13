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
import com.example.unimarket.view.explore.ExploreBuyerActivity
import com.example.unimarket.view.home.CourierHomeActivity
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

interface LoginAuthApi {
    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(@Body body: SignInBody): Response<SignInResponse>
}

@JsonClass(generateAdapter = true)
data class UserRow(val email: String?, val type: String?)

interface UsersApi {
    @GET("rest/v1/users")
    suspend fun userByEmail(
        @Query("email") emailEq: String,
        @Query("select") select: String = "type,email",
        @Header("Authorization") bearer: String,
        @Header("Range") range: String = "0-0"
    ): Response<List<UserRow>>
}

private fun buildMoshi(): Moshi =
    Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

private fun buildLoginAuthApi(): LoginAuthApi {
    val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val header = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .addHeader("apikey", SupaConst.SUPABASE_ANON_KEY)
            .addHeader("Accept", "application/json")
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
        .create(LoginAuthApi::class.java)
}

private fun buildUsersApi(): UsersApi {
    val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val apikey = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .addHeader("apikey", SupaConst.SUPABASE_ANON_KEY)
            .addHeader("Accept", "application/json")
            .build()
        chain.proceed(req)
    }
    val client = OkHttpClient.Builder()
        .addInterceptor(apikey)
        .addInterceptor(logging)
        .build()

    return Retrofit.Builder()
        .baseUrl(SupaConst.SUPABASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(buildMoshi()))
        .client(client)
        .build()
        .create(UsersApi::class.java)
}

class LoginActivity : AppCompatActivity() {

    private var passwordVisible = false
    private val authApi: LoginAuthApi by lazy { buildLoginAuthApi() }
    private val usersApi: UsersApi by lazy { buildUsersApi() }

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
            doSignIn(email, pass, btnSignIn)
        }
    }

    private fun doSignIn(email: String, pass: String, button: MaterialButton) {
        val old = button.text
        button.isEnabled = false
        button.text = "Signing in..."
        lifecycleScope.launch {
            try {
                val res = authApi.signIn(SignInBody(email, pass))
                if (!res.isSuccessful) {
                    val err = res.errorBody()?.string().orEmpty()
                    Toast.makeText(this@LoginActivity, err.ifBlank { "Credenciales inválidas (${res.code()})" }, Toast.LENGTH_LONG).show()
                    return@launch
                }
                val body = res.body()
                val token = body?.access_token
                if (token.isNullOrBlank()) {
                    Toast.makeText(this@LoginActivity, "No se recibió token del login.", Toast.LENGTH_LONG).show()
                    return@launch
                }
                val metaType = (body.user?.user_metadata?.get("type") as? String)
                val finalType = metaType ?: fetchTypeFromTable(token, email)
                val normalizedType = finalType?.trim()?.lowercase()
                when (normalizedType) {
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

    private suspend fun fetchTypeFromTable(accessToken: String, email: String): String? {
        return try {
            val r = usersApi.userByEmail(emailEq = "eq.$email", bearer = "Bearer $accessToken")
            if (r.isSuccessful) r.body()?.firstOrNull()?.type?.trim()?.lowercase() else null
        } catch (_: Throwable) { null }
    }
}