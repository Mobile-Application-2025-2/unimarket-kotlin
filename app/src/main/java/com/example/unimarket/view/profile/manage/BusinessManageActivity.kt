package com.example.unimarket.view.profile.manage

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import com.example.unimarket.R
import kotlinx.coroutines.launch
import java.util.Locale

class BusinessManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBusinessManageBinding
    private val viewModel: BusinessManageViewModel by viewModels()
    private lateinit var productsAdapter: BusinessOwnerProductsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusinessManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecycler()
        setupActions()
        observeState()

        viewModel.load()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.business_owner_title)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecycler() {
        productsAdapter = BusinessOwnerProductsAdapter { item ->
            openProductEditor(item.id)
        }
        binding.rvOwnerProducts.apply {
            layoutManager = GridLayoutManager(this@BusinessManageActivity, 2)
            adapter = productsAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupActions() {
        binding.btnAddProduct.setOnClickListener { openProductEditor(null) }
        binding.btnEditInfo.setOnClickListener { openBusinessInfoEditor() }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ui.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: BusinessManageUiState) {
        binding.progressManage.isVisible = state.loading || state.productsLoading

        val heroPlaceholder = R.drawable.formas
        binding.imgHero.load(state.heroImageUrl.ifBlank { null }) {
            crossfade(true)
            placeholder(heroPlaceholder)
            error(heroPlaceholder)
        }
        binding.imgLogo.load(state.logoUrl.ifBlank { null }) {
            crossfade(true)
            placeholder(R.drawable.personajesingup)
            error(R.drawable.personajesingup)
        }

        binding.tvBusinessNameManage.text = state.businessName.ifBlank {
            getString(R.string.business_owner_name_placeholder)
        }
        binding.tvBusinessDescription.text = state.description.ifBlank {
            getString(R.string.business_owner_description_placeholder)
        }

        val statusText = state.statusLabel.ifBlank {
            if (state.isOpen) getString(R.string.business_owner_status_open) else getString(R.string.business_owner_status_closed)
        }
        binding.tvStatus.text = statusText.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
        }

        val statusTint = if (state.isOpen) R.color.yellowLight else R.color.divider
        val statusTextColor = if (state.isOpen) R.color.text_primary else R.color.textGray
        ViewCompat.setBackgroundTintList(
            binding.tvStatus,
            ColorStateList.valueOf(ContextCompat.getColor(this, statusTint))
        )
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, statusTextColor))

        productsAdapter.submitList(state.products)
        binding.tvEmptyOwner.isVisible = state.products.isEmpty() && !state.loading && !state.productsLoading

        if (state.error != null) {
            Toast.makeText(this, state.error, Toast.LENGTH_LONG).show()
            viewModel.consumeError()
        }

        if (state.message != null) {
            Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    private fun openProductEditor(productId: String?) {
        lifecycleScope.launch {
            val categories = viewModel.ui.value.categories
            val result = viewModel.prepareProductEditor(productId)
            result.onFailure {
                Toast.makeText(
                    this@BusinessManageActivity,
                    it.message ?: getString(R.string.business_owner_error_loading_product),
                    Toast.LENGTH_LONG
                ).show()
            }
            result.onSuccess { data ->
                val dialog = BusinessProductEditorDialog.newInstance(data, categories)
                dialog.show(supportFragmentManager, DIALOG_PRODUCT_EDITOR)
            }
        }
    }

    private fun openBusinessInfoEditor() {
        val data = viewModel.prepareBusinessInfo()
        if (data == null) {
            Toast.makeText(
                this,
                getString(R.string.business_owner_error_loading_info),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val dialog = `BusinessInfoEditorDialog.kt`.newInstance(data)
        dialog.show(supportFragmentManager, DIALOG_INFO_EDITOR)
    }

    companion object {
        private const val DIALOG_PRODUCT_EDITOR = "product_editor"
        private const val DIALOG_INFO_EDITOR = "business_info_editor"
    }
}