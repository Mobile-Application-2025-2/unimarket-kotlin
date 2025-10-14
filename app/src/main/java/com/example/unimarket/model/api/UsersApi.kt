package com.example.unimarket.model.api

import com.example.unimarket.model.entity.UserRow
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface UsersApi {
    @GET("rest/v1/users")
    suspend fun userByEmail(
        @Query("email") emailEq: String,
        @Query("select") select: String = "type,email",
        @Header("Range") range: String = "0-0"
    ): Response<List<UserRow>>
}
