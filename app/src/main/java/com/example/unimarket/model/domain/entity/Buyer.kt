package com.example.unimarket.model.domain.entity

import com.google.firebase.firestore.Exclude

data class Buyer(
    val address: List<Address> = emptyList(),
    @get:Exclude @set:Exclude var id: String = "" // uid
)