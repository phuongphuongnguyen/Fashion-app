package com.example.fashionapp.model

data class User(
    val id: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val email: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0
) {
    val image: String
        get() = avatarUrl
}
