package com.example.unimarket.ui.business

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.unimarket.databinding.ActivityBusinessAccountBinding

class BusinessAccountActivity : AppCompatActivity() {

    private lateinit var b: ActivityBusinessAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityBusinessAccountBinding.inflate(layoutInflater)
        setContentView(b.root)

        // --- Demo: podrías traer estos datos por intent o ViewModel
        val userName = intent.getStringExtra(EXTRA_USER_NAME) ?: "User Name Business"
        b.tvUserName.text = userName
        // Si luego usas Glide/Coil para el avatar, aquí iría la carga.

        // ---------- Clicks sin navegación (solo feedback visual) ----------
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
        b.rowLogout.setOnClickListener   { ping("Log Out") }

        // Footer (bottom pill)
        b.navHome.setOnClickListener    { ping("Home") }
        b.navBag.setOnClickListener     { ping("Bag") }
        b.navProfile.setOnClickListener { ping("Profile") }
    }

    companion object {
        const val EXTRA_USER_NAME = "extra.USER_NAME"
    }
}