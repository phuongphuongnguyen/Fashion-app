package com.example.fashionapp.ui.app.shopping

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import com.example.fashionapp.ui.components.FashionTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage

import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import kotlinx.coroutines.delay

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fashionapp.ui.app.shopping.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: ShoppingViewModel = viewModel()
) {
    val settings = LocalAppSettings.current
    val isDark = settings.isDarkMode
    val bgColor = if (isDark) Color(0xFF121212) else Color.White
    val topBarBg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1A1A2E)
    val subTextColor = if (isDark) Color(0xFF888888) else Color.Gray
    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val imgPlaceholder = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF1F1F1)

    val uiState by viewModel.uiState.collectAsState()
    val orders = uiState.orders
    Scaffold(
        topBar = {
            FashionTopBar(
                title = settings.t(
                    en = "Shopping Cart", vi = "Giỏ hàng", fr = "Panier",
                    ja = "ショッピングカート", ko = "장바구니", zh = "购物车"
                ),
                onBackClick = { navController.popBackStack() },
                isDark = isDark,
                bgColor = topBarBg,
                textColor = textColor
            )
        },
        containerColor = bgColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CartHistoryTabs(
                    selected = "History",
                    onCart = {
                        navController.navigate(Screen.Cart.route) {
                            popUpTo(Screen.Cart.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onHistory = {}
                )
            }
            items(orders, key = { it.id }) { order ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = order.product.imageUrl,
                        contentDescription = order.product.name,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(imgPlaceholder),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(order.product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Order #${order.id.takeLast(1).padStart(5, '0')}",
                            color = subTextColor,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(order.orderDate, color = Color(0xFF0057FF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = {
                            navController.navigate(Screen.Review.createRoute(order.id)) {
                                launchSingleTop = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF1A2A4A) else Color(0xFFE5EDFF),
                            contentColor = Color(0xFF0057FF)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Text(
                            settings.t(en = "Review", vi = "Đánh giá", fr = "Avis", ja = "レビュー", ko = "리뷰", zh = "评价"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartHistoryTabs(
    selected: String,
    onCart: () -> Unit,
    onHistory: () -> Unit
) {
    val settings = LocalAppSettings.current
    val isDark = settings.isDarkMode
    val tabBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)
    val pillActiveBg = if (isDark) Color(0xFF3A3A3A) else Color.White
    val pillActiveText = if (isDark) Color.White else Color.Black
    val pillInactiveText = if (isDark) Color(0xFF888888) else Color.Gray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 16.dp)
            .background(tabBg, RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SegmentPill("Cart", selected == "Cart", onClick = onCart, modifier = Modifier.weight(1f), activeBg = pillActiveBg, activeText = pillActiveText, inactiveText = pillInactiveText)
        SegmentPill("Ongoing", selected == "Ongoing", onClick = {}, modifier = Modifier.weight(1f), activeBg = pillActiveBg, activeText = pillActiveText, inactiveText = pillInactiveText)
        SegmentPill("History", selected == "History", onClick = onHistory, modifier = Modifier.weight(1f), activeBg = pillActiveBg, activeText = pillActiveText, inactiveText = pillInactiveText)
    }
}

@Composable
private fun SegmentPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeBg: Color = Color.White,
    activeText: Color = Color.Black,
    inactiveText: Color = Color.Gray
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) activeBg else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) activeText else inactiveText, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    navController: NavController,
    orderId: String,
    viewModel: ShoppingViewModel = viewModel()
) {
    val settings = LocalAppSettings.current
    val isDark = settings.isDarkMode
    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1A1A2E)
    val subTextColor = if (isDark) Color(0xFF888888) else Color.Gray
    val inputBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)
    val overlayBg = if (isDark) Color(0xCC121212) else Color(0x99F7F7F7)

    val uiState by viewModel.uiState.collectAsState()
    val orderToReview = uiState.orders.firstOrNull { it.id == orderId }
    val productToReview = orderToReview?.product
    var rating by remember { mutableStateOf(4) }
    var comment by remember { mutableStateOf("") }
    Scaffold(
        containerColor = overlayBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        settings.t(en = "Review", vi = "Đánh giá", fr = "Avis", ja = "レビュー", ko = "리뷰", zh = "评价"),
                        fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (productToReview != null) {
                            AsyncImage(
                                model = productToReview.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(productToReview.name, fontWeight = FontWeight.SemiBold, color = textColor)
                                Text(
                                    "Order #${orderToReview.id.takeLast(1).padStart(5, '0')}",
                                    color = subTextColor,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            Text("Order not found", color = subTextColor, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row {
                        repeat(5) {
                            Icon(
                                Icons.Outlined.StarBorder,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    TextField(
                        value = comment,
                        onValueChange = { comment = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        placeholder = {
                            Text(
                                settings.t(en = "Your comment", vi = "Bình luận của bạn", fr = "Votre commentaire", ja = "コメント", ko = "댓글", zh = "您的评论"),
                                color = subTextColor
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            navController.navigate(Screen.ReviewDone.route) {
                                popUpTo(Screen.Review.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0057FF)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            settings.t(en = "Send it", vi = "Gửi", fr = "Envoyer", ja = "送信", ko = "보내기", zh = "发送")
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDoneScreen(navController: NavController) {
    val settings = LocalAppSettings.current
    val isDark = settings.isDarkMode
    val overlayBg = if (isDark) Color(0xCC121212) else Color(0xAA000000)
    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color(0xFF888888) else Color.Gray
    val checkOuterBg = if (isDark) Color(0xFF2C2C2C) else Color.White

    LaunchedEffect(Unit) {
        delay(1400)
        navController.navigate(Screen.History.route) {
            popUpTo(Screen.History.route) { inclusive = false }
            launchSingleTop = true
        }
    }

    Scaffold(containerColor = overlayBg) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    modifier = Modifier.padding(top = 28.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            settings.t(en = "Done!", vi = "Hoàn tất!", fr = "Terminé!", ja = "完了!", ko = "완료!", zh = "完成!"),
                            fontWeight = FontWeight.Black, fontSize = 20.sp, color = textColor
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            settings.t(en = "Thank you for your review", vi = "Cảm ơn bạn đã đánh giá", fr = "Merci pour votre avis", ja = "レビューありがとうございます", ko = "리뷰 감사합니다", zh = "感谢您的评价"),
                            color = subTextColor, fontSize = 14.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(5) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }

                // Floating checkmark
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(checkOuterBg)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF0057FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
