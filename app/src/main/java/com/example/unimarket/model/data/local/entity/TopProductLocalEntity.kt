package com.example.unimarket.model.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "top_product_local",
    indices = [
        Index(value = ["businessId", "subcategory", "productId"], unique = true),
        Index(value = ["businessId", "subcategory", "rank"], unique = true),
        Index(value = ["businessId"]),
        Index(value = ["subcategory"])
    ]
)
data class TopProductLocalEntity(
    @PrimaryKey val id: String,                
    val businessId: String,
    val businessName: String?,                  
    val subcategory: String,                   
    val productId: String,
    val productName: String,
    val price: Double,
    val rating: Double,
    val imageUrl: String?,                      
    val rank: Int,
)