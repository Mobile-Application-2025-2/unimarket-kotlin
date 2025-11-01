package com.example.unimarket.model.domain.entity

import com.google.firebase.firestore.Exclude

// Regla:
// { name, price(string), description, category(categoryId), business(businessUid),
//   rating(number), comments:[commentId], image }
data class Product(
    val name: String = "",
    val price: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val categoryLabel: String = "",
    val business: String = "",
    val rating: Double = 0.0,
    val comments: List<String> = emptyList(),
    val image: String = "",
    @get:Exclude @set:Exclude var id: String = ""
)