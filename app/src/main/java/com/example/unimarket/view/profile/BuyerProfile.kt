package com.example.unimarket.view.profile

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.unimarket.R
import com.example.unimarket.view.auth.WelcomePage
import com.example.unimarket.viewmodel.BuyerNavDestination
import com.example.unimarket.viewmodel.BuyerViewModel
import kotlinx.coroutines.launch

class BuyerAccountActivity : AppCompatActivity() {

    private val vm: BuyerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Usa el nombre REAL de tu layout de perfil de comprador
        setContentView(R.layout.buyer_profile)

        // --------- Header / Brand bar ----------
        val btnFavorites: ImageButton = findViewById(R.id.btnFavorites)
        val btnOrdersTop: ImageButton = findViewById(R.id.btnOrdersTop)

        // --------- Header user ----------
        val tvUserName: TextView = findViewById(R.id.tvUserName)

        // --------- Card Business Info ----------
        val rowBusinessInfo: LinearLayout = findViewById(R.id.rowBusinessInfo)
        val rowAddresses: LinearLayout = findViewById(R.id.rowAddresses)

        // --------- Card Orders ----------
        val rowOrdersHeader: LinearLayout = findViewById(R.id.rowOrdersHeader)
        val rowOrder1: LinearLayout = findViewById(R.id.rowOrder1)
        val rowOrder2: LinearLayout = findViewById(R.id.rowOrder2)
        val tvSeeAll: TextView = findViewById(R.id.tvSeeAll)

        // --------- Card Actions ----------
        val rowProducts: LinearLayout = findViewById(R.id.rowProducts)
        val rowFavoritos: LinearLayout = findViewById(R.id.rowFavoritos)
        val rowReviews: LinearLayout = findViewById(R.id.rowReviews)
        val rowLogout: LinearLayout = findViewById(R.id.rowLogout)

        // --------- Bottom Nav ----------
        val navHome: ImageButton = findViewById(R.id.nav_home)
        val navSearch: ImageButton = findViewById(R.id.nav_search)
        val navMap: ImageButton = findViewById(R.id.nav_map)
        val navProfile: ImageButton = findViewById(R.id.nav_profile)

        // ---------- Clicks básicos (placeholder) ----------
        btnFavorites.setOnClickListener { /* TODO: ir a favoritos */ }
        btnOrdersTop.setOnClickListener { /* TODO: ir a pedidos */ }

        rowBusinessInfo.setOnClickListener { /* TODO: editar info de usuario */ }
        rowAddresses.setOnClickListener { /* TODO: administrar direcciones */ }

        rowOrdersHeader.setOnClickListener { /* TODO: historial de pedidos */ }
        rowOrder1.setOnClickListener { /* TODO: detalle pedido */ }
        rowOrder2.setOnClickListener { /* TODO: detalle pedido */ }
        tvSeeAll.setOnClickListener { /* TODO: ver todos los pedidos */ }

        rowProducts.setOnClickListener { /* TODO: productos comprados */ }
        rowFavoritos.setOnClickListener { /* TODO: favoritos */ }
        rowReviews.setOnClickListener { /* TODO: reseñas */ }

        // ---------- Logout por MVVM ----------
        rowLogout.setOnClickListener { vm.logout() }

        // ---------- Bottom nav ----------
        navHome.setOnClickListener { finish() } // volver al Home
        navSearch.setOnClickListener { /* TODO: ir a búsqueda */ }
        navMap.setOnClickListener { /* TODO: ir a mapa */ }
        navProfile.setOnClickListener { /* ya estás aquí */ }

        // ---------- Observa estado del VM ----------
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.ui.collect { ui ->
                    // Nombre visible
                    tvUserName.text = ui.displayName

                    // Errores (si los hay)
                    ui.error?.let {
                        Toast.makeText(this@BuyerAccountActivity, it, Toast.LENGTH_LONG).show()
                        vm.clearNavAndError()
                    }

                    // Navegación one-shot tras logout
                    if (ui.nav == BuyerNavDestination.ToWelcome) {
                        val intent = Intent(this@BuyerAccountActivity, WelcomePage::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        // no vuelvas a esta actividad
                        finish()
                        vm.clearNavAndError()
                    }
                }
            }
        }
    }
}