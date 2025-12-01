package com.example.unimarket.model.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.unimarket.model.domain.entity.Product

/**
 * Tabla con los mejores productos globales (no por negocio).
 * Duplica la info básica para poder mostrar aunque no se pueda ir a Firebase.
 */
@Entity(tableName = "global_top_products")
data class GlobalTopProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: String,
    val price: Double,
    val rating: Double,
    val image: String, ) {
    fun toDomain(): Product =
        Product(
            id = id,
            name = name,
            description = description,
            category = category,
            price = price,
            rating = rating,
            image = image,
        )

    companion object {
        fun fromDomain(p: Product): GlobalTopProductEntity =
            GlobalTopProductEntity(
                id = p.id,
                name = p.name,
                description = p.description,
                category = p.category,
                price = p.price,
                rating = p.rating,
                image = p.image,
            )
    }
}