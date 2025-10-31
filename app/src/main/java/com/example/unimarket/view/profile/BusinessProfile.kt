package com.example.unimarket.view.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.unimarket.databinding.BusinessProfileBinding

class BusinessAccountActivity : AppCompatActivity() {

    private lateinit var b: BusinessProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = BusinessProfileBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Demo: nombre por Intent (usa EXTRA_USER_NAME) o deja el del XML
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
        b.rowLogout.setOnClickListener   { ping("Log Out") }

        // Footer
        b.navHome.setOnClickListener    { ping("Home") }
        b.navBag.setOnClickListener     { ping("Bag") }
        b.navProfile.setOnClickListener { ping("Profile") }
    }

    companion object {
        const val EXTRA_USER_NAME = "extra.USER_NAME"
    }
}