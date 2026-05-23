package com.example.fashionapp.navigation
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.fashionapp.ui.app.saved.SavedScreen
import com.example.fashionapp.ui.app.profile.ProfileScreen
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

    // Lấy màn hình hiện tại
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Chỉ hiện bottom nav ở 4 tab chính
    val showBottomBar = currentDestination?.route in listOf(
        Screen.Home.route,
        Screen.Shop.route,
        Screen.Saved.route,
        Screen.Profile.route
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
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = Screen.FirstLoginOnboarding.route,
//            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
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

            // 4 màn hình chính
            composable(Screen.Home.route) {
                HomeScreen(navController = navController)
            }
            composable(Screen.Shop.route) {
                ShoppingScreen(navController = navController)
            }
            composable(Screen.Saved.route) {
                SavedScreen(navController = navController)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(navController = navController)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
            composable(Screen.Chatbot.route) {
                ChatbotScreen(navController = navController)
            }

            // ── Thành viên A thêm vào đây ──────────────
            // composable(Screen.Login.route) {
            //     LoginScreen(navController = navController)
            // }

            // ── Thành viên B thêm vào đây ──────────────
            // composable(Screen.ProductDetail.route) { ... }

            // ── Thành viên C thêm vào đây ──────────────
            // composable(Screen.Payment.route) { ... }
        }
    }
}

// ── Bottom Bar ────────────────────────────────────────────

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
                .navigationBarsPadding() // Thêm khoảng đệm để tránh bị thanh điều hướng Android che
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->

                val isSelected = currentDestination?.hierarchy?.any {
                    it.route == item.screen.route
                } == true

                BottomNavItemView(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onTabSelected(item.screen) }
                )
            }
        }
    }
}

// ── Từng tab ──────────────────────────────────────────────

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

        // Icon đổi màu theo trạng thái
        Icon(
            painter = painterResource(id = item.iconRes),
            contentDescription = item.label,
            tint = if (isSelected) Color.Black else Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Gạch dưới khi active
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (isSelected) Color.Black
                    else Color.Transparent
                )
        )
    }
}