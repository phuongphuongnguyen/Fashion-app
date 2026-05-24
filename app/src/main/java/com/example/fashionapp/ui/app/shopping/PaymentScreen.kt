package com.example.fashionapp.ui.app.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import com.example.fashionapp.ui.components.FashionTopBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.fashionapp.ui.app.shopping.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    navController: NavController,
    viewModel: ShoppingViewModel = viewModel()
) {
    val settings = LocalAppSettings.current
    val isDark = settings.isDarkMode
    val bgColor = if (isDark) Color(0xFF121212) else Color.White
    val topBarBg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1A1A2E)
    val subTextColor = if (isDark) Color(0xFF888888) else Color.Gray
    val surfaceBg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val sectionBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF7F8FB)
    val editBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFE5EDFF)
    val itemBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF1F1F1)
    val payBtnColor = if (isDark) Color.White else Color.Black
    val payBtnTextColor = if (isDark) Color.Black else Color.White

    val uiState by viewModel.uiState.collectAsState()
    val items = uiState.cartItems
    Scaffold(
        topBar = {
            FashionTopBar(
                title = settings.t(
                    en = "Payment", vi = "Thanh toán", fr = "Paiement",
                    ja = "支払い", ko = "결제", zh = "支付"
                ),
                onBackClick = { navController.popBackStack() },
                isDark = isDark,
                bgColor = topBarBg,
                textColor = textColor
            )
        },
        bottomBar = {
            Surface(shadowElevation = 16.dp, color = surfaceBg) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            settings.t(en = "Total", vi = "Tổng", fr = "Total", ja = "合計", ko = "합계", zh = "总计"),
                            color = subTextColor, fontSize = 14.sp
                        )
                        Text(
                            "${"$%.2f".format(items.sumOf { it.totalPrice })}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = textColor
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.placeOrderFromCart()
                            navController.navigate(Screen.History.route) {
                                popUpTo(Screen.Payment.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = payBtnColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(
                            settings.t(en = "Pay", vi = "Thanh toán", fr = "Payer", ja = "支払う", ko = "결제하기", zh = "支付"),
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = payBtnTextColor
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Shipping Address
            item {
                SectionHeader(
                    settings.t(en = "Shipping Address", vi = "Địa chỉ giao hàng", fr = "Adresse de livraison", ja = "配送先住所", ko = "배송 주소", zh = "收货地址"),
                    textColor
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(sectionBg, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bonnie Green", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = textColor)
                        Spacer(Modifier.height(4.dp))
                        Text("De Nang Bul, Fashion Store, Da Nang", color = subTextColor, fontSize = 13.sp)
                    }
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(editBg).clickable { }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF0056FF), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Contact Information
            item {
                SectionHeader(
                    settings.t(en = "Contact Information", vi = "Thông tin liên hệ", fr = "Coordonnées", ja = "連絡先情報", ko = "연락처 정보", zh = "联系信息"),
                    textColor
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(sectionBg, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("+84 987 654 321", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = textColor)
                        Spacer(Modifier.height(4.dp))
                        Text("bonnie@email.com", color = subTextColor, fontSize = 13.sp)
                    }
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(editBg).clickable { }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF0056FF), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Items
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        settings.t(en = "Items", vi = "Sản phẩm", fr = "Articles", ja = "アイテム", ko = "상품", zh = "商品"),
                        fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(editBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${items.size}", color = Color(0xFF0056FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                items.take(2).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.product.imageUrl.ifEmpty { "https://via.placeholder.com/50" },
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(itemBg),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.product.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor)
                            Text("Lorem ipsum dolor sit amet", fontSize = 12.sp, color = subTextColor, maxLines = 1)
                        }
                        Text("${"$%.2f".format(item.totalPrice)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                    }
                }
            }

            // Shipping Options
            item {
                SectionHeader(
                    settings.t(en = "Shipping Options", vi = "Phương thức vận chuyển", fr = "Options de livraison", ja = "配送オプション", ko = "배송 옵션", zh = "配送方式"),
                    textColor
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShippingOption("Standard", "2-3 days", "FREE", true, isDark = isDark, textColor = textColor)
                    ShippingOption("Express", "1 day", "$10.00", false, isDark = isDark, textColor = textColor)
                }
            }

            // Payment Method
            item {
                SectionHeader(
                    settings.t(en = "Payment Method", vi = "Phương thức thanh toán", fr = "Mode de paiement", ja = "支払い方法", ko = "결제 수단", zh = "支付方式"),
                    textColor
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp, 30.dp)
                            .background(editBg, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Card", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0056FF))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("**** **** **** 1234", modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor)
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(editBg).clickable { }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF0056FF), modifier = Modifier.size(16.dp))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun SectionHeader(title: String, textColor: Color = Color.Black) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = textColor,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun ShippingOption(title: String, time: String, price: String, selected: Boolean, isDark: Boolean = false, textColor: Color = Color.Black) {
    val optionBg = if (selected) {
        if (isDark) Color(0xFF1A2A4A) else Color(0xFFF0F7FF)
    } else {
        if (isDark) Color(0xFF1E1E1E) else Color(0xFFF7F8FB)
    }
    val borderColor = if (selected) Color(0xFF0056FF) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(optionBg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) Color(0xFF0056FF) else Color.LightGray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
            Text(time, color = Color(0xFF0056FF), fontSize = 12.sp)
        }
        Text(price, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
    }
}
