package com.example.unimarket.view.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.unimarket.R
import com.example.unimarket.model.domain.entity.Product
import com.example.unimarket.viewmodel.BusinessMenuUiState
import com.example.unimarket.viewmodel.BusinessMenuViewModel
import com.example.unimarket.viewmodel.MenuCategory
import com.example.unimarket.viewmodel.MenuSection
import com.example.unimarket.view.profile.BuyerAccountActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.util.Locale

class BusinessMenuActivity : AppCompatActivity() {

    private val viewModel: BusinessMenuViewModel by viewModels()

    private lateinit var coverImage: ImageView
    private lateinit var logoImage: ImageView
    private lateinit var businessName: TextView
    private lateinit var businessSubtitle: TextView
    private lateinit var rating: TextView
    private lateinit var reviews: TextView
    private lateinit var chipGroup: ChipGroup
    private lateinit var scrollView: NestedScrollView
    private lateinit var sectionsContainer: LinearLayout
    private lateinit var emptyMenuView: View

    private var fallbackName: String = ""
    private var fallbackSubtitle: String = ""
    private var fallbackLogo: String = ""

    private var currentCategories: List<MenuCategory> = emptyList()
    private var updatingChipSelection: Boolean = false
    private var lastErrorShown: String? = null
    private var shouldAutoScroll: Boolean = false
    private var pendingScrollKey: String? = null
    private var lastSelectionKey: String? = null

    private val sectionHolders = mutableMapOf<String, SectionHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_business_menu)

        val toolbar: MaterialToolbar = findViewById(R.id.topBar)
        toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.back)
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.text_primary))
        toolbar.navigationContentDescription = getString(R.string.back)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        findViewById<TextView>(R.id.tvBrand).text = getString(R.string.app_name)

        coverImage = findViewById(R.id.imgCover)
        logoImage = findViewById(R.id.imgBusinessLogo)
        businessName = findViewById(R.id.tvBusinessName)
        businessSubtitle = findViewById(R.id.tvBusinessDescription)
        rating = findViewById(R.id.tvRating)
        reviews = findViewById(R.id.tvReviews)
        chipGroup = findViewById(R.id.chipGroupCategories)
        scrollView = findViewById(R.id.scrollContent)
        sectionsContainer = findViewById(R.id.containerSections)
        emptyMenuView = findViewById(R.id.tvEmptyMenu)

        chipGroup.isSingleSelection = true
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (updatingChipSelection) return@setOnCheckedStateChangeListener
            val selectedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val chip = group.findViewById<Chip>(selectedId) ?: return@setOnCheckedStateChangeListener
            val key = (chip.tag as? String).orEmpty()
            if (key.isEmpty()) return@setOnCheckedStateChangeListener
            shouldAutoScroll = true
            pendingScrollKey = key
            viewModel.onCategorySelected(key)
        }

        findViewById<ImageButton>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, BuyerAccountActivity::class.java))
        }

        findViewById<ImageButton>(R.id.navHome).setOnClickListener {
            finish()
        }

        fallbackName = intent.getStringExtra(EXTRA_BUSINESS_NAME).orEmpty()
        fallbackSubtitle = intent.getStringExtra(EXTRA_BUSINESS_SUBTITLE).orEmpty()
        fallbackLogo = intent.getStringExtra(EXTRA_BUSINESS_LOGO).orEmpty()

        renderFallback()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ui.collect { state ->
                    renderState(state)
                }
            }
        }

        val businessId = intent.getStringExtra(EXTRA_BUSINESS_ID)
        if (businessId.isNullOrBlank()) {
            Toast.makeText(this, R.string.business_menu_missing_id, Toast.LENGTH_LONG).show()
            finish()
        } else {
            viewModel.loadBusiness(businessId)
        }
    }

    private fun renderFallback() {
        businessName.text = fallbackName.ifBlank { getString(R.string.business_menu_placeholder_name) }
        businessSubtitle.text = fallbackSubtitle.ifBlank { getString(R.string.business_menu_placeholder_subtitle) }
        rating.text = String.format(Locale.US, "%.1f", intent.getDoubleExtra(EXTRA_BUSINESS_RATING, 4.0))
        val reviewsFallback = intent.getIntExtra(EXTRA_BUSINESS_REVIEWS, 200)
        reviews.text = getString(R.string.business_reviews_count, reviewsFallback)

        if (fallbackLogo.isBlank()) {
            logoImage.setImageResource(R.drawable.personajesingup)
            coverImage.setImageResource(R.drawable.personajesingup)
        } else {
            logoImage.load(fallbackLogo) {
                crossfade(true)
                placeholder(R.drawable.personajesingup)
                error(R.drawable.personajesingup)
            }
            coverImage.load(fallbackLogo) {
                crossfade(true)
                placeholder(R.drawable.personajesingup)
                error(R.drawable.personajesingup)
            }
        }
    }

    private fun renderState(state: BusinessMenuUiState) {
        state.business?.let { business ->
            businessName.text = business.name.ifBlank { fallbackName }
            businessSubtitle.text = business.categories
                .map { it.name }
                .filter { it.isNotBlank() }
                .joinToString(" · ")
                .ifBlank { fallbackSubtitle }

            val logo = business.logo.ifBlank { fallbackLogo }
            if (logo.isBlank()) {
                logoImage.setImageResource(R.drawable.personajesingup)
                coverImage.setImageResource(R.drawable.personajesingup)
            } else {
                logoImage.load(logo) {
                    crossfade(true)
                    placeholder(R.drawable.personajesingup)
                    error(R.drawable.personajesingup)
                }
                coverImage.load(logo) {
                    crossfade(true)
                    placeholder(R.drawable.personajesingup)
                    error(R.drawable.personajesingup)
                }
            }

            val ratingValue = if (business.rating > 0.0) business.rating else intent.getDoubleExtra(EXTRA_BUSINESS_RATING, 4.0)
            rating.text = String.format(Locale.US, "%.1f", ratingValue)
        }

        val reviewCount = if (state.reviewCount > 0) state.reviewCount else intent.getIntExtra(EXTRA_BUSINESS_REVIEWS, 200)
        reviews.text = getString(R.string.business_reviews_count, reviewCount)

        updateCategories(state.categories, state.selectedCategoryKey)
        renderSections(state.sections)
        maybeScrollToSelection(state.selectedCategoryKey)

        val errorMessage = state.error
        if (!errorMessage.isNullOrBlank() && errorMessage != lastErrorShown) {
            lastErrorShown = errorMessage
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun updateCategories(categories: List<MenuCategory>, selectedKey: String) {
        updatingChipSelection = true

        if (categories != currentCategories) {
            chipGroup.removeAllViews()
            categories.forEach { category ->
                val chip = layoutInflater.inflate(R.layout.item_menu_category_chip, chipGroup, false) as Chip
                chip.id = View.generateViewId()
                chip.text = category.label
                chip.tag = category.key
                chipGroup.addView(chip)
            }
            currentCategories = categories
        }

        if (categories.isEmpty()) {
            chipGroup.clearCheck()
            updatingChipSelection = false
            return
        }

        val normalizedKey = categories.firstOrNull { category ->
            category.key.equals(selectedKey, ignoreCase = true)
        }?.key ?: categories.first().key

        var selectedId: Int? = null
        chipGroup.children.forEach { view ->
            val chip = view as? Chip ?: return@forEach
            val chipKey = chip.tag as? String ?: ""
            val shouldCheck = chipKey.equals(normalizedKey, ignoreCase = true)
            chip.isChecked = shouldCheck
            if (shouldCheck) {
                selectedId = chip.id
            }
        }

        selectedId?.let { chipGroup.check(it) }

        updatingChipSelection = false
    }

    private fun renderSections(sections: List<MenuSection>) {
        val previousKeys = sectionHolders.keys.map { it }
        sectionHolders.clear()
        sectionsContainer.removeAllViews()

        sections.forEach { section ->
            val holder = createSectionHolder(section.key)
            holder.title.text = section.title
            holder.adapter.submit(section.products)
            sectionsContainer.addView(holder.root)
            sectionHolders[section.key] = holder
        }

        if (sections.map { it.key } != previousKeys) {
            lastSelectionKey = null
        }

        if (pendingScrollKey != null && sections.none { it.key.equals(pendingScrollKey, ignoreCase = true) }) {
            pendingScrollKey = null
            shouldAutoScroll = false
        }

        emptyMenuView.visibility = if (sections.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun maybeScrollToSelection(selectedKey: String) {
        val holder = findSectionHolder(selectedKey)
        if (holder == null) {
            if (sectionHolders.isEmpty()) {
                shouldAutoScroll = false
                pendingScrollKey = null
            }
            return
        }
        if (shouldAutoScroll && pendingScrollKey?.let { it.equals(selectedKey, ignoreCase = true) } == true) {
            scrollToSection(holder)
            shouldAutoScroll = false
            pendingScrollKey = null
        } else if (lastSelectionKey == null) {
            scrollView.post { scrollView.scrollTo(0, 0) }
        }
        lastSelectionKey = holder.key
    }

    private fun scrollToSection(holder: SectionHolder) {
        scrollView.post {
            val offset = resources.getDimensionPixelSize(R.dimen.business_menu_scroll_offset)
            val target = holder.root.top + sectionsContainer.top - offset
            scrollView.smoothScrollTo(0, target.coerceAtLeast(0))
        }
    }

    private fun findSectionHolder(key: String): SectionHolder? {
        return sectionHolders.entries.firstOrNull { entry ->
            entry.key.equals(key, ignoreCase = true)
        }?.value
    }

    private fun createSectionHolder(key: String): SectionHolder {
        val view = layoutInflater.inflate(R.layout.item_business_menu_section, sectionsContainer, false)
        val title = view.findViewById<TextView>(R.id.tvSectionTitle)
        val recycler = view.findViewById<RecyclerView>(R.id.rvSectionProducts)
        val adapter = MenuProductAdapter(emptyList(), ::handleAddProduct)
        recycler.layoutManager = GridLayoutManager(this, 2)
        recycler.adapter = adapter
        return SectionHolder(key, view, title, recycler, adapter)
    }

    private fun handleAddProduct(product: Product) {
        val label = product.name.ifBlank {
            product.description.ifBlank { getString(R.string.business_menu_product_placeholder) }
        }
        Toast.makeText(this, getString(R.string.menu_item_added, label), Toast.LENGTH_SHORT).show()
    }

    private data class SectionHolder(
        val key: String,
        val root: View,
        val title: TextView,
        val recycler: RecyclerView,
        val adapter: MenuProductAdapter
    )

    companion object {
        const val EXTRA_BUSINESS_ID = "extra_business_id"
        const val EXTRA_BUSINESS_NAME = "extra_business_name"
        const val EXTRA_BUSINESS_SUBTITLE = "extra_business_subtitle"
        const val EXTRA_BUSINESS_LOGO = "extra_business_logo"
        const val EXTRA_BUSINESS_RATING = "extra_business_rating"
        const val EXTRA_BUSINESS_REVIEWS = "extra_business_reviews"
    }
}