package com.example.unimarket.controller.explore

import com.example.unimarket.model.entity.Category
import com.example.unimarket.model.repository.ExploreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface ExploreBuyerViewPort {
    fun showLoading(show: Boolean)
    fun showError(message: String)
    fun renderChips(labels: List<String>)
    fun renderCategories(list: List<Category>)
}

class ExploreBuyerController(
    private val view: ExploreBuyerViewPort,
    private val repo: ExploreRepository,
    private val uiScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private var all: List<Category> = emptyList()
    private var chips: List<String> = listOf("Todos")

    fun onInit() {
        view.showLoading(true)
        uiScope.launch {
            try {
                val types = all.map { it.type.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
                chips = listOf("Todos") + types
                view.renderChips(chips)
                view.renderCategories(all.sortedByDescending { it.selectionCount })
            } catch (t: Throwable) {
                view.showError(t.message ?: "No se pudo cargar Explorar")
            } finally {
                view.showLoading(false)
            }
        }
    }

    fun onChipSelected(index: Int) {
        if (index !in chips.indices) return
        val label = chips[index]
        val filtered = if (label == "Todos") all
        else all.filter { it.type.equals(label, ignoreCase = true) }
        view.renderCategories(filtered.sortedByDescending { it.selectionCount })
    }

    fun onRefresh() = onInit()
}
