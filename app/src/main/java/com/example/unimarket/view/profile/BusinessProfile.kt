package com.example.unimarket.view.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.unimarket.databinding.BusinessProfileBinding
import com.example.unimarket.view.auth.WelcomePage
import com.example.unimarket.viewmodel.BusinessNavDestination
import com.example.unimarket.viewmodel.BusinessViewModel
import kotlinx.coroutines.launch

class BusinessAccountActivity : AppCompatActivity() {

    private lateinit var b: BusinessProfileBinding
    private val vm: BusinessViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = BusinessProfileBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Si viene un nombre por Intent, úsalo como override visual
        intent.getStringExtra(EXTRA_USER_NAME)?.let { name ->
            if (name.isNotBlank()) b.tvUserName.text = name
        }

        fun ping(msg: String) =
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

        // Encabezado / marca
        b.brandBar.setOnClickListener { ping("Brand bar") }

        // Card Business Info
        b.rowBusinessInfo.setOnClickListener { ping("Business Info") }
        b.rowAddresses.setOnClickListener   { ping("Addresses") }

        // Card Compras
        b.rowOrdersHeader.setOnClickListener { ping("Compras - header") }
        b.rowOrder1.setOnClickListener       { ping("Compra 1") }
        b.rowOrder2.setOnClickListener       { ping("Compra 2") }
        b.tvSeeAll.setOnClickListener        { ping("Ver todas las compras") }

        // Card Acciones
        b.rowProducts.setOnClickListener { ping("Productos") }
        b.rowReviews.setOnClickListener  { ping("User Reviews") }

        // ✅ Logout via ViewModel (MVVM)
        b.rowLogout.setOnClickListener { vm.logout() }

        // Footer
        b.navHome.setOnClickListener    { ping("Home") }
        b.navBag.setOnClickListener     { ping("Bag") }
        b.navProfile.setOnClickListener { /* ya estás aquí */ }

        // Observa el estado del VM
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.ui.collect { ui ->
                    // Nombre visible (si no fue override por Intent)
                    if (intent.getStringExtra(EXTRA_USER_NAME).isNullOrBlank()) {
                        b.tvUserName.text = ui.displayName
                    }

                    ui.error?.let {
                        Toast.makeText(this@BusinessAccountActivity, it, Toast.LENGTH_LONG).show()
                        vm.clearNavAndError()
                    }

                    if (ui.nav == BusinessNavDestination.ToWelcome) {
                        val intent = Intent(this@BusinessAccountActivity, WelcomePage::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                        vm.clearNavAndError()
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_USER_NAME = "extra.USER_NAME"
    }
}