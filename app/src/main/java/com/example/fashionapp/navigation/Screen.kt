package com.example.fashionapp.navigation

import androidx.annotation.DrawableRes
import com.example.fashionapp.R

sealed class Screen(val route: String) {
    // ── Auth ──────────────────────────────────────────────────────────────────
    object Start : Screen("start")
    object CreateAccount : Screen("create_account")
    object Login : Screen("login")
    object ForgotPassword : Screen("forgot_password")
    object VerifyResetCode : Screen("verify_reset_code/{email}")
    object ResetPassword : Screen("reset_password/{email}/{code}")
    object FirstLoginOnboarding : Screen("first_login_onboarding")

    // ── Main tabs ─────────────────────────────────────────────────────────────
    object Home    : Screen("home")
    object Shopping: Screen("shopping")
    object Shop    : Screen("shop/{shopId}") {
        fun createRoute(shopId: String) = "shop/$shopId"
    }
    object Saved   : Screen("saved")
    object Profile : Screen("profile")

    // ── App screens ───────────────────────────────────────────────────────────
    object Settings : Screen("settings")
    object Chatbot  : Screen("chatbot")
    object Messages : Screen("messages")
    object CreatePost : Screen("create_post?authorId={authorId}") {
        fun createRoute(authorId: String? = null): String {
            return if (authorId.isNullOrBlank()) {
                "create_post"
            } else {
                "create_post?authorId=$authorId"
            }
        }
    }

    object ShopDetail : Screen("shop_detail/{shopId}") {
        fun createRoute(shopId: String) = "shop_detail/$shopId"
    }
    object Search : Screen("search?query={query}&categoryId={categoryId}") {
        fun createRoute(query: String = "", categoryId: String? = null): String {
            val q = "search?query=$query"
            return if (categoryId != null) "$q&categoryId=$categoryId" else q
        }
    }

    object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
    }

    // ── Shopping flow ─────────────────────────────────────────────────────────
    object Cart       : Screen("cart")
    object Payment    : Screen("payment?cartItemIds={cartItemIds}") {
        fun createRoute(cartItemIds: Collection<String> = emptyList()): String {
            return if (cartItemIds.isEmpty()) {
                "payment"
            } else {
                "payment?cartItemIds=${cartItemIds.joinToString(",")}"
            }
        }
    }
    object History    : Screen("history")

    object Review : Screen("review/{orderId}") {
        fun createRoute(orderId: String) = "review/$orderId"
    }

    object ReviewDone : Screen("review_done")

    // ── Saved / Social ────────────────────────────────────────────────────────
    object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }

    // ── Generic helper ────────────────────────────────────────────────────────
    fun createRoute(vararg args: String): String {
        var builtRoute = route
        args.forEach { arg ->
            builtRoute = builtRoute.replaceFirst(Regex("\\{[^}]+\\}"), arg)
        }
        return builtRoute
    }
}

// ── Bottom Nav ────────────────────────────────────────────────────────────────

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    @DrawableRes val iconRes: Int
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,    "Home",    R.drawable.ic_home),
    BottomNavItem(Screen.Shopping,"Shop",    R.drawable.ic_shopping),
    BottomNavItem(Screen.Saved,   "Saved",   R.drawable.ic_saved),
    BottomNavItem(Screen.Profile, "Profile", R.drawable.ic_profile)
)
