package com.example.unimarket.model.api

import com.example.unimarket.model.entity.Category
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Query

interface CategoriesApi {

    @GET("rest/v1/categories")
    suspend fun list(
        @Query("select")
        select: String = "id,name,type,image,created_at,updated_at,selection_count",
        @Query("order")
        order: String? = null,
        @Header("Prefer")
        prefer: String = "count=estimated"
    ): Response<List<Category>>

    @PATCH("rest/v1/categories")
    suspend fun patchCategory(
        @Query("id") idEq: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer")
        prefer: String = "return=representation"
    ): Response<List<Category>>
}