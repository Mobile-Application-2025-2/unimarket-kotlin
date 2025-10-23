package com.example.unimarket.model.domain.validation

object Validators {
    fun requireNotBlank(value: String, field: String) {
        require(value.isNotBlank()) { "$field is required" }
    }

    fun requireEmail(email: String) {
        require(email.contains("@") && email.contains(".")) { "invalid email" }
    }

    fun parsePriceToDouble(raw: String): Double {
        val cleaned = raw.replace("[^0-9.,]".toRegex(), "")
            .replace(".", "")      // quita separadores de miles
            .replace(",", ".")     // usa punto decimal
        return cleaned.toDoubleOrNull() ?: error("Invalid price format: $raw")
    }
}
