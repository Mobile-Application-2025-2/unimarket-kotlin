package com.example.unimarket.model.entity

import com.squareup.moshi.Json

data class Category(
    val name: String,
    val type: String,
    @Json(name = "selection_count")
    val selectionCount: Int
)