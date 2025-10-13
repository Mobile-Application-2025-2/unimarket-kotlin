package com.example.unimarket.data

import com.example.unimarket.SupaConst
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response as OkHttpResponse
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
object CategoriesRepository {

    data class Category(
        val id: String,
        val name: String,
        val type: String,
        val image: String?,
        @Json(name = "selection_count") val selectionCount: Int?
    )

    data class CategoryCreate(
        val name: String,
        val type: String,
        val image: String? = null
    )

    data class CategoryPatch(
        val name: String? = null,
        val type: String? = null,
        val image: String? = null,
        @Json(name = "selection_count") val selectionCount: Int? = null
    )

    private class SupabaseHeaders : Interceptor {
        override fun intercept(chain: Interceptor.Chain): OkHttpResponse {
            val req = chain.request().newBuilder()
                .addHeader("apikey", SupaConst.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${SupaConst.SUPABASE_ANON_KEY}")
                .addHeader("Accept", "application/json")
                .build()
            return chain.proceed(req)
        }
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(SupabaseHeaders())
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(ensureSlash(SupaConst.SUPABASE_URL))
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(client)
        .build()

    private fun ensureSlash(url: String) = if (url.endsWith("/")) url else "$url/"

    private interface Api {
        @GET("rest/v1/categories")
        suspend fun getAll(
            @Query("select") select: String = "id,name,type,image,selection_count",
            @Query("order") order: String = "selection_count.desc"
        ): List<Category>

        @Headers("Prefer: return=representation")
        @POST("rest/v1/categories")
        suspend fun create(@Body body: CategoryCreate): List<Category>
        @Headers("Prefer: return=representation")
        @PATCH("rest/v1/categories")
        suspend fun updateById(
            @Query("id") idFilter: String,
            @Body body: CategoryPatch
        ): List<Category>

        // DELETE por id
        @DELETE("rest/v1/categories")
        suspend fun deleteById(
            @Query("id") idFilter: String
        ): okhttp3.ResponseBody
    }
    private val api: Api = retrofit.create(Api::class.java)
    suspend fun getAllSortedBySelection(): List<Category> = api.getAll()
    suspend fun createCategory(name: String, type: String, image: String?): Category? {
        val list = api.create(CategoryCreate(name = name, type = type, image = image))
        return list.firstOrNull()
    }
    suspend fun updateCategory(
        id: String,
        name: String? = null,
        type: String? = null,
        image: String? = null,
        selectionCount: Int? = null
    ): Category? {
        val list = api.updateById(
            idFilter = "eq.$id",
            body = CategoryPatch(name = name, type = type, image = image, selectionCount = selectionCount)
        )
        return list.firstOrNull()
    }

    suspend fun deleteCategory(id: String) {
        api.deleteById(idFilter = "eq.$id")
    }
    suspend fun bumpSelectionCount(id: String, current: Int?): Category? {
        val next = (current ?: 0) + 1
        return updateCategory(id = id, selectionCount = next)
    }
}
