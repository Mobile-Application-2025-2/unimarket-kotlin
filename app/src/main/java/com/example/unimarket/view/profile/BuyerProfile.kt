package com.example.unimarket.view.profile

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.unimarket.R
import com.example.unimarket.view.auth.WelcomePage
import com.example.unimarket.view.map.BusinessMapActivity
import com.example.unimarket.viewmodel.BuyerNavDestination
import com.example.unimarket.viewmodel.BuyerViewModel
import kotlinx.coroutines.launch

class BuyerAccountActivity : AppCompatActivity() {

    private val vm: BuyerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.buyer_profile)


        val btnFavorites: ImageButton = findViewById(R.id.btnFavorites)
        val btnOrdersTop: ImageButton = findViewById(R.id.btnOrdersTop)

        val tvUserName: TextView = findViewById(R.id.tvUserName)

        val rowBusinessInfo: LinearLayout = findViewById(R.id.rowBusinessInfo)
        val rowAddresses: LinearLayout = findViewById(R.id.rowAddresses)

        val rowOrdersHeader: LinearLayout = findViewById(R.id.rowOrdersHeader)
        val rowOrder1: LinearLayout = findViewById(R.id.rowOrder1)
        val rowOrder2: LinearLayout = findViewById(R.id.rowOrder2)
        val tvSeeAll: TextView = findViewById(R.id.tvSeeAll)

        val rowProducts: LinearLayout = findViewById(R.id.rowProducts)
        val rowFavoritos: LinearLayout = findViewById(R.id.rowFavoritos)
        val rowReviews: LinearLayout = findViewById(R.id.rowReviews)
        val rowLogout: LinearLayout = findViewById(R.id.rowLogout)

        val navHome: ImageButton = findViewById(R.id.nav_home)
        val navSearch: ImageButton = findViewById(R.id.nav_search)
        val navMap: ImageButton = findViewById(R.id.nav_map)
        val navProfile: ImageButton = findViewById(R.id.nav_profile)

        btnFavorites.setOnClickListener { showFeatureUnavailableToast() }
        btnOrdersTop.setOnClickListener { showFeatureUnavailableToast() }

        rowBusinessInfo.setOnClickListener { showFeatureUnavailableToast() }
        rowAddresses.setOnClickListener { showFeatureUnavailableToast() }

        rowOrdersHeader.setOnClickListener { showFeatureUnavailableToast() }
        rowOrder1.setOnClickListener { showFeatureUnavailableToast() }
        rowOrder2.setOnClickListener { showFeatureUnavailableToast() }
        tvSeeAll.setOnClickListener { showFeatureUnavailableToast() }

        rowProducts.setOnClickListener { showFeatureUnavailableToast() }
        rowFavoritos.setOnClickListener { showFeatureUnavailableToast() }
        rowReviews.setOnClickListener { showFeatureUnavailableToast() }

        rowLogout.setOnClickListener { vm.logout() }

        navHome.setOnClickListener { finish() } // volver al Home
        navSearch.setOnClickListener { showFeatureUnavailableToast() }
        navMap.setOnClickListener { startActivity(Intent(this, BusinessMapActivity::class.java)) }
        navProfile.setOnClickListener { /* ya estás aquí */ }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.ui.collect { ui ->
                    tvUserName.text = ui.displayName

                    ui.error?.let {
                        Toast.makeText(this@BuyerAccountActivity, it, Toast.LENGTH_LONG).show()
                        vm.clearNavAndError()
                    }

                    if (ui.nav == BuyerNavDestination.ToWelcome) {
                        val intent = Intent(this@BuyerAccountActivity, WelcomePage::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                        vm.clearNavAndError()
                    }
                }
            }
        }
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