package com.example.unimarket.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.model.data.cache.AllProductsMemoryCache
import com.example.unimarket.model.data.local.AppDatabase
import com.example.unimarket.model.data.local.entity.GlobalTopProductEntity
import com.example.unimarket.model.domain.entity.Product
import com.example.unimarket.model.domain.service.ProductService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExploreUiState(
    val products: List<Product> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class ExploreBuyerViewModel(
    application: Application,
    private val productService: ProductService
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ExploreBuyerViewModel"
        private const val GLOBAL_TOP_LIMIT = 20
    }

    // Constructor secundario para el factory por defecto
    constructor(application: Application) : this(
        application,
        ProductService()
    )

    // Room DB y DAO para top productos globales
    private val db by lazy { AppDatabase.getInstance(getApplication()) }
    private val globalTopProductDao by lazy { db.globalTopProductDao() }

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState

    // Lista completa en memoria para búsqueda local
    private var allProducts: List<Product> = emptyList()

    fun loadProducts(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            Log.d(TAG, "loadProducts: start (forceRefresh=$forceRefresh)")
            _uiState.update { it.copy(loading = true, error = null) }

            // 1) PRE-FILL opcional desde cache fresco (para respuesta rápida en modo online)
            val cachedFresh: List<Product>? =
                if (!forceRefresh) AllProductsMemoryCache.getAllIfFresh() else null

            if (!cachedFresh.isNullOrEmpty()) {
                Log.d(TAG, "loadProducts: prefill UI from cache FRESCO (size=${cachedFresh.size})")
                allProducts = cachedFresh
                _uiState.update {
                    it.copy(
                        products = cachedFresh,
                        loading = false,
                        error = null
                    )
                }
            } else {
                Log.d(TAG, "loadProducts: no hay cache fresco, UI dependerá de red/Room")
            }

            // 2) SIEMPRE intentar red (para refrescar o detectar que no hay internet)
            val result = productService.listAllProducts()

            if (result.isSuccess) {
                val list = result.getOrNull().orEmpty()
                Log.d(TAG, "loadProducts: éxito desde servicio (size=${list.size})")

                allProducts = list

                // 2.1 Actualizar cache global
                AllProductsMemoryCache.putAll(list)

                // 2.2 Actualizar TOP globales en Room (rating DESC)
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val sortedTop = list
                            .filter { it.rating > 0.0 }
                            .sortedByDescending { it.rating }
                            .take(GLOBAL_TOP_LIMIT)

                        val entities = sortedTop.map { GlobalTopProductEntity.fromDomain(it) }

                        globalTopProductDao.clearAll()
                        globalTopProductDao.insertAll(entities)

                        Log.d(
                            TAG,
                            "loadProducts: guardados ${entities.size} top productos globales en Room"
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "loadProducts: error guardando top global en Room", e)
                    }
                }

                // 2.3 Mostrar lista completa online
                _uiState.update {
                    it.copy(
                        products = list,
                        loading = false,
                        error = null
                    )
                }
            } else {
                // === AQUÍ ES DONDE MANEJAMOS EL CASO SIN INTERNET / ERROR ===
                val e = result.exceptionOrNull() ?: Exception("Error cargando productos")
                Log.e(TAG, "loadProducts: error desde servicio, se aplica fallback offline", e)

                // 3) Leer TOP global desde Room (ordenados por rating DESC)
                val roomTop: List<Product> = try {
                    withContext(Dispatchers.IO) {
                        globalTopProductDao
                            .getTopProducts(GLOBAL_TOP_LIMIT)
                            .map { it.toDomain() }
                            .also {
                                Log.d(
                                    TAG,
                                    "loadProducts: fallback Room TOP -> ${it.size} productos"
                                )
                            }
                    }
                } catch (roomEx: Exception) {
                    Log.e(TAG, "loadProducts: error leyendo top global desde Room", roomEx)
                    emptyList()
                }

                // 4) Obtener TODOS los productos del cache, ignorando TTL (pueden estar viejos)
                val cachedAll: List<Product> = AllProductsMemoryCache.getAllAllowStale()

                // 5) Construir lista final OFFLINE:
                //    - Primero los TOP de Room (ordenados por rating)
                //    - Luego el resto del cache que no esté ya en TOP
                val finalList: List<Product> = when {
                    roomTop.isNotEmpty() || cachedAll.isNotEmpty() -> {
                        val topIds = roomTop.map { it.id }.toSet()
                        val restFromCache = cachedAll.filter { it.id !in topIds }
                        val combined = roomTop + restFromCache
                        Log.d(
                            TAG,
                            "loadProducts: offline final -> top=${roomTop.size}, cacheExtra=${restFromCache.size}, total=${combined.size}"
                        )
                        combined
                    }
                    else -> emptyList()
                }

                if (finalList.isNotEmpty()) {
                    allProducts = finalList
                    _uiState.update {
                        it.copy(
                            products = finalList,
                            loading = false,
                            error = null
                        )
                    }
                } else {
                    // No hay ni Room ni cache usable
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = e.message ?: "Error cargando productos"
                        )
                    }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val base = allProducts
        if (query.isBlank()) {
            _uiState.update { it.copy(products = base) }
            return
        }

        val q = query.trim().lowercase()
        val filtered = base.filter { p ->
            p.name.contains(q, ignoreCase = true) ||
            p.description.contains(q, ignoreCase = true) ||
            p.category.contains(q, ignoreCase = true)
        }

        _uiState.update { it.copy(products = filtered) }
    }
}