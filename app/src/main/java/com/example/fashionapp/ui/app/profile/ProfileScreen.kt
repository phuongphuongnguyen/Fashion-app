package com.example.fashionapp.ui.app.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fashionapp.R
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.app.settings.LocalAppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val settings = LocalAppSettings.current
    val isDark = settings.isDarkMode
    
    // Dynamic styling
    val bgColor = if (isDark) Color(0xFF121212) else Color.White
    val topBarBgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1A1A2E)
    val iconColor = if (isDark) Color.White else Color.Black

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = settings.t(
                            en = "Profile",
                            vi = "Hồ sơ",
                            fr = "Profil",
                            ja = "プロフィール",
                            ko = "프로필",
                            zh = "个人中心"
                        ),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Chatbot icon
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.Chatbot.route)
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chatbot),
                            contentDescription = "Chatbot",
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    // Settings icon
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.Settings.route)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = iconColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarBgColor,
                    titleContentColor = textColor,
                    actionIconContentColor = textColor
                )
            )
        },
        containerColor = bgColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = settings.t(
                    en = "Profile Screen",
                    vi = "Màn hình hồ sơ",
                    fr = "Écran de profil",
                    ja = "プロフィール画面",
                    ko = "프로필 화면",
                    zh = "个人中心"
                ),
                color = textColor,
                fontSize = 16.sp
            )
        }
    }
}
