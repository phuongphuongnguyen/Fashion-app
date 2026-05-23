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
import com.example.fashionapp.ui.app.shopping.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    navController: NavController,
    viewModel: ShoppingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val items = uiState.cartItems
    Scaffold(
        topBar = {
            FashionTopBar(
                title = "Payment",
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
                        Text("Total", color = Color.Gray, fontSize = 14.sp)
                        Text(
                            "$${"%.2f".format(items.sumOf { it.totalPrice })}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Pay", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
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
                        Text("Bonnie Green", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("De Nang Bul, Fashion Store, Da Nang", color = Color.Gray, fontSize = 13.sp)
                    }
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE5EDFF)).clickable { }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF0056FF), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Contact Information
            item {
                SectionHeader("Contact Information")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF7F8FB), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("+84 987 654 321", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("bonnie@email.com", color = Color.Gray, fontSize = 13.sp)
                    }
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE5EDFF)).clickable { }, contentAlignment = Alignment.Center) {
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
                    Text("Items", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5EDFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("2", color = Color(0xFF0056FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                .background(Color(0xFFF1F1F1)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.product.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Lorem ipsum dolor sit amet", fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                        }
                        Text("$${"%.2f".format(item.totalPrice)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Shipping Options
            item {
                SectionHeader("Shipping Options")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShippingOption("Standard", "2-3 days", "FREE", true)
                    ShippingOption("Express", "1 day", "$10.00", false)
                }
            }

            // Payment Method
            item {
                SectionHeader("Payment Method")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp, 30.dp)
                            .background(Color(0xFFE5EDFF), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Card", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0056FF))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("**** **** **** 1234", modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE5EDFF)).clickable { }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF0056FF), modifier = Modifier.size(16.dp))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun ShippingOption(title: String, time: String, price: String, selected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFFF0F7FF) else Color(0xFFF7F8FB))
            .border(1.dp, if (selected) Color(0xFF0056FF) else Color.Transparent, RoundedCornerShape(12.dp))
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
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(time, color = Color(0xFF0056FF), fontSize = 12.sp)
        }
        Text(price, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
