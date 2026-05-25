package com.example.fashionapp.model

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val originalPrice: Double = 0.0,
    val discountPercent: Int = 0,
    val imageUrl: String = "",        // ảnh đầu tiên đã resolve
    val imageUrls: List<String> = emptyList(), // tất cả ảnh đã resolve
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val soldCount: Int = 0,
    val isSale: Boolean = false,
    val shopId: String = "",
    val categoryId: String = "",
    val description: String = "",
    val variants: List<ProductVariant> = emptyList(),
    val stock: Int = 0,
    val freeShipping: Boolean = false,
    val tags: List<String> = emptyList(),
    val specifications: Map<String, String> = emptyMap(),  // Material, Origin...
)

data class ProductVariant(
    val id: String = "",
    val size: String = "",
    val color: String = "",
    val colorHex: String = "#000000",
    val stock: Int = 0,
    val additionalPrice: Double = 0.0,
)