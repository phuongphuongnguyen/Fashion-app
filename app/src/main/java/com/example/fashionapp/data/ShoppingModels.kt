package com.example.fashionapp.data

import com.example.fashionapp.model.Product

data class CartItem(
    val id: String,
    val product: Product,
    val color: String,
    val size: String,
    val quantity: Int
) {
    val totalPrice: Double = product.price * quantity
}

data class ReviewOrder(
    val id: String,
    val product: Product,
    val status: String,
    val orderDate: String
)
