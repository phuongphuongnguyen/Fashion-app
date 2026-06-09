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

data class OrderItem(
    val cartItemId: String = "",
    val product: Product = Product(),
    val color: String = "",
    val size: String = "",
    val quantity: Int = 1,
    val lineTotal: Double = product.price * quantity
)

data class ReviewOrder(
    val id: String,
    val product: Product,
    val status: String,
    val orderDate: String,
    val placedAtMillis: Long = 0L,
    val items: List<OrderItem> = emptyList(),
    val userId: String = "",
    val paymentMethod: String = "",
    val paymentStatus: String = "",
    val shippingMethod: String = "",
    val shippingFee: Double = 0.0,
    val shippingAddress: String = "",
    val totalPrice: Double = 0.0,
    val momoOrderId: String = ""
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
