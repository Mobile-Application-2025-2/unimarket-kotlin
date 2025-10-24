package com.example.unimarket.model.domain.validation

object Validators {
    fun requireNotBlank(value: String, field: String) {
        require(value.isNotBlank()) { "$field is required" }
    }

    fun requireEmail(email: String) {
        require(email.contains("@") && email.contains(".")) { "invalid email" }
    }

    fun parsePriceToDouble(raw: Any): Double = when (raw) {
        is Number -> raw.toDouble()
        is String -> raw
            .replace("[^\\d.,]".toRegex(), "")
            .replace(".", "")
            .replace(",", ".")
            .toDoubleOrNull() ?: error("Invalid price: $raw")
        else -> error("Unsupported price type")
    }
}
