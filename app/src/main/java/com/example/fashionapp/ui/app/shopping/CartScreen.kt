package com.example.fashionapp.ui.app.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.fashionapp.ui.components.FashionTopBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.fashionapp.data.CartItem
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.app.shopping.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
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
    val surfaceBg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val qtyBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)
    val qtyBtnBg = if (isDark) Color(0xFF3A3A3A) else Color.White
    val iconColor = if (isDark) Color.White else Color.Black
    val imgPlaceholder = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEDEDED)

    val uiState by viewModel.uiState.collectAsState()
    val items = uiState.cartItems
    val total = items.sumOf { it.totalPrice }

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
                textColor = textColor,
                actions = {
                    TextButton(onClick = { /* TODO: Edit cart */ }) {
                        Text(
                            settings.t(en = "Edit", vi = "Sửa", fr = "Modifier", ja = "編集", ko = "편집", zh = "编辑"),
                            color = Color(0xFF0057FF),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 16.dp, color = surfaceBg) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            settings.t(en = "Total", vi = "Tổng", fr = "Total", ja = "合計", ko = "합계", zh = "总计"),
                            color = subTextColor, fontSize = 12.sp
                        )
                        Text("${"$%.2f".format(total)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
                    }
                    Button(
                        onClick = {
                            navController.navigate(Screen.Payment.route) {
                                launchSingleTop = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0057FF)),
                        modifier = Modifier.width(140.dp).height(48.dp)
                    ) {
                        Text(
                            settings.t(en = "Checkout", vi = "Thanh toán", fr = "Payer", ja = "チェックアウト", ko = "결제", zh = "结账"),
                            fontSize = 15.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = bgColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CartHistoryTabs(
                    selected = "Cart",
                    onCart = {},
                    onHistory = {
                        navController.navigate(Screen.History.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            items(items, key = { it.id }) { item ->
                CartItemRow(
                    item = item,
                    onUpdateQuantity = { newQty ->
                        viewModel.updateCartQuantity(item.id, newQty)
                    },
                    isDark = isDark,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    cardBg = cardBg,
                    qtyBg = qtyBg,
                    qtyBtnBg = qtyBtnBg,
                    iconColor = iconColor,
                    imgPlaceholder = imgPlaceholder
                )
            }
        }
    }
}



@Composable
private fun ShippingAddressCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF7F8FB))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Shipping Address", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Da Nang Bul, Fashion Store, Da Nang", color = Color.Gray, fontSize = 12.sp)
        }
        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF0057FF), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onUpdateQuantity: (Int) -> Unit,
    isDark: Boolean,
    textColor: Color,
    subTextColor: Color,
    cardBg: Color,
    qtyBg: Color,
    qtyBtnBg: Color,
    iconColor: Color,
    imgPlaceholder: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = true,
            onCheckedChange = {},
            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0057FF)),
            modifier = Modifier.size(34.dp)
        )
        Box(modifier = Modifier.size(64.dp)) {
            AsyncImage(
                model = item.product.imageUrl,
                contentDescription = item.product.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(imgPlaceholder),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .clip(RoundedCornerShape(topEnd = 6.dp, bottomStart = 6.dp))
                    .background(Color(0xFFE53935))
                    .padding(4.dp)
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.product.name, fontWeight = FontWeight.SemiBold, maxLines = 1, fontSize = 13.sp, color = textColor)
            Spacer(Modifier.height(2.dp))
            Text("Size ${item.size}", color = subTextColor, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${"$%.2f".format(item.product.price)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f), color = textColor)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(qtyBg)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    QuantityButton(onClick = { onUpdateQuantity(item.quantity - 1) }, bgColor = qtyBtnBg) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = iconColor, modifier = Modifier.size(14.dp))
                    }
                    Text(item.quantity.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp), color = textColor)
                    QuantityButton(onClick = { onUpdateQuantity(item.quantity + 1) }, bgColor = qtyBtnBg) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = iconColor, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityButton(onClick: () -> Unit, bgColor: Color = Color.White, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
