package com.example.unimarket.model.domain.entity

// Regla Address (map requerido en buyers[] y businesses.address)
data class Address(
    val direccion: String = "",
    val edificio: String = "",
    val piso: String = "",
    val salon: String = "",
    val local: String = ""
)