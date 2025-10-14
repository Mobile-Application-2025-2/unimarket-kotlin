package com.example.unimarket.model.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SignInBody(
    val email: String, 
    val password: String
)