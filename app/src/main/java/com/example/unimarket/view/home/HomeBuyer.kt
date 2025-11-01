package com.example.unimarket.view.home

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
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
import com.example.unimarket.view.map.BusinessMapActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class HomeBuyerActivity : AppCompatActivity() {

    private val viewModel: HomeBuyerViewModel by viewModels()

    private lateinit var chipGroup: ChipGroup
    private lateinit var rvBusinesses: RecyclerView
    private lateinit var businessAdapter: BusinessAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homepage) // tu layout

        chipGroup = findViewById(R.id.chipGroupFilters)
        rvBusinesses = findViewById(R.id.rvBusinesses)

        // Adapter de negocios
        businessAdapter = BusinessAdapter(
            items = emptyList(),
            onClick = { business: Business ->
                if (business.id.isBlank()) {
                    Toast.makeText(this, R.string.business_detail_missing, Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(
                        Intent(this, BusinessDetailActivity::class.java).apply {
                            putExtra(BusinessDetailActivity.EXTRA_BUSINESS_ID, business.id)
                        }
                    )
                }
            }
        )
        rvBusinesses.apply {
            layoutManager = LinearLayoutManager(this@HomeBuyerActivity)
            adapter = businessAdapter
            setHasFixedSize(true)
        }

        // Colores checked/unchecked del fondo de los chips (solo fondo, como pediste)
        val chipBg = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(getColor(R.color.yellowLight), getColor(android.R.color.white))
        )
        for (i in 0 until chipGroup.childCount) {
            (chipGroup.getChildAt(i) as? Chip)?.chipBackgroundColor = chipBg
        }

        // Listener de filtros del ChipGroup -> VM
        setupFilterChips()

        // Footer: navega al perfil de comprador vía VM (MVVM-friendly)
        findViewById<ImageButton>(R.id.nav_profile).setOnClickListener {
            viewModel.onClickProfile()
        }

        // Observa estado de UI (lista + errores)
        observeUi()

        // Observa navegación
        observeNav()

        val navMap: ImageButton = findViewById(R.id.nav_map)
        navMap.setOnClickListener { startActivity(Intent(this, BusinessMapActivity::class.java)) }

    }

    private fun setupFilterChips() {
        // Mapa ID de chip -> Filter
        val filterMap = mapOf(
            R.id.chip_all to Filter.ALL,
            R.id.chip_food to Filter.FOOD,
            R.id.chip_stationery to Filter.STATIONERY,
            R.id.chip_tutoring to Filter.TUTORING,
            R.id.chip_accessories to Filter.ACCESSORIES
        )

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedId = checkedIds.firstOrNull()
            val filter = filterMap[selectedId] ?: Filter.ALL
            viewModel.onFilterSelected(filter)
        }

        // Asegurar que "Todos" esté seleccionado por defecto si nada lo está
        if (chipGroup.checkedChipId == -1) {
            chipGroup.findViewById<Chip>(R.id.chip_all)?.let { chipGroup.check(it.id) }
        }
    }

    private fun observeUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ui.collect { ui ->
                    // pinta negocios filtrados
                    businessAdapter.submit(ui.businessesFiltered)

                    // muestra error si existe
                    ui.error?.let { msg ->
                        Toast.makeText(this@HomeBuyerActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun observeNav() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.nav.collect { nav ->
                    when (nav) {
                        is HomeNav.ToBuyerProfile -> {
                            startActivity(
                                Intent(
                                    this@HomeBuyerActivity,
                                    com.example.unimarket.view.profile.BuyerAccountActivity::class.java
                                )
                            )
                            viewModel.navHandled()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}