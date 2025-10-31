package com.example.unimarket.ui.buyer

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.unimarket.databinding.ActivityBuyerAccountBinding

class BuyerAccountActivity : AppCompatActivity() {

    private lateinit var b: ActivityBuyerAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityBuyerAccountBinding.inflate(layoutInflater)
        setContentView(b.root)

        val userName = intent.getStringExtra(EXTRA_USER_NAME) ?: "User Name Buyer"
        b.tvUserName.text = userName

        fun ping(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

        b.brandBar.setOnClickListener { ping("Brand bar") }

        b.rowBusinessInfo.setOnClickListener { ping("Business Info") }
        b.rowAddresses.setOnClickListener   { ping("Addresses") }

        b.rowOrdersHeader.setOnClickListener { ping("Historial - header") }
        b.rowOrder1.setOnClickListener       { ping("Orden 1") }
        b.rowOrder2.setOnClickListener       { ping("Orden 2") }
        b.tvSeeAll.setOnClickListener        { ping("Ver todos") }

        b.rowProducts.setOnClickListener  { ping("Productos") }
        b.rowFavoritos.setOnClickListener { ping("Favoritos") }
        b.rowReviews.setOnClickListener   { ping("User Reviews") }
        b.rowLogout.setOnClickListener    { ping("Log Out") }

        b.navHome.setOnClickListener    { ping("Home") }
        b.navSearch.setOnClickListener  { ping("Search") }
        b.navMap.setOnClickListener     { ping("Map") }
        b.navProfile.setOnClickListener { ping("Profile") }
    }

    companion object {
        const val EXTRA_USER_NAME = "extra.USER_NAME"
    }
}