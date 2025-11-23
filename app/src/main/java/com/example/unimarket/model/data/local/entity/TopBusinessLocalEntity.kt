package com.example.unimarket.model.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "top_business_local",
    indices = [
        Index(value = ["categoryId", "rank"], unique = true),
        Index(value = ["categoryId"]),
        Index(value = ["businessId"], unique = false)
    ]
)
data class TopBusinessLocalEntity(
    @PrimaryKey val id: String,       
    val categoryId: String,
    val categoryName: String,

    val businessId: String,
    val businessName: String,
    val logoUrl: String?,

    val rating: Double,
    val amountRatings: Long,

    val rank: Int,                    
    val updatedAtEpochMillis: Long,

    val productIdsCsv: String? = null
)