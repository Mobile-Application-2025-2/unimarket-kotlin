package com.example.unimarket.model.api

import com.example.unimarket.model.entity.Category
import retrofit2.Response
import retrofit2.http.*

/**
 * Solo REST (PostgREST). Sin SQL.
 * Incluimos variantes por 'id' y por 'uuid' porque algunas tablas usan 'uuid' como PK.
 */
interface CategoriesApi {

    @GET("rest/v1/categories")
    suspend fun list(
        @Query("select")
        select: String = "id,name,type,image,createdAt:created_at,updatedAt:updated_at,selectionCount:selection_count",
        @Query("order") order: String? = "name.asc",
        @Header("Prefer") prefer: String = "count=estimated"
    ): Response<List<Category>>

    /* ---------- PK = id ---------- */
    @GET("rest/v1/categories")
    suspend fun getOneById(
        @Query("id") idEq: String,   // "eq.<uuid>"
        @Query("select") select: String = "id,selectionCount:selection_count"
    ): Response<List<Category>>

    @PATCH("rest/v1/categories")
    suspend fun patchByIdConditional(
        @Query("id") idEq: String,
        @Query("selection_count") expectedEq: String,  // "eq.<valor_actual>"
        @Header("Prefer") prefer: String = "return=representation",
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<Category>>

    @PATCH("rest/v1/categories")
    suspend fun patchById(
        @Query("id") idEq: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<Category>>

    /* ---------- PK = uuid ---------- */
    @GET("rest/v1/categories")
    suspend fun getOneByUuid(
        @Query("uuid") idEq: String,   // "eq.<uuid>"
        @Query("select") select: String = "id:uuid,selectionCount:selection_count"
    ): Response<List<Category>>

    @PATCH("rest/v1/categories")
    suspend fun patchByUuidConditional(
        @Query("uuid") idEq: String,
        @Query("selection_count") expectedEq: String,  // "eq.<valor_actual>"
        @Header("Prefer") prefer: String = "return=representation",
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<Category>>

    @PATCH("rest/v1/categories")
    suspend fun patchByUuid(
        @Query("uuid") idEq: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<Category>>
}