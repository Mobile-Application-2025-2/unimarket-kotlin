package com.example.unimarket.model.domain.entity

import com.google.firebase.firestore.Exclude

// Regla: { email, name, idType, idNumber, type } (type: "buyer" | "business")
data class User(
    val email: String = "",
    val name: String = "",
    val idType: String = "",
    val idNumber: String = "",
    val type: String = "",
    @get:Exclude @set:Exclude var id: String = "" // uid (no se escribe en Firestore)
)