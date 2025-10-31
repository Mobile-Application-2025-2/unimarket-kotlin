package com.example.unimarket.view.profile

import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.unimarket.R
import com.example.unimarket.model.session.SessionManager

class BuyerAccountActivity : AppCompatActivity() {

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

        // ---------- Nombre visible (sin asumir 'name' en SessionManager) ----------
        val session = SessionManager.get()
        // TODO: cuando tengamos BuyerViewModel + BuyerService, reemplazar por el nombre real.
        // Por ahora, usa alias por email si existe, o placeholder:
        val displayName = try {
            // intenta leer 'email' si existe en tu SessionManager; si no, usa placeholder
            val emailField = session?.javaClass?.getDeclaredField("email")?.apply { isAccessible = true }
            val email = emailField?.get(session) as? String
            email?.substringBefore('@')?.ifBlank { null } ?: "User Name Buyer"
        } catch (_: Exception) {
            "User Name Buyer"
        }
        tvUserName.text = displayName

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

        rowLogout.setOnClickListener {
            // TODO: logout real (AuthService.signOut + limpiar SessionManager) y volver al WelcomePage
        }

        navHome.setOnClickListener { finish() } // volver al Home
        navSearch.setOnClickListener { /* TODO: ir a búsqueda */ }
        navMap.setOnClickListener { /* TODO: ir a mapa */ }
        navProfile.setOnClickListener { /* ya estás aquí */ }
    }
}