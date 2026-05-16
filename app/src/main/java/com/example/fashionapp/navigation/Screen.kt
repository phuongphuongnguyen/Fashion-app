package com.example.fashionapp.navigation

import androidx.annotation.DrawableRes
import com.example.fashionapp.R

// Tất cả màn hình của app
sealed class Screen(val route: String) {
    object Start : Screen("start")
    object CreateAccount : Screen("create_account")
    object Login : Screen("login")
    object ForgotPassword : Screen("forgot_password")
    object VerifyResetCode : Screen("verify_reset_code/{email}")
    object ResetPassword : Screen("reset_password/{email}/{code}")
    object FirstLoginOnboarding : Screen("first_login_onboarding")
    object Home    : Screen("home")
    object Shop    : Screen("shop")
    object Saved   : Screen("saved")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object Chatbot : Screen("chatbot")
    object Cart : Screen("cart")
    object Payment : Screen("payment")
    object History : Screen("history")
    object Review : Screen("review")
    object ReviewDone : Screen("review_done")

    fun createRoute(vararg args: String): String {
        var builtRoute = route
        args.forEach { arg ->
            builtRoute = builtRoute.replaceFirst(Regex("\\{[^}]+\\}"), arg)
        }
        return builtRoute
    }
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

