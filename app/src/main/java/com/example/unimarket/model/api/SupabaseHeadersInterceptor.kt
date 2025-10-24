package com.example.unimarket.model.api
/*
import okhttp3.Interceptor
import okhttp3.Response

class SupabaseHeadersInterceptor(
    private val anonKey: String,
    private val authBearer: String? = null
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val bearer = authBearer ?: anonKey

        val req = chain.request()
            .newBuilder()
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $bearer")
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .build()

        return chain.proceed(req)
    }
}*/