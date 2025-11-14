package com.example.unimarket.model.domain.entity

import com.google.firebase.firestore.Exclude

data class Comment(
    val rating: Double = 0.0,
    val comment: String = "",
    val user: String = "",
    val product: String = "",
    val business: String = "",
    @get:Exclude @set:Exclude var id: String = ""
)
