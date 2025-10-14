package com.example.unimarket.model.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserRow(
    val email: String?, 
    val type: String?
)
