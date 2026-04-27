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
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fashionapp.ui.app.home.HomeScreen
import com.example.fashionapp.ui.app.saved.SavedScreen
import com.example.fashionapp.ui.app.profile.ProfileScreen
import com.example.fashionapp.ui.app.shopping.ShoppingScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

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
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
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