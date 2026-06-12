package com.example.fashionapp.model

import com.google.firebase.Timestamp

data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvt: String = "", // Storage path hoặc URL

    val caption: String = "",
    val imageUrls: List<String> = emptyList(),
    val taggedProducts: List<ProductTag> = emptyList(),

    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val shareCount: Long = 0,
    val tags: List<String> = emptyList(),

    val createdAt: Timestamp? = null,
    val comments: List<Comment> = emptyList()
)

data class Comment(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val avatarRef: String = "",
    val avatarUrl: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
)

data class ProductTag(
    val productId: String = "",
    val thumbnailUrl: String = "",
    val label: String = "",
)
