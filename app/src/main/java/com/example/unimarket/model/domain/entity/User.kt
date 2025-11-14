package com.example.unimarket.model.domain.entity

import com.google.firebase.firestore.Exclude

data class User(
    val email: String = "",
    val name: String = "",
    val idType: String = "",
    val idNumber: String = "",
    val type: String = "",
    val onboardingCompleted: Boolean = false,
    val studentCode: String? = null,
    @get:Exclude @set:Exclude var id: String = ""
)