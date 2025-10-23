package com.example.unimarket.model.domain.entity

import com.google.firebase.firestore.Exclude

// Regla: { name, count:number }
data class Category(
    val name: String = "",
    val count: Long = 0L,
    @get:Exclude @set:Exclude var id: String = "" // docId
)