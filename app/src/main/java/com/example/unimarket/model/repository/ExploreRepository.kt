package com.example.unimarket.model.repository

import android.util.Log
import com.example.unimarket.model.api.CategoriesApi
import com.example.unimarket.model.entity.Category
import retrofit2.HttpException
import java.io.IOException

class ExploreRepository(
    private val categoriesApi: CategoriesApi
) {

    suspend fun getCategories(order: String = "name.asc"): Result<List<Category>> = runCatching {
        val resp = categoriesApi.list(order = order)
        if (!resp.isSuccessful) throw HttpException(resp)
        resp.body().orEmpty()
    }.recoverCatching { e ->
        when (e) {
            is IOException -> error("Sin conexión. Verifica tu red.")
            else -> throw e
        }
    }

    /**
     * Incrementa selection_count SOLO con REST:
     * 1) PATCH condicional por 'id'
     * 2) PATCH condicional por 'uuid'
     * 3) Fallback: leer valor actual y hacer PATCH directo (+1) por 'id' y luego por 'uuid'
     *
     * Firma compatible con tu UI: (categoryId, expectedOld: Int?)
     */
    suspend fun incrementSelectionCount(
        categoryId: String,
        expectedOld: Int?,
        retries: Int = 2
    ): Result<Unit> = runCatching {
        require(categoryId.isNotBlank()) { "categoryId en blanco" }

        var attempt = 0
        var expected: Long? = expectedOld?.toLong()

        // Intentos con bloqueo optimista (condicional)
        while (attempt < retries) {
            attempt++

            if (tryPatchConditionalById(categoryId, expected)) return@runCatching Unit
            if (tryPatchConditionalByUuid(categoryId, expected)) return@runCatching Unit

            // Releer en siguiente intento (posible carrera o expectedOld errado)
            expected = null
        }

        // Fallback: leer y actualizar directo (+1)
        val current = readCurrentCount(categoryId) ?: error("Categoría no encontrada")
        val newCount = current + 1

        categoriesApi.patchById(
            idEq = "eq.$categoryId",
            body = mapOf("selection_count" to newCount)
        ).also { r ->
            Log.d("ExploreRepo", "PATCH by id (fallback) code=${r.code()} rows=${r.body()?.size ?: 0} err=${r.errorBody()?.string()}")
            if (r.isSuccessful && r.body().orEmpty().isNotEmpty()) return@runCatching Unit
        }

        categoriesApi.patchByUuid(
            idEq = "eq.$categoryId",
            body = mapOf("selection_count" to newCount)
        ).also { r ->
            Log.d("ExploreRepo", "PATCH by uuid (fallback) code=${r.code()} rows=${r.body()?.size ?: 0} err=${r.errorBody()?.string()}")
            if (r.isSuccessful && r.body().orEmpty().isNotEmpty()) return@runCatching Unit
        }

        error("No se pudo actualizar selection_count (0 filas afectadas)")
    }.recoverCatching { e ->
        when (e) {
            is IOException -> error("Sin conexión. Verifica tu red.")
            else -> throw e
        }
    }

    // ---------- Helpers internos ----------

    private suspend fun tryPatchConditionalById(
        categoryId: String,
        expectedOld: Long?
    ): Boolean {
        val expected = expectedOld ?: run {
            val r = categoriesApi.getOneById(idEq = "eq.$categoryId")
            Log.d("ExploreRepo", "GET one by id code=${r.code()} body=${r.body()}")
            if (!r.isSuccessful) throw HttpException(r)
            r.body()?.firstOrNull()?.selectionCount
        } ?: return false

        val newCount = expected + 1
        val patch = categoriesApi.patchByIdConditional(
            idEq = "eq.$categoryId",
            expectedEq = "eq.$expected",
            body = mapOf("selection_count" to newCount)
        )
        Log.d("ExploreRepo", "PATCH by id cond code=${patch.code()} rows=${patch.body()?.size ?: 0} err=${patch.errorBody()?.string()}")
        return patch.isSuccessful && patch.body().orEmpty().isNotEmpty()
    }

    private suspend fun tryPatchConditionalByUuid(
        categoryId: String,
        expectedOld: Long?
    ): Boolean {
        val expected = expectedOld ?: run {
            val r = categoriesApi.getOneByUuid(idEq = "eq.$categoryId")
            Log.d("ExploreRepo", "GET one by uuid code=${r.code()} body=${r.body()}")
            if (!r.isSuccessful) throw HttpException(r)
            r.body()?.firstOrNull()?.selectionCount
        } ?: return false

        val newCount = expected + 1
        val patch = categoriesApi.patchByUuidConditional(
            idEq = "eq.$categoryId",
            expectedEq = "eq.$expected",
            body = mapOf("selection_count" to newCount)
        )
        Log.d("ExploreRepo", "PATCH by uuid cond code=${patch.code()} rows=${patch.body()?.size ?: 0} err=${patch.errorBody()?.string()}")
        return patch.isSuccessful && patch.body().orEmpty().isNotEmpty()
    }

    private suspend fun readCurrentCount(categoryId: String): Long? {
        runCatching {
            val r = categoriesApi.getOneById(idEq = "eq.$categoryId")
            Log.d("ExploreRepo", "GET one by id (fallback read) code=${r.code()} body=${r.body()}")
            if (!r.isSuccessful) throw HttpException(r)
            return r.body()?.firstOrNull()?.selectionCount
        }

        runCatching {
            val r = categoriesApi.getOneByUuid(idEq = "eq.$categoryId")
            Log.d("ExploreRepo", "GET one by uuid (fallback read) code=${r.code()} body=${r.body()}")
            if (!r.isSuccessful) throw HttpException(r)
            return r.body()?.firstOrNull()?.selectionCount
        }

        return null
    }
}