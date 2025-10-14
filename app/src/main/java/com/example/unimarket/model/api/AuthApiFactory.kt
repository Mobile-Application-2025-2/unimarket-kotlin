package com.example.unimarket.model.api

import com.example.unimarket.model.session.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object AuthApiFactory {

    private fun baseMoshi(): Moshi =
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    fun createSignUpAuthApi(baseUrl: String, anonKey: String, enableLogging: Boolean = true): SignUpAuthApi {
        val moshi = baseMoshi()
        val apikey = Interceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(req)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(apikey)
            .apply {
                if (enableLogging) addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
            .create(SignUpAuthApi::class.java)
    }

    fun createLoginAuthApi(baseUrl: String, anonKey: String, enableLogging: Boolean = true): LoginAuthApi {
        val moshi = baseMoshi()
        val apikey = Interceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("apikey", anonKey)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(req)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(apikey)
            .apply {
                if (enableLogging) addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
            .create(LoginAuthApi::class.java)
    }

    fun createUsersApi(baseUrl: String, anonKey: String, enableLogging: Boolean = true): UsersApi {
        val moshi = baseMoshi()
        val headers = Interceptor { chain ->
            val builder = chain.request().newBuilder()
                .addHeader("apikey", anonKey)
                .addHeader("Accept", "application/json")
            SessionManager.bearerOrNull()?.let { builder.addHeader("Authorization", it) }
            chain.proceed(builder.build())
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(headers)
            .apply {
                if (enableLogging) addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
            .create(UsersApi::class.java)
    }
}