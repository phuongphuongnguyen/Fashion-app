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

data class ShopProfile(
    val id: String = "",
    val ownerUserId: String = "",
    val name: String = "",
    val logoUrl: String = "",
    val followerCount: Int = 0,
    val productCount: Int = 0,
    val rating: Float = 0f,
    val location: String = "",
    val description: String = ""
)
