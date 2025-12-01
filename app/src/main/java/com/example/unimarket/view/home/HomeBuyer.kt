package com.example.unimarket.view.home

import android.content.Intent
import android.content.res.ColorStateList
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarket.R
import com.example.unimarket.model.domain.entity.Business
import com.example.unimarket.view.map.BusinessMapActivity
import com.example.unimarket.view.profile.CartActivity
import com.example.unimarket.viewmodel.CartViewModel
import com.example.unimarket.viewmodel.Filter
import com.example.unimarket.viewmodel.HomeBuyerViewModel
import com.example.unimarket.viewmodel.HomeNav
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

class HomeBuyerActivity : AppCompatActivity() {

    private val viewModel: HomeBuyerViewModel by viewModels()
    // ViewModel de carrito
    private val cartViewModel: CartViewModel by viewModels()

    private lateinit var rvBusinesses: RecyclerView
    private lateinit var businessAdapter: BusinessAdapter

    private lateinit var btnFavorites: ImageButton
    private lateinit var btnOrdersTop: ImageButton

    private lateinit var navHome: ImageButton
    private lateinit var navSearch: ImageButton
    private lateinit var navMap: ImageButton
    private lateinit var navProfile: ImageButton

    private lateinit var chipGroupFilters: ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var chipFood: Chip
    private lateinit var chipStationery: Chip
    private lateinit var chipTutoring: Chip
    private lateinit var chipAccessories: Chip

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homepage)
        Log.d("HomeBuyerActivity", "onCreate() llamado")
        initViews()
        setupRecycler()
        setupClicks()
        observeUi()
        observeNav()
    }

    private fun initViews() {
        rvBusinesses = findViewById(R.id.rvBusinesses)

        btnFavorites = findViewById(R.id.btnFavorites)
        btnOrdersTop = findViewById(R.id.btnOrdersTop)

        navHome = findViewById(R.id.nav_home)
        navSearch = findViewById(R.id.nav_search)
        navMap = findViewById(R.id.nav_map)
        navProfile = findViewById(R.id.nav_profile)

        chipGroupFilters = findViewById(R.id.chipGroupFilters)
        chipAll         = findViewById(R.id.chip_all)
        chipFood        = findViewById(R.id.chip_food)
        chipStationery  = findViewById(R.id.chip_stationery)
        chipTutoring    = findViewById(R.id.chip_tutoring)
        chipAccessories = findViewById(R.id.chip_accessories)

        val chipBg = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(
                ContextCompat.getColor(this, R.color.yellowLight),
                ContextCompat.getColor(this, android.R.color.white)
            )
        )
        for (i in 0 until chipGroupFilters.childCount) {
            (chipGroupFilters.getChildAt(i) as? Chip)?.chipBackgroundColor = chipBg
        }
    }

    private fun setupRecycler() {
        businessAdapter = BusinessAdapter(
            items = emptyList(),
            onClick = { business: Business ->
                viewModel.onBusinessSelected(business)
            },
            // nuevo callback: agregar producto al carrito
            onAddProductClick = { productItem ->
                cartViewModel.addProduct(productItem.id)
            }
        )

        rvBusinesses.apply {
            layoutManager = LinearLayoutManager(this@HomeBuyerActivity)
            adapter = businessAdapter
        }
    }

    private fun setupClicks() {
        navProfile.setOnClickListener { viewModel.onClickProfile() }

        chipAll.setOnClickListener        { viewModel.onFilterSelected(Filter.ALL) }
        chipFood.setOnClickListener       { viewModel.onFilterSelected(Filter.FOOD) }
        chipStationery.setOnClickListener { viewModel.onFilterSelected(Filter.STATIONERY) }
        chipTutoring.setOnClickListener   { viewModel.onFilterSelected(Filter.TUTORING) }
        chipAccessories.setOnClickListener{ viewModel.onFilterSelected(Filter.ACCESSORIES) }

        navMap.setOnClickListener {
            startActivity(Intent(this, BusinessMapActivity::class.java))
        }

        btnFavorites.setOnClickListener { showFeatureUnavailableToast() }
        btnOrdersTop.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }
        navHome.setOnClickListener {}
        navSearch.setOnClickListener {
            startActivity(Intent(this, ExploreBuyerActivity::class.java))
        }
    }

    private fun observeUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ui.collect { ui ->
                    businessAdapter.submit(ui.businessesFiltered)
                    renderFilters(ui.selected)
                    ui.error?.let { msg ->
                        Toast.makeText(this@HomeBuyerActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun renderFilters(selected: Filter) {
        chipAll.isChecked         = selected == Filter.ALL
        chipFood.isChecked        = selected == Filter.FOOD
        chipStationery.isChecked  = selected == Filter.STATIONERY
        chipTutoring.isChecked    = selected == Filter.TUTORING
        chipAccessories.isChecked = selected == Filter.ACCESSORIES
    }

    private fun observeNav() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.nav.collect { nav ->
                    when (nav) {
                        is HomeNav.ToBuyerProfile -> {
                            startActivity(
                                Intent(
                                    this@HomeBuyerActivity,
                                    com.example.unimarket.view.profile.BuyerAccountActivity::class.java
                                )
                            )
                            viewModel.navHandled()
                        }
                        is HomeNav.ToBusinessDetail -> {
                            val b = nav.business
                            val intent = Intent(
                                this@HomeBuyerActivity,
                                BusinessDetailActivity::class.java
                            ).apply {
                                putExtra(BusinessDetailActivity.EXTRA_BUSINESS_ID, b.id)
                                putExtra(BusinessDetailActivity.EXTRA_BUSINESS_NAME, b.name)
                                putExtra(BusinessDetailActivity.EXTRA_BUSINESS_RATING, b.rating.toFloat())
                                putExtra(BusinessDetailActivity.EXTRA_BUSINESS_AMOUNT_RATINGS, b.amountRatings.toInt())
                                putExtra(BusinessDetailActivity.EXTRA_BUSINESS_LOGO_URL, b.logo)
                                putStringArrayListExtra(
                                    BusinessDetailActivity.EXTRA_BUSINESS_PRODUCT_IDS,
                                    ArrayList(b.products)
                                )
                            }
                            startActivity(intent)
                            viewModel.navHandled()
                        }
                        HomeNav.None -> Unit
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun isOnline(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java)
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    override fun onStart() {
        super.onStart()
        viewModel.reloadBusinesses(isOnline())
    }

    override fun onResume() {
        super.onResume()
        val online = isOnline()
        viewModel.reloadBusinesses(isOnline())
        if (online) viewModel.flushOfflineCategoryClicks()
    }
}