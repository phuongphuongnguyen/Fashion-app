package com.example.fashionapp.model

data class Shop(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val logoRef: String = "",
    val followerCount: Int = 28900,
    val isOfficial: Boolean = true,
    val productCount: Int = 0,
    val rating: Double = 0.0,
    val responseRate: Int = 0,
    val responseTime: String = "Trong vài phút",
    val reviewCount: Int = 0,
    val orderCount: Int = 0,
    val soldCount: Int = 0,
    val revenue: Double = 0.0,
    val createdAt: com.google.firebase.Timestamp? = null,
    val updatedAt: com.google.firebase.Timestamp? = null
)
