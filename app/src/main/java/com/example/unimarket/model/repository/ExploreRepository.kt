package com.example.unimarket.model.repository

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
}