package com.example.unimarket.model.entity

import com.squareup.moshi.Json

data class Delivery(
    val id: String,
    @Json(name = "address_delivery") val addressDelivery: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)