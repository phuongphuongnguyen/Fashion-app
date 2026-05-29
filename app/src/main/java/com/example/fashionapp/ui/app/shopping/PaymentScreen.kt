package com.example.fashionapp.ui.app.shopping

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.fashionapp.R
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.components.FashionTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    navController: NavController,
    selectedCartItemIds: Set<String> = emptySet(),
    viewModel: ShopViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val items = uiState.cartItems
    val checkoutItems = remember(items, selectedCartItemIds) {
        if (selectedCartItemIds.isEmpty()) {
            items
        } else {
            items.filter { it.id in selectedCartItemIds }
        }
    }
    val itemsTotal = checkoutItems.sumOf { it.totalPrice }
    var selectedPaymentMethod by remember { mutableStateOf("visa") }
    var selectedShippingMethod by remember { mutableStateOf("standard") }
    val shippingFee = if (selectedShippingMethod == "express") 50000.0 else 0.0
    val total = itemsTotal + shippingFee
    val shippingAddress = "Danang Bul, Fashion Store, Da Nang City"

    Scaffold(
        topBar = {
            FashionTopBar(
                title = "Checkout",
                onBackClick = { navController.popBackStack() }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 16.dp, color = Color.White) {
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
                        Text("Order Total", color = Color.Gray, fontSize = 14.sp)
                        Text(
                            "₫${formatPrice(total)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color(0xFF1A1A1A)
                        )
                    }
                    Button(
                        //ấn place order chuyenr qua momo
                        onClick = {
                            if (selectedPaymentMethod == "momo") {
                                navController.navigate(
                                    Screen.MomoPayment.createRoute(
                                        amount = total.toLong(),
                                        cartItemIds = checkoutItems.map { it.id }
                                    )
                                )
                            } else {
                                viewModel.placeOrderFromCart(
                                    cartItems = checkoutItems,
                                    paymentMethod = selectedPaymentMethod,
                                    shippingMethod = selectedShippingMethod,
                                    shippingFee = shippingFee,
                                    shippingAddress = shippingAddress
                                )
                                navController.navigate(Screen.History.createRoute("Ongoing")) {
                                    popUpTo(Screen.Payment.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0056FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = checkoutItems.isNotEmpty()
                    ) {
                        Text("Place Order", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = Color.White
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // Shipping Address
            item {
                SectionHeader("Shipping Address")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF7F8FB), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bonnie Green", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(shippingAddress, color = Color.Gray, fontSize = 13.sp)
                    }
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE5EDFF)).clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, null, tint = Color(0xFF0056FF), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Items Summary
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Items", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFFE5EDFF),
                        shape = CircleShape,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${checkoutItems.size}", color = Color(0xFF0056FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                checkoutItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.product.imageUrl.ifBlank { null },
                            contentDescription = null,
                            modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F5F5)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.ic_launcher_foreground)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.product.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text("${item.color} | ${item.size} x ${item.quantity}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text("₫${formatPrice(item.totalPrice)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Shipping Options
            item {
                SectionHeader("Shipping Options")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShippingOption(
                        id = "standard",
                        selectedId = selectedShippingMethod,
                        title = "Standard Delivery",
                        time = "Arrival in 3-5 days",
                        price = "FREE",
                        onSelected = { selectedShippingMethod = it }
                    )
                    ShippingOption(
                        id = "express",
                        selectedId = selectedShippingMethod,
                        title = "Express Delivery",
                        time = "Arrival in 1-2 days",
                        price = "₫50.000",
                        onSelected = { selectedShippingMethod = it }
                    )
                }
            }

            // Payment Method
            item {
                SectionHeader("Payment Method")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PaymentMethodOption(
                        id = "visa",
                        selectedId = selectedPaymentMethod,
                        badgeText = "VISA",
                        badgeColor = Color(0xFFE5EDFF),
                        badgeTextColor = Color(0xFF0056FF),
                        title = "Visa",
                        subtitle = "**** **** **** 4567",
                        onSelected = { selectedPaymentMethod = it }
                    )
                    PaymentMethodOption(
                        id = "momo",
                        selectedId = selectedPaymentMethod,
                        badgeText = "MoMo",
                        badgeColor = Color(0xFFA50064),
                        badgeTextColor = Color.White,
                        title = "MoMo",
                        subtitle = "Pay with MoMo e-wallet",
                        onSelected = { selectedPaymentMethod = it }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun PaymentMethodOption(
    id: String,
    selectedId: String,
    badgeText: String,
    badgeColor: Color,
    badgeTextColor: Color,
    title: String,
    subtitle: String,
    onSelected: (String) -> Unit
) {
    val selected = id == selectedId
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFFF0F7FF) else Color(0xFFF7F8FB))
            .border(1.dp, if (selected) Color(0xFF0056FF) else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable { onSelected(id) }
            .padding(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(52.dp, 32.dp),
            shape = RoundedCornerShape(6.dp),
            color = badgeColor,
            border = if (selected) BorderStroke(1.dp, Color(0xFF0056FF)) else null
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Black, color = badgeTextColor)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Icon(
            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) Color(0xFF0056FF) else Color.LightGray,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        modifier = Modifier.padding(bottom = 12.dp),
        color = Color(0xFF1A1A1A)
    )
}

@Composable
private fun ShippingOption(
    id: String,
    selectedId: String,
    title: String,
    time: String,
    price: String,
    onSelected: (String) -> Unit
) {
    val selected = id == selectedId
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFFF0F7FF) else Color(0xFFF7F8FB))
            .border(1.dp, if (selected) Color(0xFF0056FF) else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable { onSelected(id) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) Color(0xFF0056FF) else Color.LightGray,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(time, color = if (selected) Color(0xFF0056FF) else Color.Gray, fontSize = 12.sp)
        }
        Text(price, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (price == "FREE") Color(0xFF00B248) else Color.Black)
    }
}

private fun formatPrice(price: Double): String {
    return "%.0f".format(price).reversed().chunked(3).joinToString(".").reversed()
}
