package com.example.unimarket.view.home

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.unimarket.R
import com.example.unimarket.view.map.BusinessMapActivity
import com.example.unimarket.view.profile.BuyerAccountActivity
import com.example.unimarket.view.profile.CartActivity
import com.example.unimarket.viewmodel.CartViewModel
import com.example.unimarket.viewmodel.ProductDetailUiState
import com.example.unimarket.viewmodel.ProductDetailViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Locale

class ProductDetailActivity : AppCompatActivity() {

    private val viewModel: ProductDetailViewModel by viewModels()
    private val cartViewModel: CartViewModel by viewModels()

    // Header
    private lateinit var btnFavorites: ImageButton
    private lateinit var btnOrdersTop: ImageButton

    // Imagen y back
    private lateinit var imgProduct: ImageView
    private lateinit var btnBack: ImageButton

    // Info producto
    private lateinit var tvProductName: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvTime: TextView

    // Cantidad y botón ADD
    private lateinit var btnMinus: ImageButton
    private lateinit var btnPlus: ImageButton
    private lateinit var tvQuantity: TextView
    private lateinit var addButtonCard: MaterialCardView

    private lateinit var btnRateProduct: MaterialButton
    private lateinit var tvAdd: TextView
    private lateinit var tvAddPrice: TextView

    // Bottom nav
    private lateinit var navHome: ImageButton
    private lateinit var navSearch: ImageButton
    private lateinit var navMap: ImageButton
    private lateinit var navProfile: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.product_detail)

        initViews()
        setupClicks()
        observeUi()

        val productId = intent.getStringExtra(EXTRA_PRODUCT_ID).orEmpty()
        if (productId.isNotBlank()) {
            viewModel.loadProduct(productId)
        } else {
            Toast.makeText(this, "Producto inválido", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initViews() {
        btnFavorites = findViewById(R.id.btnFavorites)
        btnOrdersTop = findViewById(R.id.btnOrdersTop)

        imgProduct = findViewById(R.id.imgProduct)
        btnBack    = findViewById(R.id.btnBack)

        tvProductName = findViewById(R.id.tvProductName)
        tvPrice       = findViewById(R.id.tvPrice)
        tvDescription = findViewById(R.id.tvDescription)
        tvRating      = findViewById(R.id.tvRating)
        tvTime        = findViewById(R.id.tvTime)

        btnMinus   = findViewById(R.id.btnMinus)
        btnPlus    = findViewById(R.id.btnPlus)
        tvQuantity = findViewById(R.id.tvQuantity)

        addButtonCard = findViewById(R.id.addButtonCard)
        tvAdd         = findViewById(R.id.tvAdd)
        tvAddPrice    = findViewById(R.id.tvAddPrice)

        navHome    = findViewById(R.id.nav_home)
        navSearch  = findViewById(R.id.nav_search)
        navMap     = findViewById(R.id.nav_map)
        navProfile = findViewById(R.id.nav_profile)
        btnRateProduct = findViewById(R.id.btnRate)
    }

    private fun setupClicks() {
        // Back
        btnBack.setOnClickListener { finish() }

        btnFavorites.setOnClickListener {
            Toast.makeText(this, "Favoritos aún no disponible", Toast.LENGTH_SHORT).show()
        }

        btnOrdersTop.setOnClickListener {
            startActivity(Intent(this@ProductDetailActivity, CartActivity::class.java))
        }

        // Cantidad manejada por el ViewModel
        btnMinus.setOnClickListener {
            viewModel.decrementQuantity()
        }

        btnPlus.setOnClickListener {
            viewModel.incrementQuantity()
        }

        // ADD → agrega al carrito usando CartViewModel
        addButtonCard.setOnClickListener {
            val ui = viewModel.uiState.value
            if (ui.productId.isBlank()) {
                Toast.makeText(this, "Producto inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            cartViewModel.addProduct(ui.productId, ui.quantity)

            Toast.makeText(
                this,
                "Se agregaron ${ui.quantity} al carrito",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnRateProduct.setOnClickListener {
            showRateDialog()
        }

        // Bottom nav
        navHome.setOnClickListener {
            goToRoot(HomeBuyerActivity::class.java)
        }

        navSearch.setOnClickListener {
            goToRoot(ExploreBuyerActivity::class.java)
        }

        navMap.setOnClickListener {
            goToRoot(BusinessMapActivity::class.java)
        }

        navProfile.setOnClickListener {
            goToRoot(BuyerAccountActivity::class.java)
        }
    }

    private fun goToRoot(target: Class<*>) {
        if (this::class.java == target) return

        val intent = Intent(this, target).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        startActivity(intent)
        finish()
    }

    private fun observeUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { ui ->
                    renderUi(ui)
                }
            }
        }
    }

    private fun renderUi(ui: ProductDetailUiState) {
        ui.error?.let { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            viewModel.errorShown()
        }

        tvProductName.text = ui.name
        tvDescription.text = ui.description

        tvRating.text = if (ui.rating > 0.0) {
            String.format(Locale.getDefault(), "%.1f", ui.rating)
        } else {
            "—"
        }

        tvTime.text = ui.time

        tvPrice.text = formatPrice(ui.price)
        tvQuantity.text = ui.quantity.toString()
        tvAddPrice.text = formatPrice(ui.price * ui.quantity)

        if (ui.imageUrl.isNotBlank()) {
            imgProduct.load(ui.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.personajesingup)
                error(R.drawable.personajesingup)
            }
        }
    }

    private fun formatPrice(value: Double): String {
        return if (value > 0.0) {
            String.format("$%,.0f", value)
        } else {
            "$0"
        }
    }

    // --- Rating ---

    private fun showRateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rate_business, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)

        MaterialAlertDialogBuilder(this)
            .setTitle("Califica el producto")
            .setView(dialogView)
            .setPositiveButton("Enviar") { _, _ ->
                val value = ratingBar.rating.toInt().coerceIn(1, 5)
                onRatingSubmitted(value)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun onRatingSubmitted(newValue: Int) {
        if (newValue <= 0) return

        val ui = viewModel.uiState.value

        val x = ui.ratingCount          // cantidad actual de calificaciones
        val y = ui.rating               // rating promedio actual
        val v = newValue.coerceIn(1, 5).toFloat()

        val newCount = x + 1
        val newAvg = if (x <= 0L) {
            v
        } else {
            ((x * y) + v) / newCount.toDouble()
        }

        // Actualizamos la UI optimistamente
        tvRating.text = String.format(Locale.getDefault(), "%.1f", newAvg)

        // Avisamos al ViewModel para persistir en Firestore
        viewModel.rateProduct(
            newAvg   = newAvg.toFloat(),
            newCount = newCount.toInt()
        )

        showRatingThanksToast()
    }

    private fun showRatingThanksToast() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
        }

        val iconView = ImageView(this).apply {
            setImageResource(R.drawable.personajesingup)
            val size = dp(20)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                rightMargin = dp(8)
            }
        }

        val textView = TextView(this).apply {
            text = "¡Gracias por tu calificación!"
            setTextColor(android.graphics.Color.BLACK)
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

    companion object {
        const val EXTRA_PRODUCT_ID = "extra_product_id"
    }
}