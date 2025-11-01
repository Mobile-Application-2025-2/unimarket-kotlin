package com.example.unimarket.model.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_local")
data class BusinessLocalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val logoUrl: String?,
    val categoryNames: String? // categorías como CSV simple p/evitar TypeConverters
)