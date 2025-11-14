package com.example.unimarket.view.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.unimarket.R
import com.example.unimarket.databinding.BusinessProfileBinding
import com.example.unimarket.view.auth.WelcomePage
import com.example.unimarket.viewmodel.BusinessNavDestination
import com.example.unimarket.viewmodel.BusinessViewModel
import kotlinx.coroutines.launch
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color

class BusinessAccountActivity : AppCompatActivity() {

    private lateinit var b: BusinessProfileBinding
    private val vm: BusinessViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = BusinessProfileBinding.inflate(layoutInflater)
        setContentView(b.root)

        intent.getStringExtra(EXTRA_USER_NAME)?.let { name ->
            if (name.isNotBlank()) b.tvUserName.text = name
        }

        fun ping(msg: String) =
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

        b.brandBar.setOnClickListener { showFeatureUnavailableToast() }

        b.rowBusinessInfo.setOnClickListener { showFeatureUnavailableToast() }
        b.rowAddresses.setOnClickListener   { showFeatureUnavailableToast() }

        b.rowOrdersHeader.setOnClickListener { showFeatureUnavailableToast() }
        b.rowOrder1.setOnClickListener       { showFeatureUnavailableToast() }
        b.rowOrder2.setOnClickListener       { showFeatureUnavailableToast() }
        b.tvSeeAll.setOnClickListener        { showFeatureUnavailableToast() }

        b.rowProducts.setOnClickListener { ping("Productos") }
        b.rowReviews.setOnClickListener  { showFeatureUnavailableToast() }

        b.rowLogout.setOnClickListener { vm.logout() }

        // Footer
        b.navHome.setOnClickListener    { showFeatureUnavailableToast() }
        b.navBag.setOnClickListener     { showFeatureUnavailableToast() }
        b.navProfile.setOnClickListener { /* ya estás aquí */ }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.ui.collect { ui ->
                    // Nombre visible (si no fue override por Intent)
                    if (intent.getStringExtra(EXTRA_USER_NAME).isNullOrBlank()) {
                        b.tvUserName.text = ui.displayName
                    }

                    ui.error?.let {
                        Toast.makeText(this@BusinessAccountActivity, it, Toast.LENGTH_LONG).show()
                        vm.clearNavAndError()
                    }

                    if (ui.nav == BusinessNavDestination.ToWelcome) {
                        val intent = Intent(this@BusinessAccountActivity, WelcomePage::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                        vm.clearNavAndError()
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_USER_NAME = "extra.USER_NAME"
    }
    private fun showFeatureUnavailableToast() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.parseColor("#FFFFFF"))
        }

        val iconView = ImageView(this).apply {
            // usa un drawable que ya tengas; aquí reutilizo el del header
            setImageResource(R.drawable.personajesingup)
            val size = dp(20)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                rightMargin = dp(8)
            }
        }

        val textView = TextView(this).apply {
            text = "Esta opción aún no está habilitada"
            setTextColor(Color.BLACK)
            textSize = 14f
        }

        container.addView(iconView)
        container.addView(textView)

        Toast(this).apply {
            duration = Toast.LENGTH_SHORT
            view = container
            show()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}