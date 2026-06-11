package com.example.fashionapp.data

import com.google.firebase.Timestamp

data class NotificationModel(
    val id: String = "",
    val userId: String = "",
    val message: String = "",
    val type: String = "PAYMENT", // "PAYMENT" or "SHIPPING"
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null
)
