package com.example.unimarket.view.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarket.R
import com.example.unimarket.model.domain.entity.Business
import com.example.unimarket.view.profile.BuyerAccountActivity
import com.example.unimarket.viewmodel.HomeBuyerViewModel
import kotlinx.coroutines.launch

class HomeBuyerActivity : AppCompatActivity() {

    private val vm: HomeBuyerViewModel by viewModels()

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: BusinessAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Tu XML se llama "homepage.xml"
        setContentView(R.layout.homepage)

        recycler = findViewById(R.id.rvBusinesses)

        adapter = BusinessAdapter(
            items = emptyList(),
            onClick = { business: Business ->
                Toast.makeText(this, "Click: ${business.name}", Toast.LENGTH_SHORT).show()
                // TODO: navegar al detalle/lista de productos con business.id
            }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // Bottom nav -> Perfil
        findViewById<ImageButton>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, BuyerAccountActivity::class.java))
        }

        // Observa el estado del VM
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.ui.collect { ui ->
                    render(ui.items)
                    ui.error?.let { msg ->
                        Toast.makeText(this@HomeBuyerActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Cargar negocios
        vm.loadBusinesses()
    }

    private fun render(items: List<Business>) {
        adapter.submit(items)
        // Si algún día agregas empty view al layout, puedes controlar su visibilidad aquí.
        // findViewById<View>(R.id.emptyView)?.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }
}