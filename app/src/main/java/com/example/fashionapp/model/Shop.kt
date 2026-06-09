package com.example.fashionapp.model

data class Shop(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val logoRef: String = "",
    val followerCount: Int = 0,
    val isOfficial: Boolean = false,
    val productCount: Int = 0,
    val rating: Double = 0.0,
    val responseRate: Int = 100,
    val responseTime: String = "Trong vài giờ",
    val reviewCount: Int = 0,
    val createdAt: com.google.firebase.Timestamp? = null
)
