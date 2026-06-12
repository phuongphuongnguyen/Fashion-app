package com.example.fashionapp.ui.app.shopDashboard

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
import com.example.fashionapp.data.user.UserSession
import com.example.fashionapp.ui.app.saved.SavedScreen
import com.google.firebase.auth.FirebaseAuth

/**
 * Tab thứ 3 (icon Saved) rẽ theo role của tài khoản đang đăng nhập:
 *  - role == "shop" → [ShopDashboardScreen] (quản lý shop: doanh thu, sản phẩm)
 *  - còn lại         → [SavedScreen] (bài viết đã lưu của user thường)
 *
 * Role đọc ưu tiên từ [UserSession] (đã có sẵn → hiện ngay), đồng thời xác thực
 * lại bằng Firestore users/{uid}. So sánh không phân biệt hoa/thường.
 */
@Composable
fun SavedTabRouter(navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    val role by produceState<String?>(initialValue = null, uid) {
        if (uid.isBlank()) {
            value = "user"
            return@produceState
        }
        // Hiện ngay theo session nếu đã có role
        val sessionRole = UserSession.currentUser.value?.role
        if (!sessionRole.isNullOrBlank()) {
            value = sessionRole.lowercase()
        }
        // Xác thực lại từ Firestore
        val fetched = UserRepository().getUserProfile(uid)?.role
        value = (fetched ?: sessionRole ?: "").lowercase().ifBlank { "user" }
    }

    when (role) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        "shop" -> ShopDashboardScreen(navController = navController)
        else -> SavedScreen(navController = navController)
    }
}