package com.example.fashionapp.model

data class Category(
    val id: String,
    val name: String,
    val previewImages: List<String> = emptyList()
)