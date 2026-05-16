package com.example.fashionapp.data

import com.example.fashionapp.model.Comment
import com.example.fashionapp.model.Order
import com.example.fashionapp.model.Post
import com.example.fashionapp.model.Product
import com.example.fashionapp.model.ProductTag
import com.google.firebase.Timestamp
import java.util.Date

data class CartItem(
    val id: String,
    val product: Product,
    val color: String,
    val size: String,
    val quantity: Int
) {
    val totalPrice: Double = product.price * quantity
}

data class ReviewOrder(
    val id: String,
    val product: Product,
    val status: String,
    val orderDate: String
)

object MockData {
    private val avatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300"

    val products = listOf(
        Product("product-1", "Linen summer white set", 32.0, "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=600", 4.8f, 1200),
        Product("product-2", "Loose square dress", 27.0, "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=600", 4.6f, 834),
        Product("product-3", "Classic white shirt", 32.0, "https://images.unsplash.com/photo-1598033129183-c4f50c736f10?w=600", 4.7f, 962),
        Product("product-4", "Pink casual trousers", 32.0, "https://images.unsplash.com/photo-1506629905607-d9c297d61d43?w=600", 4.5f, 541),
        Product("product-5", "Black denim jacket", 27.0, "https://images.unsplash.com/photo-1543076447-215ad9ba6923?w=600", 4.9f, 431)
    )

    val feedPosts = listOf(
        Post(
            id = "post-1",
            authorId = "user-romina",
            authorName = "Romina",
            authorAvt = avatar,
            caption = "Clean layers for a city walk.",
            imageUrls = listOf("https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=900"),
            taggedProducts = listOf(ProductTag("product-1", products[0].imageUrl, products[0].name)),
            likeCount = 2400,
            commentCount = 162,
            shareCount = 64,
            createdAt = Timestamp(Date(System.currentTimeMillis() - 2_400_000)),
            comments = listOf(Comment("comment-1", "Mina", avatar, "Love this look."))
        ),
        Post(
            id = "post-2",
            authorId = "user-local",
            authorName = "Local",
            authorAvt = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
            caption = "Soft neutrals and a little denim.",
            imageUrls = listOf("https://images.unsplash.com/photo-1529139574466-a303027c1d8b?w=900"),
            likeCount = 835,
            commentCount = 84,
            shareCount = 32,
            createdAt = Timestamp(Date(System.currentTimeMillis() - 7_200_000))
        )
    )

    val cartItems = listOf(
        CartItem("cart-1", products[0], "Blinging black", "S", 1),
        CartItem("cart-2", products[2], "Soft white", "M", 1),
        CartItem("cart-3", products[3], "Pink rose", "S", 1)
    )

    val orders = cartItems.map {
        Order(
            id = it.id.replace("cart", "order"),
            product = it.product,
            quantity = it.quantity,
            totalPrice = it.totalPrice,
            status = "pending"
        )
    }

    val reviewOrders = listOf(
        ReviewOrder("history-1", products[3], "Paid", "April 2026"),
        ReviewOrder("history-2", products[4], "Delivered", "April 2026"),
        ReviewOrder("history-3", products[1], "Delivered", "March 2026"),
        ReviewOrder("history-4", products[2], "Delivered", "March 2026")
    )

    val savedImages = listOf(
        "https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=500",
        "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=500",
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500",
        "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=500",
        "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=500",
        "https://images.unsplash.com/photo-1496747611176-843222e1e57c?w=500",
        "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=500",
        "https://images.unsplash.com/photo-1516826957135-700dedea698c?w=500",
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=500",
        "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=500",
        "https://images.unsplash.com/photo-1529139574466-a303027c1d8b?w=500",
        "https://images.unsplash.com/photo-1472214103451-9374bd1c798e?w=500"
    )
}
