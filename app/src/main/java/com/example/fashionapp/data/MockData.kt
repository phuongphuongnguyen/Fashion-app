package com.example.fashionapp.data

import com.example.fashionapp.model.Message
import com.example.fashionapp.model.Product
import com.example.fashionapp.model.User
import com.example.fashionapp.model.Order
object MockData {

    val products = listOf(
        Product("1", "Áo thun trắng basic", 150000.0, "", rating = 4.5f, soldCount = 320),
        Product("2", "Váy hoa nhí dáng xòe", 280000.0, "", rating = 4.8f, soldCount = 150),
        Product("3", "Quần jeans ống rộng", 320000.0, "", rating = 4.3f, soldCount = 210),
        Product("4", "Áo khoác denim", 450000.0, "", rating = 4.6f, soldCount = 89),
        Product("5", "Đầm maxi boho", 390000.0, "", rating = 4.7f, soldCount = 175)
    )

    val users = listOf(
        User("u1", "Bonnie Green", image = "c", email = "bonnie@email.com"),
        User("u2", "Jese Leos", image = "c", email = "jese@email.com"),
        User("u3", "Butee Shop", "c","sa")
    )

    val orders = listOf(
        Order("o1", products[0], quantity = 1, totalPrice = 150000.0, status = "confirmed"),
        Order("o2", products[2], quantity = 2, totalPrice = 640000.0, status = "shipping")
    )

    val messages = listOf(
        Message("m1", senderId = "u1", content = "Cho mình hỏi size M còn không ạ?"),
        Message("m2", senderId = "u3", content = "Dạ bên shop còn size M bạn nhé!"),
        Message("m3", senderId = "u1", content = "Cho mình đặt 1 cái nhé shop ơi")
    )
}