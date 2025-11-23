package com.example.unimarket.view.home

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.unimarket.R
import com.example.unimarket.model.domain.entity.Product
import com.example.unimarket.view.map.BusinessMapActivity
import com.example.unimarket.viewmodel.BusinessDetailUiState
import com.example.unimarket.viewmodel.HomeBuyerViewModel
import com.example.unimarket.viewmodel.HomeNav
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Locale
import com.google.android.material.button.MaterialButton
import android.content.res.ColorStateList

class BusinessDetailActivity : AppCompatActivity() {

    private val viewModel: HomeBuyerViewModel by viewModels()

    private var businessId: String = ""
    private var currentRatingAvg: Float = 0f
    private var currentRatingCount: Int = 0
    private lateinit var ivHero: ImageView
    private lateinit var tvBusinessName: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvVotes: TextView

    private lateinit var chipGroupProductFilters: ChipGroup

    private lateinit var rvProducts: RecyclerView
    private lateinit var adapter: ProductsAdapter

    private lateinit var btnFavorites: ImageButton
    private lateinit var btnOrdersTop: ImageButton
    private lateinit var navHome: ImageButton
    private lateinit var navSearch: ImageButton
    private lateinit var navMap: ImageButton
    private lateinit var navProfile: ImageButton

    private lateinit var btnRateBusiness: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_business_detail)

        bindViews()
        setupTopBarAndFooter()
        setupRecycler()

        businessId          = intent.getStringExtra(EXTRA_BUSINESS_ID).orEmpty()
        val businessName    = intent.getStringExtra(EXTRA_BUSINESS_NAME).orEmpty()
        currentRatingAvg    = intent.getFloatExtra(EXTRA_BUSINESS_RATING, 0f)
        currentRatingCount  = intent.getIntExtra(EXTRA_BUSINESS_AMOUNT_RATINGS, 0)
        val logoUrl         = intent.getStringExtra(EXTRA_BUSINESS_LOGO_URL)
        val productIds: List<String> =
            intent.getStringArrayListExtra(EXTRA_BUSINESS_PRODUCT_IDS) ?: emptyList()

        viewModel.detail_init(businessId, businessName)
        viewModel.detail_loadProducts(productIds)

        if (businessName.isNotBlank()) tvBusinessName.text = businessName

        if (!logoUrl.isNullOrBlank()) {
            ivHero.load(logoUrl) {
                crossfade(true)
                placeholder(R.drawable.personajesingup)
                error(R.drawable.personajesingup)
            }
        } else {
            ivHero.setImageResource(R.drawable.funko)
        }

        if (currentRatingCount > 0) {
            tvRating.text = String.format(Locale.getDefault(), "%.1f", currentRatingAvg)
        } else {
            tvRating.text = "-"
        }
        tvVotes.text = "(${currentRatingCount}+)"

        observeDetail()
        observeNav()
    }

    private fun bindViews() {
        ivHero         = findViewById(R.id.ivHero)
        tvBusinessName = findViewById(R.id.tvBusinessName)
        tvRating       = findViewById(R.id.tvRating)
        tvVotes        = findViewById(R.id.tvVotes)

        chipGroupProductFilters = findViewById(R.id.chipGroupProductFilters)
        rvProducts   = findViewById(R.id.rvProducts)

        btnFavorites = findViewById(R.id.btnFavorites)
        btnOrdersTop = findViewById(R.id.btnOrdersTop)
        navHome      = findViewById(R.id.nav_home)
        navSearch    = findViewById(R.id.nav_search)
        navMap       = findViewById(R.id.nav_map)
        navProfile   = findViewById(R.id.nav_profile)

        btnRateBusiness = findViewById(R.id.btnRate)
    }

    private fun setupTopBarAndFooter() {
        btnFavorites.setOnClickListener { showFeatureUnavailableToast() }
        btnOrdersTop.setOnClickListener { showFeatureUnavailableToast() }

        navHome.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        navSearch.setOnClickListener { showFeatureUnavailableToast() }

        navMap.setOnClickListener {
            startActivity(Intent(this, BusinessMapActivity::class.java))
        }

        navProfile.setOnClickListener {
            viewModel.onClickProfile()
        }

        btnRateBusiness.apply {
            stateListAnimator = null

            val strongElevation = dp(10).toFloat()
            elevation = strongElevation
            translationZ = strongElevation

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val darkShadow = androidx.core.content.ContextCompat.getColor(
                    context,
                    android.R.color.black
                )
                outlineAmbientShadowColor = darkShadow
                outlineSpotShadowColor = darkShadow
            }

            setTextColor(
                androidx.core.content.ContextCompat.getColor(
                    context,
                    R.color.white
                )
            )
        }

        btnRateBusiness.setOnClickListener {
            showRateDialog()
        }
    }

    private fun setupRecycler() {
        adapter = ProductsAdapter(onAddClick = { product ->
            Toast.makeText(this, "Añadido: ${product.name}", Toast.LENGTH_SHORT).show()
        })
        rvProducts.layoutManager = GridLayoutManager(this, 2)
        rvProducts.adapter = adapter
    }

    private fun observeDetail() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.detail.collect { ui ->
                    renderDetail(ui)
                }
            }
        }
    }

    private fun observeNav() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.nav.collect { nav ->
                    when (nav) {
                        is HomeNav.ToBuyerProfile -> {
                            startActivity(
                                Intent(
                                    this@BusinessDetailActivity,
                                    com.example.unimarket.view.profile.BuyerAccountActivity::class.java
                                )
                            )
                            viewModel.navHandled()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun renderDetail(ui: BusinessDetailUiState) {
        if (ui.businessName.isNotBlank()) {
            tvBusinessName.text = ui.businessName
        }

        renderChips(ui)

        val items = filterProducts(ui.products, ui.currentFilter)
            .map { it.toBusinessProductItem() }

        adapter.submit(items)

        ui.error?.let {
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            viewModel.detail_errorShown()
        }
    }

    private fun renderChips(ui: BusinessDetailUiState) {
        chipGroupProductFilters.isSingleSelection = true

        val tags: List<String> = listOf(FILTER_ALL) + ui.categories

        chipGroupProductFilters.setOnCheckedStateChangeListener(null)
        chipGroupProductFilters.removeAllViews()

        // Colores desde colors.xml
        val selectedBg  = ContextCompat.getColor(this, R.color.yellowLight)
        val unselectedBg = ContextCompat.getColor(this, android.R.color.white)
        val textColor   = ContextCompat.getColor(this, R.color.text_primary)

        val bgColors = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                selectedBg,
                unselectedBg
            )
        )

        for (tag in tags) {
            val label = if (tag == FILTER_ALL) {
                getString(R.string.business_products_filter_all)
            } else {
                tag
            }

            val chip = Chip(this).apply {
                id = View.generateViewId()
                this.tag = tag
                text = label
                isCheckable = true
                isClickable = true

                // colores personalizados
                chipBackgroundColor = bgColors
                setTextColor(textColor)
                isCheckedIconVisible = false
            }
            chipGroupProductFilters.addView(chip)
        }

        val toSelectTag = ui.currentFilter.ifBlank { FILTER_ALL }
        val chipId = getChipIdByTag(toSelectTag)
        if (chipId != View.NO_ID) {
            chipGroupProductFilters.check(chipId)
        }

        chipGroupProductFilters.setOnCheckedStateChangeListener { group, checkedIds ->
            val id = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val chip: Chip? = group.findViewById(id)
            val tag = chip?.tag as? String ?: FILTER_ALL
            viewModel.detail_onFilterSelected(tag)
        }
    }

    private fun getChipIdByTag(tag: String): Int {
        for (i in 0 until chipGroupProductFilters.childCount) {
            val chip = chipGroupProductFilters.getChildAt(i) as? Chip ?: continue
            if ((chip.tag as? String).equals(tag, ignoreCase = true)) return chip.id
        }
        return View.NO_ID
    }

    private fun filterProducts(products: List<Product>, tag: String): List<Product> =
        when {
            tag.isBlank() || tag == FILTER_ALL -> products
            else -> products.filter {
                it.category.trim().equals(tag.trim(), ignoreCase = true)
            }
        }

    private fun Product.toBusinessProductItem(): BusinessProductItem {
        return BusinessProductItem(
            id = id,
            name = name,
            subtitle = category,
            price = formatPrice(price),
            rating = String.format(Locale.getDefault(), "%.1f", rating),
            imageUrl = image
        )
    }

    private fun formatPrice(value: Double): String {
        return "$" + String.format(Locale("es", "CO"), "%,.0f", value)
    }

    private fun showRateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rate_business, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)

        MaterialAlertDialogBuilder(this)
            .setTitle("Califica el negocio")
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

        val x = currentRatingCount
        val y = currentRatingAvg
        val v = newValue.toFloat()

        val newAvg = ((x * y) + v) / (x + 1)

        currentRatingCount = x + 1
        currentRatingAvg   = newAvg

        tvRating.text = String.format(Locale.getDefault(), "%.1f", currentRatingAvg)
        tvVotes.text  = "(${currentRatingCount}+)"

        viewModel.rateBusiness(
            businessId = businessId,
            newAvg = currentRatingAvg,
            newCount = currentRatingCount
        )

        showRatingThanksToast()
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

    companion object {
        const val FILTER_ALL = "all"

        const val EXTRA_BUSINESS_ID = "EXTRA_BUSINESS_ID"
        const val EXTRA_BUSINESS_NAME = "EXTRA_BUSINESS_NAME"
        const val EXTRA_BUSINESS_RATING = "EXTRA_BUSINESS_RATING"
        const val EXTRA_BUSINESS_AMOUNT_RATINGS = "EXTRA_BUSINESS_AMOUNT_RATINGS"
        const val EXTRA_BUSINESS_LOGO_URL = "EXTRA_BUSINESS_LOGO_URL"
        const val EXTRA_BUSINESS_PRODUCT_IDS = "EXTRA_BUSINESS_PRODUCT_IDS"
    }

    private fun showRatingThanksToast() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.parseColor("#FFFFFF"))
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
}