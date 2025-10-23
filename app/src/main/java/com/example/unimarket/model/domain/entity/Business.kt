package com.example.unimarket.model.domain.entity

import com.google.firebase.firestore.Exclude

// Regla: { name, address(Address), rating:number, products:[productId], logo }
data class Business(
    val name: String = "",
    val address: Address = Address(),
    val rating: Double = 0.0,
    val products: List<String> = emptyList(),
    val logo: String = "",
    @get:Exclude @set:Exclude var id: String = "" // uid
)