package com.example.unimarket.workers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 👇 Ajusta estos imports a tus rutas reales:
import com.example.unimarket.model.data.local.AppDatabase
import com.example.unimarket.model.data.local.dao.BusinessLocalDao
import com.example.unimarket.model.data.local.entity.BusinessLocalEntity
import com.example.unimarket.model.domain.service.BusinessService

/**
 * Prefetch de negocios:
 * - Concurrencia: corre en Dispatchers.IO (no bloquea la UI)
 * - Almacenamiento local: guarda en Room (tabla business_local)
 *
 * Requisitos:
 *  - AppDatabase con `abstract fun businessLocalDao(): BusinessLocalDao`
 *  - BusinessLocalDao con `clear()` y `upsertAll(...)`
 *  - BusinessService con `suspend fun getAllBusinesses(): Result<List<Business>>`
 */
class PrefetchBusinessesWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Start on thread: ${Thread.currentThread().name}")
        try {
            // 1) Construir la DB local aquí mismo (sin getInstance)
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "unimarket.db"
            ).build()
            val dao: BusinessLocalDao = db.businessLocalDao()

            // 2) Llamar el servicio remoto existente
            val service = BusinessService()
            val businesses = service.getAllBusinesses().getOrElse { e ->
                Log.e(TAG, "Remote fetch failed: ${e.message}", e)
                return@withContext Result.retry()
            }

            // 3) Mapear a tu Entity de Room y persistir
            val entities = businesses.map { b ->
                val logo = runCatching { b.logo?.trim().takeIf { !it.isNullOrEmpty() } }.getOrNull()
                val categoriesCsv = runCatching {
                    // si tu modelo tiene `categories: List<Category>` con `name`
                    b.categories.joinToString(",") { c -> c.name }
                }.getOrNull()

                BusinessLocalEntity(
                    id = b.id,
                    name = b.name,
                    logoUrl = logo,
                    categoryNames = categoriesCsv
                )
            }

            dao.clear()
            dao.upsertAll(entities)
            Log.d(TAG, "Saved to Room: ${entities.size}")

            Result.success()
        } catch (t: Throwable) {
            Log.e(TAG, "Unexpected error: ${t.message}", t)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "PrefetchWorker"
        private const val UNIQUE = "prefetch-businesses"

        /**
         * Encola el prefetch con red requerida.
         * Usa KEEP para no duplicar; usa REPLACE si quieres forzar nueva corrida.
         */
        fun enqueue(context: Context, replace: Boolean = false) {
            val req = OneTimeWorkRequestBuilder<PrefetchBusinessesWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE,
                if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                req
            )
        }
    }
}