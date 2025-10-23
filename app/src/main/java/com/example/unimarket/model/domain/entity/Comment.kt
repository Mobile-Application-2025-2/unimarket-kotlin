package com.example.unimarket.model.domain.entity

import com.google.firebase.firestore.Exclude

// Regla: { rating:number, comment:string, user:uid, product:productId, business:businessUid }
data class Comment(
    val rating: Double = 0.0,
    val comment: String = "",
    val user: String = "",     // uid buyer
    val product: String = "",  // productId
    val business: String = "", // businessUid
    @get:Exclude @set:Exclude var id: String = "" // docId
)
