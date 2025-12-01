package com.example.unimarket.view.profile

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

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
            // SOLO aquí se valida red antes de intentar el checkout
            if (!isOnline()) {
                Toast.makeText(
                    this,
                    "Sin conexión. Intenta de nuevo cuando tengas internet.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

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

        // Mensajes de error del ViewModel (por ejemplo, fallo al hacer checkout)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { msg ->
                    if (msg != null) {
                        Toast.makeText(this@CartActivity, msg, Toast.LENGTH_LONG).show()
                        viewModel.errorShown()
                    }
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

    // --- Conectividad ---

    private fun isOnline(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false

        // Red válida con capacidad de internet y validada
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun registerNetworkCallback() {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        connectivityManager = cm

        if (networkCallback != null) return

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // SOLO mensaje positivo cuando vuelve la conexión
                runOnUiThread {
                    Toast.makeText(
                        this@CartActivity,
                        "Conexión restaurada. Ya puedes hacer tu compra.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // No mostramos mensaje automático cuando se pierde la red:
            // el mensaje de "sin conexión" solo sale al oprimir el botón.
        }

        cm.registerDefaultNetworkCallback(networkCallback!!)
    }

    private fun unregisterNetworkCallback() {
        val cm = connectivityManager
        val cb = networkCallback
        if (cm != null && cb != null) {
            cm.unregisterNetworkCallback(cb)
        }
        networkCallback = null
    }

    override fun onStart() {
        super.onStart()
        registerNetworkCallback()
    }

    override fun onStop() {
        super.onStop()
        unregisterNetworkCallback()
    }
}