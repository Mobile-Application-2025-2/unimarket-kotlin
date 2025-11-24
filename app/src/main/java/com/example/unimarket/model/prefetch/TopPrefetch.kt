package com.example.unimarket.model.prefetch

import android.content.Context
import android.util.Log
import com.example.unimarket.model.data.local.AppDatabase
import com.example.unimarket.model.data.local.entity.TopBusinessLocalEntity
import com.example.unimarket.model.data.local.entity.TopProductLocalEntity
import com.example.unimarket.model.domain.entity.Business
import com.example.unimarket.model.domain.entity.Category
import com.example.unimarket.model.domain.entity.Product
import com.example.unimarket.model.domain.service.BusinessService
import com.example.unimarket.model.domain.service.CategoryService
import com.example.unimarket.model.domain.service.ProductService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.Semaphore
import kotlin.math.ln

object TopPrefetch {

    private const val TAG = "TopPrefetch"
    private const val MAX_CONCURRENCY = 4

    suspend fun prefetchTopBusinessesAndProducts(appContext: Context) = withContext(Dispatchers.IO) {
        try {
            val db          = AppDatabase.getInstance(appContext)
            val topBizDao   = db.topBusinessDao()
            val topProdDao  = db.topProductDao()

            val categorySvc = CategoryService()
            val businessSvc = BusinessService()
            val productSvc  = ProductService()

            // 1) Traer todo remoto
            val categories: List<Category> = categorySvc.listAll().getOrElse { emptyList() }
            val businesses: List<Business> = businessSvc.getAllBusinesses().getOrElse { emptyList() }

            if (categories.isEmpty() || businesses.isEmpty()) {
                Log.d("TopPrefetch", "Sin datos para prefetch (cats=${categories.size}, biz=${businesses.size})")
                return@withContext
            }

            // 2) Para cada categoría: top 2 por rating desc, luego amountRatings desc
            for (cat in categories) {
                val cid   = cat.id.trim().lowercase()
                val cname = cat.name.trim().lowercase()

                val inCat = businesses.filter { b ->
                    b.categories.any { c ->
                        val i = c.id.trim().lowercase()
                        val n = c.name.trim().lowercase()
                        i == cid || n == cname
                    }
                }

                if (inCat.isEmpty()) {
                    topBizDao.replaceForCategory(cat.id, emptyList())
                    continue
                }

                val top2 = inCat
                    .sortedWith(
                        compareByDescending<Business> { it.rating }
                            .thenByDescending { it.amountRatings }
                            .thenBy { it.name.lowercase() }
                    )
                    .take(2)
                val now = System.currentTimeMillis()
                val entities = top2.mapIndexed { idx, b ->
                    TopBusinessLocalEntity(
                        id        = "${cat.id}_${b.id}",  // <- misma PK para REPLACE
                        categoryId   = cat.id,
                        categoryName = cat.name,
                        businessId   = b.id,
                        businessName = b.name,
                        logoUrl      = b.logo.ifBlank { null },
                        rating       = b.rating,
                        amountRatings = b.amountRatings,
                        rank         = idx + 1,
                        updatedAtEpochMillis = now,
                        productIdsCsv = b.products.joinToString(",") { it.trim() }.ifBlank { null }
                    )
                }

                // Reemplazo atómico para la categoría
                topBizDao.replaceForCategory(cat.id, entities)
            }

            // 3) Productos top (2 por subcategoría) SOLO de los negocios que quedaron en top
            val nowTop: List<TopBusinessLocalEntity> = db.topBusinessDao().getAllOnce()
            val topBizIds = nowTop.map { it.businessId }.toSet()
            if (topBizIds.isEmpty()) {
                Log.d("TopPrefetch", "No hay negocios top para prefetch de productos")
                return@withContext
            }

            // Preparamos un map negocio -> ids de productos
            val productIdsByBusiness: Map<String, List<String>> =
                businesses.associate { b -> b.id to b.products.map { it.trim() }.filter { it.isNotEmpty() } }

            val bizNameById: Map<String, String> =
                nowTop.associate { it.businessId to it.businessName }

            val semaphore = java.util.concurrent.Semaphore(4)

            coroutineScope {
                topBizIds.map { businessId ->
                    async(Dispatchers.IO) {
                        try {
                            semaphore.acquire()
                            val ids = productIdsByBusiness[businessId].orEmpty()
                            if (ids.isEmpty()) return@async

                            val products = productSvc.listByIds(ids).getOrElse { emptyList() }
                            if (products.isEmpty()) return@async

                            val businessName = bizNameById[businessId]

                            // 2 mejores por subcategoría (category del Product)
                            val perSub = products
                                .groupBy { it.category.trim().lowercase() }
                                .flatMap { (subcategory, list) ->
                                    list.sortedWith(
                                        compareByDescending<Product> { it.rating }
                                            .thenBy { it.name.lowercase() }
                                    ).take(2).mapIndexed { idx, p ->
                                        TopProductLocalEntity(
                                            id           = "${businessId}_${subcategory}_${p.id}",
                                            businessId   = businessId,
                                            businessName = businessName,
                                            subcategory  = subcategory,
                                            productId    = p.id,
                                            productName  = p.name,
                                            price        = p.price,
                                            rating       = p.rating,
                                            imageUrl     = p.image.ifBlank { null },
                                            rank         = idx + 1
                                        )
                                    }
                                }

                            if (perSub.isEmpty()) return@async

                            // Reemplazo por subcategoría
                            val grouped = perSub.groupBy { it.subcategory }
                            grouped.forEach { (sub, items) ->
                                topProdDao.clearFor(businessId = businessId, subcategory = sub)
                                topProdDao.upsertAll(items)
                            }
                        } finally {
                            semaphore.release()
                        }
                    }
                }.awaitAll()
            }

            Log.d("TopPrefetch", "Prefetch OK (biz top=${nowTop.size})")
        } catch (t: Throwable) {
            Log.d("TopPrefetch", "Prefetch falló: ${t.message}")
        }
    }

    // ---------- Helpers de ranking de negocios ----------
    private fun businessScore(b: Business): Double {
        val r = b.rating.coerceIn(0.0, 5.0)
        val n = b.amountRatings.toDouble().coerceAtLeast(0.0)
        return r * (1.0 + ln(1.0 + n))
    }

    private fun belongsToCategory(b: Business, cat: Category): Boolean {
        val cid = cat.id.trim().lowercase()
        val cname = cat.name.trim().lowercase()
        return b.categories.any { c ->
            val i = c.id.trim().lowercase()
            val n = c.name.trim().lowercase()
            i == cid || n == cname || n.contains(cname) || i.contains(cid)
        }
    }

    private fun buildTopBusinesses(
        categories: List<Category>,
        businesses: List<Business>
    ): List<TopBusinessLocalEntity> {
        val out = mutableListOf<TopBusinessLocalEntity>()
        for (cat in categories) {
            val inCat = businesses.filter { b -> belongsToCategory(b, cat) }
            if (inCat.isEmpty()) continue

            val bestTwo = inCat
                .sortedWith(compareByDescending<Business> { businessScore(it) }.thenBy { it.name.lowercase() })
                .take(2)
            val now = System.currentTimeMillis()
            bestTwo.forEachIndexed { idx, b ->
                out += TopBusinessLocalEntity(
                    id = "${cat.id}_${b.id}",
                    categoryId = cat.id,
                    categoryName = cat.name,
                    businessId = b.id,
                    businessName = b.name,
                    logoUrl = b.logo.ifBlank { null },
                    rating = b.rating,
                    amountRatings = b.amountRatings,
                    rank = idx + 1,
                    updatedAtEpochMillis = now,
                    productIdsCsv = b.products.joinToString(",") { it.trim() }.ifBlank { null }
                )
            }
        }
        return out
    }
}