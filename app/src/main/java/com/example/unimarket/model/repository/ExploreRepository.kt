package com.example.unimarket.model.repository

import com.example.unimarket.model.api.CategoriesApi
import com.example.unimarket.model.entity.Category
import retrofit2.HttpException

class ExploreRepository(
    private val categoriesApi: CategoriesApi
) {

    suspend fun fetchAllCategories(order: String = "name.asc"): List<Category> {
        val resp = categoriesApi.list(order = order)
        if (!resp.isSuccessful) throw HttpException(resp)
        return resp.body().orEmpty()
    }

    suspend fun getCategories(order: String = "name.asc"): Result<List<Category>> = runCatching {
        fetchAllCategories(order)
    }

    suspend fun incrementCategorySelection(
        categoryId: String,
        newCount: Long
    ): Result<Category> = runCatching {
        val resp = categoriesApi.patchCategory(
            idEq = "eq.$categoryId",
            body = mapOf("selection_count" to newCount)
        )
        if (!resp.isSuccessful) throw HttpException(resp)
        resp.body()?.firstOrNull()
            ?: error("Respuesta vacía al actualizar category=$categoryId")
    }
}