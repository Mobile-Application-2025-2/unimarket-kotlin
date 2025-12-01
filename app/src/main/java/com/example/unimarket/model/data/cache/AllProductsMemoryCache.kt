package com.example.unimarket.model.data.cache

import android.util.LruCache
import android.util.Log
import com.example.unimarket.model.domain.entity.Product

/**
 * Cache global en memoria para TODOS los productos, independientes del negocio.
 *
 * Clave: productId
 * Valor: Product
 *
 * Mantiene además un timestamp global de la última actualización para controlar frescura.
 */
object AllProductsMemoryCache {

    private const val TAG = "AllProductsMemoryCache"

    // Máximo de productos en cache (ajusta según tamaño real)
    private const val MAX_ENTRIES = 500

    // TTL por defecto de frescura: 5 minutos
    private const val DEFAULT_TTL_MS: Long = 5 * 60 * 1000L

    private val lruCache = object : LruCache<String, Product>(MAX_ENTRIES) {
        override fun sizeOf(key: String, value: Product): Int = 1
    }

    @Volatile
    private var lastUpdate: Long = 0L

    /**
     * Reemplaza el contenido del cache con una lista completa de productos.
     */
    fun putAll(products: List<Product>) {
        if (products.isEmpty()) {
            Log.d(TAG, "putAll: lista vacía, se limpia el cache")
            clear()
            return
        }

        synchronized(this) {
            lruCache.evictAll()
            for (p in products) {
                if (p.id.isNotBlank()) {
                    lruCache.put(p.id, p)
                }
            }
            lastUpdate = System.currentTimeMillis()
            Log.d(
                TAG,
                "putAll: se cachean ${products.size} productos (lastUpdate=$lastUpdate)"
            )
        }
    }

    /**
     * Guarda o reemplaza un producto individual en el cache.
     * Útil cuando se actualiza precio, rating, etc. de un solo producto.
     */
    fun put(product: Product) {
        if (product.id.isBlank()) {
            Log.d(TAG, "put: producto sin id, no se cachea")
            return
        }
        synchronized(this) {
            lruCache.put(product.id, product)
            if (lastUpdate == 0L) {
                lastUpdate = System.currentTimeMillis()
            }
            Log.d(TAG, "put: producto ${product.id} cacheado/actualizado")
        }
    }

    /**
     * Devuelve todos los productos del cache si:
     *  - El cache no está vacío
     *  - El timestamp global no está expirado según TTL
     *
     * Si algo falla, devuelve null.
     */
    fun getAllIfFresh(ttlMs: Long = DEFAULT_TTL_MS): List<Product>? {
        val now = System.currentTimeMillis()
        synchronized(this) {
            val size = lruCache.size()
            if (size == 0) {
                Log.d(TAG, "getAllIfFresh: MISS (cache vacío)")
                return null
            }
            if (lastUpdate == 0L) {
                Log.d(TAG, "getAllIfFresh: MISS (sin lastUpdate)")
                return null
            }

            val age = now - lastUpdate
            if (age > ttlMs) {
                Log.d(TAG, "getAllIfFresh: MISS (expirado, age=${age}ms, ttl=${ttlMs}ms)")
                return null
            }

            val snapshot = ArrayList(lruCache.snapshot().values)
            Log.d(
                TAG,
                "getAllIfFresh: HIT (size=$size, age=${age}ms, ttl=${ttlMs}ms)"
            )
            return snapshot
        }
    }

    /**
     * Devuelve un producto específico (sin chequear frescura global).
     */
    fun get(productId: String): Product? {
        if (productId.isBlank()) {
            Log.d(TAG, "get: productId en blanco")
            return null
        }
        synchronized(this) {
            val product = lruCache.get(productId)
            if (product == null) {
                Log.d(TAG, "get: MISS ($productId)")
            } else {
                Log.d(TAG, "get: HIT ($productId)")
            }
            return product
        }
    }

    /**
     * Limpia completamente el cache.
     */
    fun clear() {
        synchronized(this) {
            lruCache.evictAll()
            lastUpdate = 0L
            Log.d(TAG, "clear: cache limpiado")
        }
    }

    /**
     * Verifica solo si el cache está fresco (para debug/uso adicional).
     */
    fun isFresh(ttlMs: Long = DEFAULT_TTL_MS): Boolean {
        val now = System.currentTimeMillis()
        val localLastUpdate = lastUpdate
        if (localLastUpdate == 0L) {
            Log.d(TAG, "isFresh: false (sin lastUpdate)")
            return false
        }
        val age = now - localLastUpdate
        val fresh = (age <= ttlMs && lruCache.size() > 0)
        Log.d(TAG, "isFresh: $fresh (age=${age}ms, ttl=${ttlMs}ms, size=${lruCache.size()})")
        return fresh
    }

    fun getAllAllowStale(): List<Product> {
        synchronized(this) {
            val size = lruCache.size()
            if (size == 0) {
                Log.d(TAG, "getAllAllowStale: cache vacío")
                return emptyList()
            }
            val snapshot = ArrayList(lruCache.snapshot().values)
            Log.d(TAG, "getAllAllowStale: devolviendo $size productos (ignorando TTL)")
            return snapshot
        }
    }
}