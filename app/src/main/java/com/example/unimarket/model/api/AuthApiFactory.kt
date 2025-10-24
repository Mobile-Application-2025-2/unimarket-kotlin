package com.example.unimarket.model.api
/*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

import okhttp3.Response

object AuthApiFactory {

    fun create(baseUrl: String, anonKey: String, enableLogging: Boolean = true): SignUpAuthApi {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

        val apikeyInterceptor = Interceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("apikey", anonKey)
                .build()
            chain.proceed(req)
        }

        val clientBuilder = OkHttpClient.Builder().addInterceptor(apikeyInterceptor)
        if (enableLogging) {
            clientBuilder.addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(clientBuilder.build())
            .build()
            .create(SignUpAuthApi::class.java)
    }

    fun login(baseUrl: String, anonKey: String, enableLogging: Boolean = true): LoginAuthApi {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

        val apikeyInterceptor = Interceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("apikey", anonKey)
                .addHeader("Accept", "application/json")
                .build()
            chain.proceed(req)
        }

        val clientBuilder = OkHttpClient.Builder().addInterceptor(apikeyInterceptor)
        if (enableLogging) {
            clientBuilder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(clientBuilder.build())
            .build()
            .create(LoginAuthApi::class.java)
    }

    fun getUsers(baseUrl: String, anonKey: String, enableLogging: Boolean = true): UsersApi {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

        val apikeyInterceptor = Interceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("apikey", anonKey)
                .addHeader("Accept", "application/json")
                .build()
            chain.proceed(req)
        }

        val clientBuilder = OkHttpClient.Builder().addInterceptor(apikeyInterceptor)
        if (enableLogging) {
            clientBuilder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(clientBuilder.build())
            .build()
            .create(UsersApi::class.java)
    }

    private fun baseRetrofit(
        baseUrl: String,
        anonKey: String,
        userJwt: String? = null,
        enableLogging: Boolean = false
    ): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = if (enableLogging) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(SupabaseHeadersInterceptor(anonKey, userJwt))
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private class SupabaseHeadersInterceptor(
        private val anonKey: String,
        private val userJwt: String?
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val token = userJwt?.takeIf { it.isNotBlank() } ?: anonKey
            val req = chain.request().newBuilder()
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build()
            return chain.proceed(req)
        }
    }

    fun createCategoriesApi(
        baseUrl: String,
        anonKey: String,
        userJwt: String? = null,
        enableLogging: Boolean = false
    ): CategoriesApi =
        baseRetrofit(baseUrl, anonKey, userJwt, enableLogging).create(CategoriesApi::class.java)
    fun createDeliveriesApi(
        baseUrl: String,
        anonKey: String,
        userJwt: String? = null,
        enableLogging: Boolean = false
    ): DeliveriesApi =
        baseRetrofit(baseUrl, anonKey, userJwt, enableLogging).create(DeliveriesApi::class.java)
}*/