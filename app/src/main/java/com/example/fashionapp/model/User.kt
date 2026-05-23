package com.example.fashionapp.model

data class User(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val email: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0
) {
    val username: String
        get() = name

    val image: String
        get() = avatarUrl
}

val currentUser = User(
    id = "current-user",
    name = "Your Story",
    avatarUrl = "https://randomuser.me/api/portraits/women/1.jpg",
    email = "you@example.com"
)
