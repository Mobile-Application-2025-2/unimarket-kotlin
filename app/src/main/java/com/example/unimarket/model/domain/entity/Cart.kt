package com.example.unimarket.model.domain.entity
data class Cart(
    val products: Map<String, Int> = emptyMap(),
    val price: Double = 0.0,
    val business: String=""
)