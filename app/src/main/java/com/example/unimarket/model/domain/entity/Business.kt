package com.example.unimarket.model.domain.entity

import com.google.firebase.firestore.Exclude
import com.example.unimarket.model.domain.entity.Category

data class Business(
    val name: String = "",
    val address: Address = Address(),
    val rating: Double = 0.0,
    val amountRatings: Long = 0L,
    val products: List<String> = emptyList(),
    val logo: String = "",
    val categories: List<Category> = emptyList(),
    @get:Exclude @set:Exclude var id: String = "" // uid
)