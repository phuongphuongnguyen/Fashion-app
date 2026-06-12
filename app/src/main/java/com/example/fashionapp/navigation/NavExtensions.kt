package com.example.fashionapp.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.fashionapp.data.user.UserSession
import com.google.firebase.auth.FirebaseAuth

/**
 * Mở trang cá nhân của [targetId] — có thể là userId (bấm vào tác giả post) hoặc shopId
 * (bấm vào shop ở product detail).
 *
 * Nếu [targetId] chính là tài khoản đang đăng nhập (so khớp cả userId lẫn shopId) thì
 * chuyển sang TAB Profile (tab 4) giống như bấm vào tab — GIỮ bottom navigation, không có
 * nút back của ShopDetail. Ngược lại mở [Screen.ShopDetail] như bình thường.
 */
fun NavController.openProfileOrShop(targetId: String) {
    if (targetId.isBlank()) return

    val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val currentShopId = UserSession.currentUser.value?.shopId.orEmpty()
    val isSelf = targetId == currentUid ||
        (currentShopId.isNotBlank() && targetId == currentShopId)

    if (isSelf) {
        // Giống hệt onTabSelected của bottom nav → vào tab Profile, vẫn còn bottom bar.
        navigate(Screen.Profile.route) {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    } else {
        navigate(Screen.ShopDetail.createRoute(targetId))
    }
}
