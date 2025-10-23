package com.example.unimarket.model.domain.entity

// Regla: { products:[productId], price:number }
data class Cart(
    val products: List<String> = emptyList(),
    val price: Double = 0.0
)