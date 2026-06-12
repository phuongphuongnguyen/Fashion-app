package com.example.fashionapp.model

data class Shop(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val logoRef: String = "avatar/logo1.jpg",
    val followerCount: Int = 28900,
    val isOfficial: Boolean = true,
    val productCount: Int = 6,
    val rating: Double = 4.8,
    val responseRate: Int = 98,
    val responseTime: String = "Trong vài phút",
    val reviewCount: Int = 12430,
    val orderCount: Int = 36,
    val soldCount: Int = 5970,
    val revenue: Double = 14019000.0,
    val createdAt: com.google.firebase.Timestamp? = null,
    val updatedAt: com.google.firebase.Timestamp? = null
)
