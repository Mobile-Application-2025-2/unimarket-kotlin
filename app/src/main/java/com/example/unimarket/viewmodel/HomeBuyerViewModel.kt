package com.example.unimarket.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.model.domain.entity.Business
import com.example.unimarket.model.domain.entity.Category
import com.example.unimarket.model.domain.entity.Product
import com.example.unimarket.model.domain.service.BusinessService
import com.example.unimarket.model.domain.service.CategoryService
import com.example.unimarket.model.domain.service.ProductService
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

// Room local
import com.example.unimarket.model.data.local.AppDatabase
import com.example.unimarket.model.data.local.entity.TopBusinessLocalEntity
import com.example.unimarket.model.data.local.entity.TopProductLocalEntity

// Cache
import com.example.unimarket.model.data.cache.BusinessMemoryCache
import com.example.unimarket.model.data.cache.ProductMemoryCache

enum class Filter { ALL, FOOD, STATIONERY, TUTORING, ACCESSORIES }

data class HomeUiState(
    val businessesAll: List<Business> = emptyList(),
    val businessesFiltered: List<Business> = emptyList(),
    val selected: Filter = Filter.ALL,
    val loading: Boolean = false,
    val error: String? = null
)

data class BusinessDetailUiState(
    val businessId: String = "",
    val businessName: String = "",
    val currentFilter: String = HomeBuyerViewModel.FILTER_ALL,
    val categories: List<String> = emptyList(),
    val products: List<Product> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

sealed class HomeNav {
    data object None : HomeNav()
    data object ToBuyerProfile : HomeNav()
    data class ToBusinessDetail(val business: Business) : HomeNav()
}

class HomeBuyerViewModel(
    application: Application,
    private val service: BusinessService,
    private val categoryService: CategoryService,
    private val productService: ProductService
) : AndroidViewModel(application) {

    // Constructor secundario requerido por AndroidViewModelFactory
    constructor(application: Application) : this(
        application,
        BusinessService(),
        CategoryService(),
        ProductService()
    )

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui

    private val _nav = MutableStateFlow<HomeNav>(HomeNav.None)
    val nav: StateFlow<HomeNav> = _nav

    private val _detail = MutableStateFlow(BusinessDetailUiState())
    val detail: StateFlow<BusinessDetailUiState> = _detail

    private var loadJob: Job? = null

    private val detailPrefs = com.example.unimarket.model.data.local.DetailPrefs(getApplication())
    private val prefs = com.example.unimarket.model.data.local.HomePrefs(getApplication())
    private val offlineClicks = com.example.unimarket.model.data.local.OfflineClicks(getApplication())

    private val CACHE_TTL_MILLIS = 5 * 60 * 1000L

    init {
        viewModelScope.launch {
            prefs.filterFlow.collect { saved ->
                val f = runCatching { Filter.valueOf(saved) }.getOrDefault(Filter.ALL)
                _ui.update { it.copy(selected = f) }
            }
        }
        reloadBusinesses(remoteFirst = true)
    }

    // --- Carga genérica (aún la dejamos por si la usas en algún sitio) ---
    private fun loadBusinesses() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }

            service.getAllBusinesses()
                .onSuccess { list ->
                    val asIs = list

                    BusinessMemoryCache.updateAll(asIs)

                    _ui.update { st ->
                        val filtered = filterBy(st.selected, asIs)
                        st.copy(
                            businessesAll = asIs,
                            businessesFiltered = filtered,
                            loading = false,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    if (e is CancellationException) {
                        _ui.update { it.copy(loading = false) }
                        return@onFailure
                    }

                    try {
                        val ctx = getApplication<Application>().applicationContext
                        val db = AppDatabase.getInstance(ctx)
                        val topLocal = db.topBusinessDao().getAllOnce()

                        if (topLocal.isEmpty()) {
                            _ui.update {
                                it.copy(
                                    loading = false,
                                    error = e.message ?: "Error cargando negocios"
                                )
                            }
                            return@onFailure
                        }

                        val mapped = topLocal.map { it.toDomainBusiness() }

                        BusinessMemoryCache.updateAll(mapped)

                        _ui.update { st ->
                            val filtered = filterBy(st.selected, mapped)
                            st.copy(
                                businessesAll = mapped,
                                businessesFiltered = filtered,
                                loading = false,
                                error = null
                            )
                        }
                    } catch (t: Throwable) {
                        _ui.update {
                            it.copy(
                                loading = false,
                                error = e.message ?: "Error cargando negocios"
                            )
                        }
                    }
                }
        }
    }

    fun reloadBusinessesRemoteFirst() = loadRemoteFirst()

    /**
     * remoteFirst = true  → hay internet, intentamos cache primero y luego remoto.
     * remoteFirst = false → no hay internet, vamos directo a Room + cache (loadLocalOnly).
     */
    fun reloadBusinesses(remoteFirst: Boolean) {
        // 🔌 Sin internet → modo offline: Room (top) + cache
        if (!remoteFirst) {
            loadLocalOnly()
            return
        }

        // 🌐 Con internet → intentamos primero cache en memoria (LruCache) SIN ordenar
        val cached = BusinessMemoryCache.snapshotIfFresh(CACHE_TTL_MILLIS)
        if (cached != null && cached.isNotEmpty()) {
            loadJob?.cancel()

            val asIs = cached

            _ui.update { st ->
                val filtered = filterBy(st.selected, asIs)
                st.copy(
                    businessesAll = asIs,
                    businessesFiltered = filtered,
                    loading = false,
                    error = null
                )
            }
            return
        }

        // Si no hay cache fresca → vamos a remoto
        loadRemoteFirst()
    }

    /**
     * Carga remota desde Firestore (usa BusinessService) y actualiza cache + UI.
     * No ordena por rating, respeta el orden que devuelva el backend.
     */
    private fun loadRemoteFirst() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            android.util.Log.d("HomeVM", "loadRemoteFirst: llamando a getAllBusinesses()")

            service.getAllBusinesses()
                .onSuccess { list ->
                    val asIs = list

                    // Actualizamos cache en memoria con el orden tal cual se recibe
                    BusinessMemoryCache.updateAll(asIs)

                    _ui.update { st ->
                        val filtered = filterBy(st.selected, asIs)
                        st.copy(
                            businessesAll = asIs,
                            businessesFiltered = filtered,
                            loading = false,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    if (e is CancellationException) {
                        _ui.update { it.copy(loading = false) }
                        return@onFailure
                    }
                    // Fallback local si el remoto falla
                    loadLocalOnly()
                }
        }
    }

    /**
     * Modo offline / fallback:
     * - Lee TOP de Room (2 mejores por categoría) → las ordena por rating desc.
     * - Luego añade lo que haya en cache en memoria (LruCache), sin duplicar IDs.
     * Resultado: primero las mejores (Room), luego el resto de cache.
     */
    private fun loadLocalOnly() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val ctx = getApplication<Application>().applicationContext
                val db  = AppDatabase.getInstance(ctx)
                val top = db.topBusinessDao().getAllOnce()

                // 1) TOP de Room (ya guardaste 2 mejores por categoría)
                val topMapped = top.map { it.toDomainBusiness() }
                val topSorted = topMapped.sortedByDescending { it.rating }

                // 2) Cache en memoria (ignoramos TTL usando Long.MAX_VALUE)
                val cached: List<Business> =
                    BusinessMemoryCache.snapshotIfFresh(Long.MAX_VALUE) ?: emptyList()

                // 3) Merge: primero TOP de Room, luego cache, sin duplicar IDs
                val seenIds = mutableSetOf<String>()
                val merged  = mutableListOf<Business>()

                for (b in topSorted) {
                    val id = b.id.trim()
                    if (id.isNotEmpty() && seenIds.add(id)) {
                        merged.add(b)
                    }
                }

                for (b in cached) {
                    val id = b.id.trim()
                    if (id.isNotEmpty() && seenIds.add(id)) {
                        merged.add(b)
                    }
                }

                if (merged.isEmpty()) {
                    _ui.update {
                        it.copy(
                            businessesAll = emptyList(),
                            businessesFiltered = emptyList(),
                            loading = false,
                            error = "Sin datos locales"
                        )
                    }
                    return@launch
                }

                // Actualizamos también cache en memoria con el orden offline (top + resto)
                BusinessMemoryCache.updateAll(merged)

                _ui.update { st ->
                    val filtered = filterBy(st.selected, merged)
                    st.copy(
                        businessesAll = merged,
                        businessesFiltered = filtered,
                        loading = false,
                        error = null
                    )
                }
            } catch (t: Throwable) {
                if (t is CancellationException) {
                    _ui.update { it.copy(loading = false) }
                } else {
                    _ui.update {
                        it.copy(
                            loading = false,
                            error = t.message ?: "Error leyendo datos locales"
                        )
                    }
                }
            }
        }
    }

    // --------- FILTROS / NAVEGACIÓN ---------

    fun onFilterSelected(filter: Filter) {
        _ui.update { st ->
            val filtered = filterBy(filter, st.businessesAll)
            st.copy(selected = filter, businessesFiltered = filtered)
        }
        viewModelScope.launch { prefs.saveFilter(filter.name) }
        incrementCategoryCount(filter)
    }

    fun onCategoryClicked(categoryId: String) {
        val id = categoryId.trim().lowercase()
        val mapped = when {
            id.contains("food") || id.contains("comida") -> Filter.FOOD
            id.contains("stationery") || id.contains("papeler") || id.contains("copias") -> Filter.STATIONERY
            id.contains("tutor") -> Filter.TUTORING
            id.contains("accessor") || id.contains("accesor") -> Filter.ACCESSORIES
            else -> Filter.ALL
        }
        onFilterSelected(mapped)
    }

    fun onBusinessSelected(business: Business) {
        if (business.id.isBlank()) return
        _nav.value = HomeNav.ToBusinessDetail(business)
    }

    fun onClickProfile() { _nav.value = HomeNav.ToBuyerProfile }
    fun navHandled()     { _nav.value = HomeNav.None }

    // --------- DETALLE ---------

    fun detail_init(businessId: String, businessName: String) {
        _detail.value = BusinessDetailUiState(
            businessId = businessId,
            businessName = businessName,
            currentFilter = FILTER_ALL
        )
        // restaurar filtro guardado por negocio
        viewModelScope.launch {
            detailPrefs.filterFlow(businessId).collect { saved ->
                _detail.update { it.copy(currentFilter = saved) }
            }
        }
    }

    fun detail_loadProducts(productIds: List<String>) {
        viewModelScope.launch {
            _detail.update { it.copy(loading = true, error = null) }

            val bid = detail.value.businessId

            // 1) Intentar leer productos desde cache LRU en memoria
            if (bid.isNotBlank()) {
                android.util.Log.d("HomeVM", "detail_loadProducts: intentando cache LRU para businessId=$bid")
                val cached = ProductMemoryCache.snapshotIfFresh(bid, CACHE_TTL_MILLIS)
                if (cached != null && cached.isNotEmpty()) {
                    val cats = cached.map { it.category.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                        .sorted()

                    _detail.update {
                        it.copy(
                            products = cached,
                            categories = cats,
                            loading = false,
                            error = null
                        )
                    }
                    // Ya servimos desde cache → no vamos ni a red ni a Room
                    return@launch
                }
            }

            // 2) Lógica normal (remote + RecentProducts + fallback local)
            val ctx = getApplication<Application>()
            val rp  = com.example.unimarket.model.data.local.RecentProducts(ctx, detail.value.businessId)

            val ids = if (productIds.isNotEmpty()) productIds else rp.load()

            if (ids.isNotEmpty()) {
                productService.listByIds(ids)
                    .onSuccess { list ->
                        // guardar ids recientes
                        rp.save(list.map { it.id })

                        val cats = list.map { it.category.trim() }
                            .filter { it.isNotEmpty() }
                            .distinct()
                            .sorted()

                        // Guardar en cache LRU por negocio
                        if (bid.isNotBlank()) {
                            ProductMemoryCache.updateForBusiness(bid, list)
                        }

                        _detail.update {
                            it.copy(
                                products = list,
                                categories = cats,
                                loading = false,
                                error = null
                            )
                        }
                    }
                    .onFailure {
                        // Fallback → Room (top products)
                        loadProductsLocalOnly()
                    }
            } else {
                // No tengo ni ids → Room directamente
                loadProductsLocalOnly()
            }
        }
    }

    fun detail_reloadLocalIfRemoteEmpty() {
        val st = detail.value
        if (st.products.isEmpty() && st.businessId.isNotBlank()) {
            viewModelScope.launch { loadProductsLocalOnly() }
        }
    }

    private suspend fun loadProductsLocalOnly() {
        val st = detail.value
        val bid = st.businessId
        if (bid.isBlank()) {
            _detail.update { it.copy(loading = false, error = "Negocio inválido") }
            return
        }
        try {
            val ctx = getApplication<Application>().applicationContext
            val db  = AppDatabase.getInstance(ctx)
            val dao = db.topProductDao()

            // Lee todos los top-products del negocio (ya guardados por subcategoría)
            val locals: List<TopProductLocalEntity> = dao.getByBusinessOnce(bid)

            if (locals.isEmpty()) {
                _detail.update { it.copy(loading = false, error = null, products = emptyList()) }
                return
            }

            val mapped: List<Product> = locals.map { it.toDomainProduct() }

            val cats = mapped.map { it.category.trim() }
                .filter { it.isNotEmpty() }
                .distinct().sorted()

            ProductMemoryCache.updateForBusiness(bid, mapped)

            _detail.update {
                it.copy(
                    products = mapped,
                    categories = cats,
                    loading = false,
                    error = null // viene de cache local → no mostrar error
                )
            }
        } catch (t: Throwable) {
            if (t is CancellationException) {
                _detail.update { it.copy(loading = false) }
            } else {
                _detail.update { it.copy(loading = false, error = t.message ?: "Error leyendo productos locales") }
            }
        }
    }

    fun detail_onFilterSelected(filterTag: String) {
        _detail.update { st -> st.copy(currentFilter = filterTag) }
        viewModelScope.launch { detailPrefs.saveFilter(detail.value.businessId, filterTag) }
    }

    fun detail_errorShown() {
        _detail.update { it.copy(error = null) }
    }

    // --------- Helpers de negocio / categorías ---------

    private fun filterBy(filter: Filter, list: List<Business>): List<Business> =
        when (filter) {
            Filter.ALL -> list
            Filter.FOOD -> list.filter { it.matchesAny("food", "comida", "rest", "snack") }
            Filter.STATIONERY -> list.filter { it.matchesAny("stationery", "papeler", "copias", "impresion", "impresión") }
            Filter.TUTORING -> list.filter { it.matchesAny("tutor", "clases", "curso") }
            Filter.ACCESSORIES -> list.filter { it.matchesAny("accessor", "accesor") }
        }

    private fun Business.matchesAny(vararg keys: String): Boolean =
        categories.any { it.matchesAny(*keys) }

    private fun Category.matchesAny(vararg keys: String): Boolean {
        val n = name.trim().lowercase()
        val i = id.trim().lowercase()
        return keys.any { k -> n.contains(k) || i.contains(k) }
    }

    private fun incrementCategoryCount(filter: Filter) {
        val docId = when (filter) {
            Filter.FOOD -> "comida"
            Filter.STATIONERY -> "papeleria"
            Filter.TUTORING -> "tutorias"
            Filter.ACCESSORIES -> "accesorios"
            Filter.ALL -> null
        } ?: return

        viewModelScope.launch {
            try {
                categoryService.update(docId, mapOf("count" to FieldValue.increment(1)))
            } catch (_: Exception) {
                offlineClicks.enqueue(docId)
            }
        }
    }

    fun flushOfflineCategoryClicks() {
        viewModelScope.launch {
            val docIds = offlineClicks.drain()
            docIds.groupingBy { it }.eachCount().forEach { (id, times) ->
                repeat(times) {
                    runCatching {
                        categoryService.update(id, mapOf("count" to FieldValue.increment(1)))
                    }
                }
            }
        }
    }

    fun rateBusiness(businessId: String, newAvg: Float, newCount: Int) {
        if (businessId.isBlank()) return
        viewModelScope.launch {
            service.updateRating(
                businessId = businessId,
                rating = newAvg.toDouble(),
                amountRatings = newCount.toLong()
            ).onSuccess {
                _ui.update { st ->
                    val updatedAll = st.businessesAll.map { b ->
                        if (b.id == businessId) {
                            val updated = b.copy(
                                rating = newAvg.toDouble(),
                                amountRatings = newCount.toLong()
                            )
                            BusinessMemoryCache.put(businessId, updated)
                            updated
                        } else {
                            b
                        }
                    }
                    st.copy(
                        businessesAll = updatedAll,
                        businessesFiltered = filterBy(st.selected, updatedAll)
                    )
                }
            }.onFailure { e ->
                _detail.update { it.copy(error = e.message ?: "Error actualizando calificación") }
            }
        }
    }

    companion object { const val FILTER_ALL = "all" }
}

private fun TopBusinessLocalEntity.toDomainBusiness(): Business {
    val productIds: List<String> =
        this.productIdsCsv?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    return Business(
        id = this.businessId,
        name = this.businessName,
        rating = this.rating,
        amountRatings = this.amountRatings,
        logo = this.logoUrl ?: "",
        products = productIds,
        categories = listOf(
            Category(id = this.categoryId, name = this.categoryName, count = 0L)
        ),
        address = com.example.unimarket.model.domain.entity.Address()
    )
}

private fun TopProductLocalEntity.toDomainProduct(): Product {
    return Product(
        id          = productId,
        name        = productName,
        price       = price,
        description = "",
        category    = subcategory,
        business    = businessId,
        rating      = rating,
        comments    = emptyList(),
        image       = imageUrl ?: ""
    )
}