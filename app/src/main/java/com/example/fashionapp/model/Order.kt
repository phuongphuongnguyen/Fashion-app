package com.example.fashionapp.model

data class Order(
    val id: String,
    val product: Product,
    val quantity: Int,
    val totalPrice: Double,
    val status: String = "pending"
)
