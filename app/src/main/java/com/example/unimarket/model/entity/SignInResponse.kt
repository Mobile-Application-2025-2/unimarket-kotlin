package com.example.unimarket.model.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SignInRespUser(
    val email: String?,
    val user_metadata: Map<String, Any>?
)

@JsonClass(generateAdapter = true)
data class SignInResponse(
    val access_token: String?,
    val token_type: String?,
    val user: SignInRespUser?
)