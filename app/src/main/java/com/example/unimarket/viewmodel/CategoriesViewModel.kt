package com.example.unimarket.viewmodel.categories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.model.domain.entity.Category
import com.example.unimarket.model.domain.service.CategoryService
import kotlinx.coroutines.launch

/**
 * ViewModel que reemplaza a CategoriesController.
 * Mantiene un UiState con loading/items/error y expone loadAll().
 */
class CategoriesViewModel(
    private val service: CategoryService = CategoryService()
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val items: List<Category> = emptyList(),
        val error: String? = null
    )

    private val _ui = MutableLiveData(UiState())
    val ui: LiveData<UiState> = _ui

    /** Equivalente a Controller.loadAll() */
    fun loadAll() {
        // set loading
        _ui.value = _ui.value?.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val res = service.listAll()
                res.onSuccess { list ->
                    _ui.value = UiState(loading = false, items = list, error = null)
                }.onFailure { e ->
                    _ui.value = UiState(
                        loading = false,
                        items = emptyList(),
                        error = e.message ?: "Error cargando categorías"
                    )
                }
            } catch (t: Throwable) {
                _ui.value = UiState(
                    loading = false,
                    items = emptyList(),
                    error = t.message ?: "Error cargando categorías"
                )
            }
        }
    }
}