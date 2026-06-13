package com.example.fashionapp.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class NotificationModel(
    val id: String = "",
    val userId: String = "",
    val message: String = "",
    val type: String = "PAYMENT", // "PAYMENT" or "SHIPPING"
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,
    val createdAt: Timestamp? = null
)
