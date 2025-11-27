package com.example.unimarket.model.data.cache

import android.util.LruCache
import com.example.unimarket.model.domain.entity.Product

/**
 * Cache en memoria para productos por negocio usando LruCache.
 *
 * Clave: businessId
 * Valor: Entry con lista de productos + timestamp de última actualización.
 */
object ProductMemoryCache : LruCache<String, ProductMemoryCache.Entry>(100) {

    data class Entry(
        val products: List<Product>,
        val lastUpdateMillis: Long
    )

    private const val MAX_SIZE = 50 // máx. negocios con productos cacheados

    override fun sizeOf(key: String, value: Entry): Int {
        // Cada businessId cuenta como 1 entrada
        return 1
    }

    /**
     * Reemplaza o crea la entrada de productos para un negocio.
     */
    fun updateForBusiness(businessId: String, products: List<Product>) {
        if (businessId.isBlank()) return
        if (products.isEmpty()) {
            remove(businessId)
            android.util.Log.d("ProductMemoryCache", "updateForBusiness: remove($businessId)")
            return
        }
        val entry = Entry(products, System.currentTimeMillis())
        put(businessId, entry)
        android.util.Log.d(
            "ProductMemoryCache",
            "updateForBusiness: $businessId -> ${products.size} productos"
        )
    }

    /**
     * Devuelve los productos cacheados para un negocio si:
     *  - Hay entrada
     *  - No está expirada según el TTL
     *  - La lista no está vacía
     * Si no se cumple, devuelve null.
     */
    fun snapshotIfFresh(businessId: String, ttlMillis: Long): List<Product>? {
        val entry = get(businessId) ?: run {
            android.util.Log.d("ProductMemoryCache", "snapshotIfFresh: MISS (no entry) $businessId")
            return null
        }
        val age = System.currentTimeMillis() - entry.lastUpdateMillis
        if (age > ttlMillis) {
            android.util.Log.d("ProductMemoryCache", "snapshotIfFresh: MISS (expirado $businessId, age=$age)")
            return null
        }
        if (entry.products.isEmpty()) {
            android.util.Log.d("ProductMemoryCache", "snapshotIfFresh: MISS (lista vacía $businessId)")
            return null
        }
        android.util.Log.d(
            "ProductMemoryCache",
            "snapshotIfFresh: HIT ($businessId, size=${entry.products.size})"
        )
        return entry.products
    }

    fun clearForBusiness(businessId: String) {
        remove(businessId)
    }
}