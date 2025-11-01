package com.example.unimarket.view.business

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import com.example.unimarket.R
import com.example.unimarket.databinding.ActivityBusinessDetailBinding
import com.example.unimarket.view.profile.BuyerAccountActivity
import com.example.unimarket.viewmodel.BusinessCategoryTab
import com.example.unimarket.viewmodel.BusinessDetailUiState
import com.example.unimarket.viewmodel.BusinessDetailViewModel
import com.example.unimarket.viewmodel.BusinessDetailViewModelFactory
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import kotlin.LazyThreadSafetyMode

class BusinessDetailActivity : AppCompatActivity() {

    private val businessId: String by lazy(LazyThreadSafetyMode.NONE) {
        intent?.getStringExtra(EXTRA_BUSINESS_ID).orEmpty()
    }

    private val viewModel: BusinessDetailViewModel by viewModels {
        BusinessDetailViewModelFactory(businessId)
    }

    private lateinit var binding: ActivityBusinessDetailBinding
    private val productsAdapter = BusinessProductsAdapter { product ->
        Toast.makeText(this, product.name, Toast.LENGTH_SHORT).show()
    }
    private var suppressChipCallbacks = false
    private var lastError: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (businessId.isBlank()) {
            finish()
            return
        }

        binding = ActivityBusinessDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imgCover.setImageResource(R.drawable.header_wave)
        binding.imgLogo.setImageResource(R.drawable.personajesingup)
        binding.imgBrand.setImageResource(R.drawable.personajesingup)
        binding.imgAppBrand.setImageResource(R.drawable.personajesingup)
        binding.tvAppBrand.text = getString(R.string.app_name)
        binding.tvLogoInitial.isVisible = false
        binding.tvBrandFallback.isVisible = false

        setupTopBar()
        setupRecycler()
        setupChipListener()
        setupBottomNavigation()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ui.collect { ui ->
                    render(ui)
                }
            }
        }
    }

    private fun setupTopBar() {
        binding.btnFavorite.setOnClickListener {
            Toast.makeText(this, R.string.business_detail_favorite, Toast.LENGTH_SHORT).show()
        }
        binding.btnCart.setOnClickListener {
            Toast.makeText(this, R.string.business_detail_cart, Toast.LENGTH_SHORT).show()
        }
        binding.imgAppBrand.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.tvAppBrand.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecycler() {
        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(this@BusinessDetailActivity, 2)
            adapter = productsAdapter
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
        }
    }

    private fun setupBottomNavigation() {
        binding.navHome.setOnClickListener { finish() }
        val placeholder = getString(R.string.business_detail_nav_placeholder)
        binding.navProfile.setOnClickListener {
            startActivity(Intent(this, BuyerAccountActivity::class.java))
        }
        binding.navSearch.setOnClickListener {
            Toast.makeText(this, placeholder, Toast.LENGTH_SHORT).show()
        }
        binding.navMap.setOnClickListener {
            Toast.makeText(this, placeholder, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupChipListener() {
        binding.chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            if (suppressChipCallbacks) return@setOnCheckedStateChangeListener
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val chip = group.findViewById<Chip>(checkedId) ?: return@setOnCheckedStateChangeListener
            val categoryId = chip.tag as? String ?: return@setOnCheckedStateChangeListener
            viewModel.onCategorySelected(categoryId)
        }
    }

    private fun render(ui: BusinessDetailUiState) {
        binding.progress.isVisible = ui.loading
        binding.scrollContainer.isVisible = !ui.loading

        if (ui.error != null && ui.error != lastError) {
            Toast.makeText(this, ui.error, Toast.LENGTH_LONG).show()
            lastError = ui.error
        } else if (ui.error == null) {
            lastError = null
        }

        ui.business?.let { business ->
            val displayName = business.name.ifBlank { getString(R.string.app_name) }
            binding.tvBusinessName.text = displayName

            val rating = if (business.rating > 0) business.rating else 4.0
            binding.tvBusinessRating.text = getString(R.string.business_detail_rating_format, rating)

            val reviewsCount = when {
                business.products.isNotEmpty() -> business.products.size.toString()
                else -> "20"
            }
            binding.tvBusinessReviews.text = getString(R.string.business_detail_reviews_format, reviewsCount)

            val highlight = business.categories.firstOrNull()?.name?.takeIf { it.isNotBlank() }
                ?: getString(R.string.business_detail_delivery)
            binding.tvBusinessTag.text = highlight

            val address = business.address
            val meta = buildList {
                if (address.direccion.isNotBlank()) add(address.direccion)
                if (address.edificio.isNotBlank()) add(address.edificio)
                if (business.products.isNotEmpty()) {
                    add(getString(R.string.business_detail_products_count, business.products.size))
                }
            }.joinToString(" • ")
            binding.tvBusinessMeta.text = meta.ifBlank { getString(R.string.business_detail_eta_default) }

            val logoUrl = business.logo.orEmpty()
            if (logoUrl.isNotBlank()) {
                binding.imgLogo.load(logoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.personajesingup)
                    error(R.drawable.personajesingup)
                }
                binding.imgLogo.isVisible = true
                binding.tvLogoInitial.isVisible = false
                binding.imgBrand.load(logoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.personajesingup)
                    error(R.drawable.personajesingup)
                }
                binding.imgBrand.isVisible = true
                binding.tvBrandFallback.isVisible = false
                binding.imgCover.load(logoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.header_wave)
                    error(R.drawable.header_wave)
                }
            } else {
                binding.imgLogo.isVisible = false
                val initial = displayName.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
                binding.tvLogoInitial.text = initial.ifBlank { "?" }
                binding.tvLogoInitial.isVisible = true

                binding.imgBrand.isVisible = false
                binding.tvBrandFallback.text = displayName.uppercase()
                binding.tvBrandFallback.isVisible = true
                binding.imgCover.setImageResource(R.drawable.header_wave)
            }
        }

        updateChips(ui.tabs, ui.selectedTabId)

        val products = ui.selectedTabId?.let { id -> ui.productsByTab[id] }.orEmpty()
        productsAdapter.submitList(products)
        val selectedTab = ui.tabs.firstOrNull { it.id == ui.selectedTabId }
        binding.tvSectionTitle.text = selectedTab?.let { getString(it.labelRes) }
            ?: getString(R.string.business_detail_category_discounts)
        binding.rvProducts.isVisible = products.isNotEmpty()
        binding.tvEmptyState.isVisible = products.isEmpty()
    }

    private fun updateChips(tabs: List<BusinessCategoryTab>, selected: String?) {
        suppressChipCallbacks = true
        binding.chipGroupCategories.removeAllViews()
        var toCheck = View.NO_ID

        tabs.forEach { tab ->
            val chip = Chip(this, null, 0, R.style.Widget_App_BusinessFilterChip).apply {
                id = View.generateViewId()
                text = getString(tab.labelRes)
                tag = tab.id
                isCheckable = true
                isCheckedIconVisible = false
                setEnsureMinTouchTargetSize(false)
            }
            val params = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = resources.getDimensionPixelSize(R.dimen.space_s)
            }
            binding.chipGroupCategories.addView(chip, params)
            if (tab.id == selected && toCheck == View.NO_ID) {
                toCheck = chip.id
            }
        }

        if (binding.chipGroupCategories.childCount > 0) {
            val targetId = if (toCheck != View.NO_ID) toCheck else binding.chipGroupCategories.getChildAt(0).id
            binding.chipGroupCategories.check(targetId)
        }
        suppressChipCallbacks = false
    }

    companion object {
        private const val EXTRA_BUSINESS_ID = "extra_business_id"

        fun createIntent(context: Context, businessId: String): Intent {
            return Intent(context, BusinessDetailActivity::class.java).apply {
                putExtra(EXTRA_BUSINESS_ID, businessId)
            }
        }
    }
}
