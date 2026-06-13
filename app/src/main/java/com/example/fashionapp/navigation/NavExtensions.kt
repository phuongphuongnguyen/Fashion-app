package com.example.fashionapp.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.fashionapp.data.user.UserSession
import com.google.firebase.auth.FirebaseAuth

/**
 * Nếu targetId là tài khoản đang đăng nhập thì điều hướng sang profile chính
 */
fun NavController.openProfileOrShop(targetId: String) {
    if (targetId.isBlank()) return

    val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val currentShopId = UserSession.currentUser.value?.shopId.orEmpty()
    val isSelf = targetId == currentUid ||
        (currentShopId.isNotBlank() && targetId == currentShopId)

    if (isSelf) {
        navigate(Screen.Profile.route) {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    } else {
        navigate(Screen.ShopDetail.createRoute(targetId))
    }
}
