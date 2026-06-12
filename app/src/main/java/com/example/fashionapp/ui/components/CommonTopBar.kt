package com.example.fashionapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fashionapp.R
import com.example.fashionapp.ui.app.settings.AppSettingsViewModel
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.theme.AppTheme

// ─────────────────────────────────────────────────────────────────────────────
//  GENERIC TOP BAR (dùng chung cho các màn hình có back + actions tùy chỉnh)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FashionTopBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            Row(
                modifier = Modifier.padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((-4).dp)
            ) {
                actions()
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        windowInsets = TopAppBarDefaults.windowInsets,
        scrollBehavior = scrollBehavior
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  HOME TOP BAR
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onSearchClick: () -> Unit = {},
    onMessClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = "FashionApp",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        actions = {
            Row(
                modifier = Modifier.padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionIconButton(adaptiveIcon(R.drawable.ic_search, R.drawable.ic_search_dark), "Search", onSearchClick)
                ActionIconButton(adaptiveIcon(R.drawable.ic_messbox, R.drawable.ic_messbox_dark), "Messages", onMessClick)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        windowInsets = TopAppBarDefaults.windowInsets,
        scrollBehavior = scrollBehavior
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  SHOPPING TOP BAR
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingTopBar(
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onSearchClick: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onCartClick: () -> Unit = {}
) {
    val settings = LocalAppSettings.current
    TopAppBar(
        title = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(42.dp),
                shape = RoundedCornerShape(21.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onSearchClick() }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = settings.t("Search", "Tìm kiếm"),
                        color = AppTheme.colors.textSecondary,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onCameraClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_camera),
                            contentDescription = "Camera",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        },
        actions = {
            Row(
                modifier = Modifier.padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionIconButton(adaptiveIcon(R.drawable.ic_cart, R.drawable.ic_cart_dark), "Cart", onCartClick)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        windowInsets = TopAppBarDefaults.windowInsets,
        scrollBehavior = scrollBehavior
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  SAVED TOP BAR
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedTopBar(
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val settings = LocalAppSettings.current
    TopAppBar(
        title = { Text(settings.t("Saved Items", "Mục đã lưu"), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        windowInsets = TopAppBarDefaults.windowInsets,
        scrollBehavior = scrollBehavior
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  PROFILE TOP BAR
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopBar(
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onSettingsClick: () -> Unit = {}
) {
    val settings = LocalAppSettings.current
    TopAppBar(
        title = { Text(settings.t("Profile", "Trang cá nhân"), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        actions = {
            Row(
                modifier = Modifier.padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionIconButton(adaptiveIcon(R.drawable.ic_setting, R.drawable.ic_setting_dark), "Settings", onSettingsClick)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        windowInsets = TopAppBarDefaults.windowInsets,
        scrollBehavior = scrollBehavior
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  COMMON COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

// Chọn biến thể icon theo dark mode (in-app toggle, không phải uiMode hệ thống).
@Composable
private fun adaptiveIcon(light: Int, dark: Int): Int =
    if (LocalAppSettings.current.isDarkMode) dark else light

@Composable
private fun ActionIconButton(iconRes: Int, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(34.dp),
            tint = Color.Unspecified
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PREVIEWS
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HomeTopBarPreview() {
    HomeTopBar()
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun ShoppingTopBarPreview() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("preview_prefs", Context.MODE_PRIVATE)
    val settings = remember { AppSettingsViewModel(prefs) }
    CompositionLocalProvider(LocalAppSettings provides settings) {
        ShoppingTopBar()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun FashionTopBarPreview() {
    FashionTopBar(title = "Detail", onBackClick = {})
}