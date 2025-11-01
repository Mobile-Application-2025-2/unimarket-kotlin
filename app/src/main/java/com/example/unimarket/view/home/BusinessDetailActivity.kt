package com.example.unimarket.view.home

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.unimarket.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class BusinessDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BUSINESS_ID = "extra_business_id"
    }

    private val viewModel: BusinessDetailViewModel by viewModels()

    private lateinit var imgHero: ImageView
    private lateinit var imgLogo: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvCategories: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvRatingCount: TextView
    private lateinit var progress: android.widget.ProgressBar
    private lateinit var chipGroup: ChipGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var productsAdapter: BusinessProductsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_business_detail)

        bindViews()
        setupRecycler()
        setupTopActions()
        observeUi()

        val businessId = intent?.getStringExtra(EXTRA_BUSINESS_ID).orEmpty()
        viewModel.load(businessId)
    }

    private fun bindViews() {
        imgHero = findViewById(R.id.imgHero)
        imgLogo = findViewById(R.id.imgBusinessLogo)
        tvName = findViewById(R.id.tvBusinessName)
        tvCategories = findViewById(R.id.tvBusinessCategories)
        tvDescription = findViewById(R.id.tvBusinessCategories)
        tvRating = findViewById(R.id.tvBusinessRating)
        tvRatingCount = findViewById(R.id.tvBusinessRatingCount)
        progress = findViewById(R.id.progress)
        chipGroup = findViewById(R.id.chipGroupProductFilters)
        recyclerView = findViewById(R.id.rvBusinessProducts)
        emptyView = findViewById(R.id.tvEmpty)
    }

    private fun setupRecycler() {
        productsAdapter = BusinessProductsAdapter { product ->
            Toast.makeText(
                this,
                getString(R.string.business_product_added_toast, product.name),
                Toast.LENGTH_SHORT
            ).show()
        }
        recyclerView.apply {
            layoutManager = GridLayoutManager(this@BusinessDetailActivity, 2)
            adapter = productsAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupTopActions() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        findViewById<ImageButton>(R.id.btnFavorite).setOnClickListener {
            Toast.makeText(this, R.string.business_action_favorite, Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageButton>(R.id.btnOrders).setOnClickListener {
            Toast.makeText(this, R.string.business_action_orders, Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageButton>(R.id.navHome).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        findViewById<ImageButton>(R.id.navFavorites).setOnClickListener {
            Toast.makeText(this, R.string.business_action_favorite, Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageButton>(R.id.navBag).setOnClickListener {
            Toast.makeText(this, R.string.business_action_orders, Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageButton>(R.id.navProfile).setOnClickListener {
            Toast.makeText(this, R.string.business_action_profile, Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ui.collect { state ->
                    progress.isVisible = state.loading

                    tvName.text = state.businessName
                    tvCategories.text = state.categoriesText
                    tvDescription.text = state.description
                    tvRating.text = state.ratingText
                    tvRatingCount.text = state.ratingCountText

                    renderImage(imgHero, state.heroImageUrl, R.drawable.formas)
                    renderImage(imgLogo, state.logoUrl, R.drawable.personajesingup)

                    renderFilters(state)
                    productsAdapter.submit(state.productsFiltered)

                    emptyView.isVisible = !state.loading && state.productsFiltered.isEmpty()

                    state.error?.let { message ->
                        Toast.makeText(this@BusinessDetailActivity, message, Toast.LENGTH_LONG).show()
                        viewModel.onErrorConsumed()
                    }
                }
            }
        }
    }

    private fun renderFilters(state: BusinessDetailUiState) {
        val selectedId = state.selectedFilterId
        chipGroup.setOnCheckedStateChangeListener(null)
        chipGroup.removeAllViews()

        state.filters.forEach { option ->
            val chip = Chip(this)
            chip.id = View.generateViewId()
            chip.tag = option.id
            chip.text = if (option.id == ProductFilterOption.ALL_ID) {
                getString(R.string.business_products_filter_all)
            } else {
                option.label
            }
            chip.isCheckable = true
            styleChip(chip)
            chipGroup.addView(chip)
            if (option.id.equals(selectedId, ignoreCase = true)) {
                chipGroup.check(chip.id)
            }
        }

        if (chipGroup.checkedChipId == View.NO_ID && chipGroup.childCount > 0) {
            val first = chipGroup.getChildAt(0) as? Chip
            first?.let {
                chipGroup.check(it.id)
                val filterId = it.tag as? String ?: ProductFilterOption.ALL_ID
                viewModel.onFilterSelected(filterId)
            }
        }

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val id = checkedIds.firstOrNull()
            val chip = id?.let { chipGroup.findViewById<Chip>(it) }
            val filterId = chip?.tag as? String ?: return@setOnCheckedStateChangeListener
            viewModel.onFilterSelected(filterId)
        }
    }

    private fun styleChip(chip: Chip) {
        val bg = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(ContextCompat.getColor(this, R.color.yellowLight), ContextCompat.getColor(this, android.R.color.white))
        )
        chip.chipBackgroundColor = bg
        chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        chip.chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.yellowStroke))
        chip.chipStrokeWidth = resources.displayMetrics.density
        chip.isAllCaps = false
        chip.isCloseIconVisible = false
        chip.setEnsureMinTouchTargetSize(false)
    }

    private fun renderImage(target: ImageView, url: String, placeholderRes: Int) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            target.setImageResource(placeholderRes)
        } else {
            target.load(trimmed) {
                crossfade(true)
                placeholder(placeholderRes)
                error(placeholderRes)
            }
        }
    }
}