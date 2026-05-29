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
    val orderDate: String,
    val placedAtMillis: Long = 0L
)

data class ProductReview(
    val id: String = "",
    val orderId: String = "",
    val productId: String = "",
    val userId: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val editCount: Int = 0,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
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
