package com.example.unimarket.model.domain.entity

import com.google.firebase.firestore.Exclude

// Regla: { address:[Address], cart:{ products, price } }
data class Buyer(
    val address: List<Address> = emptyList(),
    val cart: Cart = Cart(),
    @get:Exclude @set:Exclude var id: String = "" // uid
)