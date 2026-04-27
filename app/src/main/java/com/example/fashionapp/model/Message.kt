package com.example.fashionapp.model

data class Message(
    val id: String,
    val senderId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
