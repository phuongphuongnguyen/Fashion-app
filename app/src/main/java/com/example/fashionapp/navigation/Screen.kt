package com.example.fashionapp.navigation

import androidx.annotation.DrawableRes
import com.example.fashionapp.R

// Tất cả màn hình của app
sealed class Screen(val route: String) {
    object Home    : Screen("home")
    object Shop    : Screen("shop")
    object Saved   : Screen("saved")
    object Profile : Screen("profile")
}

// Cấu hình 4 tab bottom nav
data class BottomNavItem(
    val screen: Screen,
    val label: String,
    @DrawableRes val iconRes: Int
)

val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.Home,
        label = "Home",
        iconRes = R.drawable.ic_home
    ),
    BottomNavItem(
        screen = Screen.Shop,
        label = "Shop",
        iconRes = R.drawable.ic_shopping
    ),
    BottomNavItem(
        screen = Screen.Saved,
        label = "Saved",
        iconRes = R.drawable.ic_saved
    ),
    BottomNavItem(
        screen = Screen.Profile,
        label = "Profile",
        iconRes = R.drawable.ic_profile
    )
)

