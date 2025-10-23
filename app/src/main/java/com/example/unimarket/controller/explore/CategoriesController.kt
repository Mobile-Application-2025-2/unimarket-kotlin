package com.example.unimarket.controller.explore

import com.example.unimarket.model.domain.entity.Category
import com.example.unimarket.model.domain.service.CategoryService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface CategoriesViewPort {
    fun setLoading(loading: Boolean)
    fun showCategories(items: List<Category>)
    fun showError(message: String)
}

class CategoriesController(
    private val view: CategoriesViewPort,
    private val service: CategoryService = CategoryService(),
    private val uiScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    fun loadAll() {
        view.setLoading(true)
        uiScope.launch {
            try {
                val res = service.listAll()
                res.onSuccess { list ->
                    view.showCategories(list)
                }.onFailure { e ->
                    view.showError(e.message ?: "Error cargando categorías")
                }
            } catch (t: Throwable) {
                view.showError(t.message ?: "Error cargando categorías")
            } finally {
                view.setLoading(false)
            }
        }
    }
}