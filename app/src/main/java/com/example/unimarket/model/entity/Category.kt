package com.example.unimarket.model.entity

import com.squareup.moshi.Json

data class Category(
    val id: String,
    val name: String,
    val type: String,
    val image: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    @Json(name = "selection_count") val selectionCount: Long = 0
)