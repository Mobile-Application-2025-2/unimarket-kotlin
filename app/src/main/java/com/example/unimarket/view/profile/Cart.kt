package com.example.unimarket.view.profile

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarket.R
import com.example.unimarket.viewmodel.CartViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class CartActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var rvCartItems: RecyclerView
    private lateinit var tvTotalValue: TextView
    private lateinit var btnOrder: MaterialButton
    private lateinit var tvPaymentEdit: TextView
    private lateinit var tvPaymentValue: TextView

    private lateinit var cartProductAdapter: CartProductAdapter
    private val viewModel: CartViewModel by viewModels()

    // Método de pago seleccionado
    private var currentPaymentMethod: String = "Efectivo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cart)

        initViews()
        setupRecycler()
        setupClicks()
        observeViewModel()
    }

    private fun initViews() {
        btnBack        = findViewById(R.id.btnBack)
        rvCartItems    = findViewById(R.id.rvCartItems)
        tvTotalValue   = findViewById(R.id.tvTotalValue)
        btnOrder       = findViewById(R.id.btnOrder)
        tvPaymentEdit  = findViewById(R.id.tvPaymentEdit)
        tvPaymentValue = findViewById(R.id.tvPaymentValue)
        tvPaymentValue.text = currentPaymentMethod
    }

    private fun setupRecycler() {
        cartProductAdapter = CartProductAdapter(
            onPlusClick = { item ->
                viewModel.increaseQuantity(item.id)
            },
            onMinusClick = { item ->
                viewModel.decreaseQuantity(item.id)
            },
            onRemoveClick = { item ->
                viewModel.removeProduct(item.id)
            }
        )

        rvCartItems.apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter = cartProductAdapter
        }
    }

    private fun setupClicks() {
        btnBack.setOnClickListener {
            finish()
        }

        btnOrder.setOnClickListener {
            viewModel.checkout(currentPaymentMethod)
        }

        tvPaymentEdit.setOnClickListener {
            showPaymentDropdown(it)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { uiItems ->
                    cartProductAdapter.submit(uiItems)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.total.collect { total ->
                    tvTotalValue.text = String.format("$%,.0f", total)
                }
            }
        }
    }

    private fun showPaymentDropdown(anchor: View) {
        val popup = PopupMenu(this, anchor)

        val methods = listOf("Efectivo", "Tarjeta")

        methods.forEachIndexed { index, method ->
            popup.menu.add(0, index, index, method)
        }

        popup.setOnMenuItemClickListener { item ->
            val selected = methods.getOrNull(item.itemId) ?: return@setOnMenuItemClickListener false
            currentPaymentMethod = selected
            tvPaymentValue.text = currentPaymentMethod
            true
        }

        popup.show()
    }
}