package com.example.fashionapp.model

data class User(
    val name: String,
    val username: String,
    val image: String,
    val email:  String
)

val currentUser = User(
    name = "Diu Dao",
    username = "sunny",
    image = "https://s.gravatar.com/avatar/62a968f41c1feb83fd1cd142e7c043f3?s=200",
    email = "da"
)