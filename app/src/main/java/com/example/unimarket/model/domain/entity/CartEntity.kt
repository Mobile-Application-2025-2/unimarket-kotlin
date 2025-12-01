package com.example.unimarket.model.domain.entity

data class Cart(
    val userId: String,
    val products: List<CartItem>,
    val finalPrice: Double
) {
    companion object {
        fun fromProducts(
            userId: String,
            products: List<CartItem>
        ): Cart {
            val total = products.sumOf { it.totalPrice }
            return Cart(
                userId = userId,
                products = products,
                finalPrice = total
            )
        }
    }
}

data class CartItem(
    val productId: String,
    val name: String,
    val unitPrice: Double,
    val quantity: Int,
    val imageUrl: String
) {
    val totalPrice: Double
        get() = unitPrice * quantity
}