package com.example.unimarket.model.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

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

    /** REST users table: UsersApi */
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
}
