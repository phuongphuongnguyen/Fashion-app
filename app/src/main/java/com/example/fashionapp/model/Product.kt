package com.example.fashionapp.model

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val imageUrl: String,
    val rating: Float = 0f,
    val soldCount: Int = 0
)
