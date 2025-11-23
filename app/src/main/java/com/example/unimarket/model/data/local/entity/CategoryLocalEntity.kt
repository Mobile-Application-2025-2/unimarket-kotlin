package com.example.unimarket.model.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories_local")
data class CategoryLocalEntity(
    @PrimaryKey val id: String,   // Firebase docId
    val name: String,
    val count: Long
)
