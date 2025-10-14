package com.example.unimarket.model.entity

data class SignUpBody(
    val email: String,
    val password: String,
    val data: Map<String, String>
)