package com.example.fashionapp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fashionapp.data.auth.AuthRepository
import com.example.fashionapp.data.auth.FirebaseAuthBackend
import com.example.fashionapp.data.onboarding.OnboardingPreferences
import com.google.firebase.auth.FirebaseAuth
import com.example.fashionapp.ui.app.home.HomeScreen
import com.example.fashionapp.ui.app.home.MessagesScreen
import com.example.fashionapp.ui.app.saved.SavedScreen
import com.example.fashionapp.ui.app.saved.PostDetailScreen
import com.example.fashionapp.ui.app.profile.ProfileScreen
import com.example.fashionapp.ui.app.shopping.CartScreen
import com.example.fashionapp.ui.app.shopping.HistoryScreen
import com.example.fashionapp.ui.app.shopping.PaymentScreen
import com.example.fashionapp.ui.app.shopping.ReviewDoneScreen
import com.example.fashionapp.ui.app.shopping.ReviewScreen
import com.example.fashionapp.ui.app.shopping.ShopScreen
import com.example.fashionapp.ui.app.shopping.ShoppingScreen
import com.example.fashionapp.ui.auth.CreateAccountScreen
import com.example.fashionapp.ui.auth.ForgotPasswordScreen
import com.example.fashionapp.ui.auth.LoginScreen
import com.example.fashionapp.ui.auth.ResetPasswordScreen
import com.example.fashionapp.ui.auth.StartScreen
import com.example.fashionapp.ui.auth.VerifyResetCodeScreen
import com.example.fashionapp.ui.onboarding.FirstLoginOnboardingScreen
import com.example.fashionapp.ui.app.settings.SettingsScreen
import com.example.fashionapp.ui.app.chatbot.ChatbotScreen
import com.example.fashionapp.ui.app.search.SearchScreen
import com.example.fashionapp.ui.app.productdetail.ProductDetailScreen
import com.example.fashionapp.ui.app.post.CreatePostScreen
import com.example.fashionapp.ui.app.shopping.MomoPaymentScreen
import com.example.fashionapp.ui.app.shopping.ShopViewModel

@Composable
fun AppNavigation(startDestination: String = Screen.Start.route) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val onboardingPreferences = remember { OnboardingPreferences(context) }
    val authRepository = remember { AuthRepository(FirebaseAuthBackend()) }

    fun navigateAfterAuthenticated() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val destination =
            if (uid.isNotEmpty() && !onboardingPreferences.isOnboardingCompleted(uid)) {
                Screen.FirstLoginOnboarding.route
            } else {
                Screen.Home.route
            }
        navController.navigate(destination) {
            popUpTo(Screen.Start.route) { inclusive = true }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in listOf(
        Screen.Home.route,
        Screen.Shopping.route,
        Screen.Shop.route,
        Screen.Saved.route,
        Screen.Profile.route,
        Screen.Cart.route,
        Screen.Payment.route,
        Screen.History.route,
        Screen.Review.route,
        Screen.ReviewDone.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentDestination = currentDestination,
                    onTabSelected = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize()
            ) {
                // ── Auth ──────────────────────────────────────────────────

                composable(Screen.Start.route) {
                    StartScreen(
                        onGetStarted = { navController.navigate(Screen.CreateAccount.route) },
                        onLoginClick = { navController.navigate(Screen.Login.route) }
                    )
                }

                composable(Screen.CreateAccount.route) {
                    CreateAccountScreen(
                        authRepository = authRepository,
                        onBack = { navController.popBackStack() },
                        onLoginClick = { navController.navigate(Screen.Login.route) },
                        onRegisterSuccess = { navigateAfterAuthenticated() }
                    )
                }

                composable(Screen.Login.route) {
                    LoginScreen(
                        authRepository = authRepository,
                        onBack = { navController.popBackStack() },
                        onCreateAccountClick = { navController.navigate(Screen.CreateAccount.route) },
                        onForgotPasswordClick = { navController.navigate(Screen.ForgotPassword.route) },
                        onLoginSuccess = { navigateAfterAuthenticated() }
                    )
                }

                composable(Screen.ForgotPassword.route) {
                    ForgotPasswordScreen(
                        authRepository = authRepository,
                        onBack = { navController.popBackStack() },
                        onCodeSent = { email ->
                            navController.navigate(Screen.VerifyResetCode.createRoute(email))
                        }
                    )
                }

                composable(
                    route = Screen.VerifyResetCode.route,
                    arguments = listOf(navArgument("email") { type = NavType.StringType })
                ) { backStackEntry ->
                    val email = backStackEntry.arguments?.getString("email").orEmpty()
                    VerifyResetCodeScreen(
                        authRepository = authRepository,
                        email = email,
                        onBack = { navController.popBackStack() },
                        onOtpVerified = { code ->
                            navController.navigate(Screen.ResetPassword.createRoute(email, code))
                        }
                    )
                }

                composable(
                    route = Screen.ResetPassword.route,
                    arguments = listOf(
                        navArgument("email") { type = NavType.StringType },
                        navArgument("code") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val email = backStackEntry.arguments?.getString("email").orEmpty()
                    val code = backStackEntry.arguments?.getString("code").orEmpty()
                    ResetPasswordScreen(
                        authRepository = authRepository,
                        email = email,
                        verifiedCode = code,
                        onBack = { navController.popBackStack() },
                        onPasswordResetSuccess = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    )
                }

                // ── Onboarding ────────────────────────────────────────────

                composable(Screen.FirstLoginOnboarding.route) {
                    FirstLoginOnboardingScreen(
                        onComplete = {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                            onboardingPreferences.markOnboardingCompleted(uid)
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.FirstLoginOnboarding.route) { inclusive = true }
                            }
                        }
                    )
                }

                // ── Main tabs ─────────────────────────────────────────────

                composable(Screen.Home.route) {
                    HomeScreen(navController = navController)
                }
                composable(Screen.Shopping.route) {
                    ShoppingScreen(navController = navController)
                }
                composable(
                    route = Screen.Shop.route,
                    arguments = listOf(navArgument("shopId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val shopId = backStackEntry.arguments?.getString("shopId").orEmpty()
                    ShopScreen(navController = navController, shopId = shopId)
                }
                composable(Screen.Saved.route) {
                    SavedScreen(navController = navController)
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(navController = navController)
                }

                // ── App screens ───────────────────────────────────────────

                composable(Screen.Settings.route) {
                    SettingsScreen(navController = navController)
                }
                composable(Screen.Chatbot.route) {
                    ChatbotScreen(navController = navController)
                }
                composable(Screen.Messages.route) {
                    MessagesScreen(navController = navController)
                }
                composable(
                    route = Screen.CreatePost.route,
                    arguments = listOf(
                        navArgument("authorId") { nullable = true; defaultValue = null; type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    CreatePostScreen(
                        navController = navController,
                        authorId = backStackEntry.arguments?.getString("authorId")
                    )
                }

                composable(
                    route = Screen.Search.route,
                    arguments = listOf(
                        navArgument("query") { defaultValue = ""; type = NavType.StringType },
                        navArgument("categoryId") { nullable = true; defaultValue = null; type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val query = backStackEntry.arguments?.getString("query").orEmpty()
                    val catId = backStackEntry.arguments?.getString("categoryId")
                    SearchScreen(
                        navController = navController,
                        initialQuery = query,
                        initialCategoryId = catId
                    )
                }

                composable(
                    route = Screen.ProductDetail.route,
                    arguments = listOf(navArgument("productId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val productId = backStackEntry.arguments?.getString("productId").orEmpty()
                    ProductDetailScreen(
                        productId = productId,
                        navController = navController
                    )
                }

                composable(
                    route = Screen.ShopDetail.route,
                    arguments = listOf(navArgument("shopId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val shopId = backStackEntry.arguments?.getString("shopId").orEmpty()
                    ShopScreen(
                        shopId        = shopId,
                        navController = navController
                    )
                }

                // ── Shopping flow ─────────────────────────────────────────

                composable(Screen.Cart.route) {
                    CartScreen(navController = navController)
                }
                composable(
                    route = Screen.Payment.route,
                    arguments = listOf(
                        navArgument("cartItemIds") { nullable = true; defaultValue = null; type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val selectedIds = backStackEntry.arguments
                        ?.getString("cartItemIds")
                        ?.split(",")
                        ?.filter { it.isNotBlank() }
                        ?.toSet()
                        .orEmpty()
                    PaymentScreen(navController = navController, selectedCartItemIds = selectedIds)
                }
                composable(
                    route = Screen.MomoPayment.route,
                    arguments = listOf(
                        navArgument("amount") { type = NavType.LongType },
                        navArgument("cartItemIds") { nullable = true; defaultValue = null; type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val amount = backStackEntry.arguments?.getLong("amount") ?: 0L
                    val selectedIds = backStackEntry.arguments
                        ?.getString("cartItemIds")
                        ?.split(",")
                        ?.filter { it.isNotBlank() }
                        ?.toSet()
                        .orEmpty()
                    val shopViewModel: ShopViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    MomoPaymentScreen(
                        amount = amount,
                        navController = navController,
                        selectedCartItemIds = selectedIds,
                        viewModel = shopViewModel
                    )
                }
                composable(
                    route = Screen.History.route,
                    arguments = listOf(navArgument("tab") { defaultValue = "Ongoing"; type = NavType.StringType })
                ) { backStackEntry ->
                    HistoryScreen(
                        navController = navController,
                        initialTab = backStackEntry.arguments?.getString("tab") ?: "Ongoing"
                    )
                }
                composable(
                    route = Screen.Review.route,
                    arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
                    ReviewScreen(navController = navController, orderId = orderId)
                }
                composable(Screen.ReviewDone.route) {
                    ReviewDoneScreen(navController = navController)
                }

                // ── Saved / Social ────────────────────────────────────────

                composable(
                    route = Screen.PostDetail.route,
                    arguments = listOf(navArgument("postId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val postId = backStackEntry.arguments?.getString("postId").orEmpty()
                    PostDetailScreen(navController = navController, postId = postId)
                }
            }
        }
    }
}

// ── Bottom Bar ────────────────────────────────────────────────────────────────

@Composable
fun AppBottomBar(
    currentDestination: NavDestination?,
    onTabSelected: (Screen) -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val currentRoute = currentDestination?.route
                val isSelected = when (item.screen) {
                    Screen.Shopping -> currentRoute in listOf(
                        Screen.Shopping.route,
                        Screen.Shop.route,
                        Screen.Cart.route,
                        Screen.Payment.route,
                        Screen.History.route,
                        Screen.Review.route,
                        Screen.ReviewDone.route
                    )
                    else -> currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                }

                BottomNavItemView(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onTabSelected(item.screen) }
                )
            }
        }
    }
}

// ── Bottom Nav Item ───────────────────────────────────────────────────────────

@Composable
fun BottomNavItemView(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = item.iconRes),
            contentDescription = item.label,
            tint = if (isSelected) Color.Black else Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isSelected) Color.Black else Color.Transparent)
        )
    }
}
