package com.example.unimarket.model.data.cache

import android.util.LruCache
import com.example.unimarket.model.domain.entity.Business

/**
 * Cache en memoria para negocios usando LRU.
 *
 * - Clave: id del negocio (String).
 * - Valor: Business completo.
 * - maxSize: máximo número de negocios en memoria.
 * - Política: Least Recently Used (se expulsan los menos usados).
 */
object BusinessMemoryCache : LruCache<String, Business>(100) {

    private var lastUpdateMillis: Long = 0L

    /**
     * Actualiza la cache con una nueva lista completa de negocios.
     * Limpia lo anterior y guarda solo los actuales.
     */
    fun updateAll(list: List<Business>) {
        evictAll()
        list.forEach { business ->
            if (business.id.isNotBlank()) {
                put(business.id, business)
            }
        }
        lastUpdateMillis = System.currentTimeMillis()
        android.util.Log.d("BusinessMemoryCache", "updateAll: guardados ${list.size} negocios")
    }

    /**
     * Devuelve la lista de negocios en cache SOLO si:
     * - No está vacía.
     * - No ha expirado según el TTL.
     *
     * Si no cumple, devuelve null.
     */
    fun snapshotIfFresh(ttlMillis: Long): List<Business>? {
        if (size() == 0) {
            android.util.Log.d("BusinessMemoryCache", "snapshotIfFresh: MISS (vacío)")
            return null
        }
        if (lastUpdateMillis == 0L) {
            android.util.Log.d("BusinessMemoryCache", "snapshotIfFresh: MISS (sin timestamp)")
            return null
        }
        val age = System.currentTimeMillis() - lastUpdateMillis
        if (age > ttlMillis) {
            android.util.Log.d("BusinessMemoryCache", "snapshotIfFresh: MISS (expirado, age=$age)")
            return null
        }
        android.util.Log.d("BusinessMemoryCache", "snapshotIfFresh: HIT (size=${size()})")
        return snapshot().values.toList()
    }

    private const val MAX_SIZE = 100

    override fun sizeOf(key: String, value: Business): Int {
        // Cada Business cuenta como 1 unidad.
        return 1
    }
}