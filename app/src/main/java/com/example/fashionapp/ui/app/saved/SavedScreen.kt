package com.example.fashionapp.ui.app.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.example.fashionapp.ui.components.FashionTopBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.app.saved.SavedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    navController: NavController,
    viewModel: SavedViewModel = viewModel()
) {
    val settings = LocalAppSettings.current
    val isDark = settings.isDarkMode
    val bgColor = if (isDark) Color(0xFF121212) else Color.White
    val topBarBg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1A1A2E)
    val iconBgColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF7F9FF)
    val iconTintColor = if (isDark) Color.White else Color(0xFF0057FF)
    val cardBgColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFECECEC)

    val uiState by viewModel.uiState.collectAsState()
    val savedPosts = uiState.savedPosts

    Scaffold(
        topBar = {
            FashionTopBar(
                title = settings.t(
                    en = "Saved",
                    vi = "Đã lưu",
                    fr = "Enregistré",
                    ja = "保存済み",
                    ko = "저장됨",
                    zh = "已保存"
                ),
                isDark = isDark,
                bgColor = topBarBg,
                textColor = textColor,
                actions = {
                    SavedHeaderIcon(onClick = { navController.navigate(Screen.Cart.route) }, bgColor = iconBgColor) {
                        Icon(Icons.Outlined.ShoppingCart, contentDescription = "Cart", tint = iconTintColor)
                    }
                    Spacer(Modifier.width(8.dp))
                    SavedHeaderIcon(onClick = {}, bgColor = iconBgColor) {
                        Icon(Icons.Outlined.FilterList, contentDescription = "Filter", tint = iconTintColor)
                    }
                    Spacer(Modifier.width(8.dp))
                    SavedHeaderIcon(onClick = {}, bgColor = iconBgColor) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = iconTintColor)
                    }
                }
            )
        },
        containerColor = bgColor
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(savedPosts, key = { _, post -> post.id }) { index, post ->
                val imageUrl = post.imageUrls.firstOrNull()
                if (imageUrl != null) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(if (index % 5 == 1) 0.62f else 0.78f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(cardBgColor)
                            .clickable { navController.navigate(Screen.PostDetail.createRoute(post.id)) }
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedHeaderIcon(
    onClick: () -> Unit,
    bgColor: Color = Color(0xFFF7F9FF),
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(bgColor)
    ) {
        content()
    }
}
