package com.example.unimarket.view.home

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.EditText
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarket.R
import com.example.unimarket.model.domain.entity.Product
import com.example.unimarket.view.map.BusinessMapActivity
import com.example.unimarket.view.profile.BuyerAccountActivity
import com.example.unimarket.view.profile.CartActivity
import com.example.unimarket.viewmodel.ExploreBuyerViewModel
import kotlinx.coroutines.launch
import com.example.unimarket.viewmodel.CartViewModel
import com.example.unimarket.view.home.ProductDetailActivity


class ExploreBuyerActivity : AppCompatActivity() {

    private val viewModel: ExploreBuyerViewModel by viewModels()
    private val cartViewModel: CartViewModel by viewModels()

    // Lista de productos
    private lateinit var rvProducts: RecyclerView
    private lateinit var productsAdapter: ProductsAdapter

    // Header
    private lateinit var btnFavorites: ImageButton
    private lateinit var btnOrdersTop: ImageButton

    // Search
    private lateinit var etSearch: EditText

    // Bottom nav
    private lateinit var navHome: ImageButton
    private lateinit var navSearch: ImageButton
    private lateinit var navMap: ImageButton
    private lateinit var navProfile: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.explore)

        initViews()
        setupRecycler()
        setupSearch()
        setupClicks()
        observeUi()
    }

    private fun initViews() {
        rvProducts   = findViewById(R.id.rvProducts)
        btnFavorites = findViewById(R.id.btnFavorites)
        btnOrdersTop = findViewById(R.id.btnOrdersTop)

        etSearch = findViewById(R.id.etSearch)

        navHome    = findViewById(R.id.nav_home)
        navSearch  = findViewById(R.id.nav_search)
        navMap     = findViewById(R.id.nav_map)
        navProfile = findViewById(R.id.nav_profile)
    }

    private fun setupRecycler() {
        productsAdapter = ProductsAdapter(
            onCardClick = { item: BusinessProductItem ->
                // Navegar a ProductDetail con el id del producto
                val intent = Intent(this, ProductDetailActivity::class.java).apply {
                    putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, item.id)
                }
                startActivity(intent)
            },
            onAddClick = { item: BusinessProductItem ->
                // Agregar 1 unidad al carrito
                cartViewModel.addProduct(item.id)

                Toast.makeText(
                    this,
                    "Añadido al carrito: ${item.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        rvProducts.apply {
            layoutManager = GridLayoutManager(this@ExploreBuyerActivity, 2)
            adapter = productsAdapter
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty()
                viewModel.onSearchQueryChanged(query)
            }
        })
    }

    private fun setupClicks() {
        // Header
        btnFavorites.setOnClickListener { showFeatureUnavailableToast() }
        btnOrdersTop.setOnClickListener { startActivity(Intent(this, CartActivity::class.java)) }

        // Bottom nav

        // Home: ir SIEMPRE a Home, sin apilar Explore infinitas veces
        navHome.setOnClickListener {
            goTo(HomeBuyerActivity::class.java)
        }

        // Ya estamos en Explore/Search
        navSearch.setOnClickListener {
            // no-op
        }

        navMap.setOnClickListener {
            goTo(BusinessMapActivity::class.java)
        }

        navProfile.setOnClickListener {
            goTo(BuyerAccountActivity::class.java)
        }
    }

    private fun goTo(target: Class<*>) {
        // Si ya estoy en esa Activity, no hago nada
        if (this::class.java == target) return

        val intent = Intent(this, target).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        startActivity(intent)
    }

    private fun observeUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { ui ->
                    val items = ui.products.map { it.toBusinessProductItem() }
                    productsAdapter.submit(items)

                    ui.error?.let { msg ->
                        Toast.makeText(this@ExploreBuyerActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.loadProducts()
    }

    // --- Helpers UI ---

    private fun Product.toBusinessProductItem(): BusinessProductItem {
        val subtitle = if (description.isNotBlank()) description else category

        val priceText = if (price > 0.0) {
            // puedes ajustar formato más adelante (COP, etc.)
            String.format("$%.0f", price)
        } else {
            "—"
        }

        val ratingText = if (rating > 0.0) {
            String.format("%.1f", rating)
        } else {
            "—"
        }

        return BusinessProductItem(
            id       = id,
            name     = name,
            subtitle = subtitle,
            price    = priceText,
            rating   = ratingText,
            imageUrl = image
        )
    }

    private fun showFeatureUnavailableToast() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.parseColor("#FFFFFF"))
        }

        val iconView = ImageView(this).apply {
            setImageResource(R.drawable.personajesingup)
            val size = dp(20)
            layoutParams = LinearLayout.LayoutParams(size, size).apply { rightMargin = dp(8) }
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