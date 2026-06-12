package com.example.fashionapp.ui.app.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.fashionapp.data.user.UserRepository
import com.example.fashionapp.ui.app.shopping.ShopScreen

/**
 * Điểm vào chung cho "trang cá nhân" từ feed / search / product detail.
 * Resolve chủ tài khoản theo [id] (có thể là userId hoặc shopId) rồi rẽ UI theo role:
 *  - role == "shop"  → [ShopScreen] (giao diện shop)
 *  - còn lại          → [ProfileScreen] (giao diện user thường, read-only + Follow)
 */
@Composable
fun ProfileRouterScreen(
    navController: NavController,
    id: String
) {
    val role by produceState<String?>(initialValue = null, id) {
        val repo = UserRepository()
        val owner = repo.getUserProfile(id) ?: repo.findUserByShopId(id)
        value = (owner?.role ?: "").lowercase().ifBlank { "user" }
    }

    when (role) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        "shop" -> ShopScreen(shopId = id, navController = navController)
        else -> ProfileScreen(navController = navController, userId = id)
    }
}
