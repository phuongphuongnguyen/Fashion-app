package com.example.fashionapp.ui.app.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
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
import com.example.fashionapp.data.MockData
import com.example.fashionapp.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            "$${"%.2f".format(MockData.orders.sumOf { it.totalPrice })}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Button(
                        onClick = {
                            navController.navigate(Screen.History.route) {
                                popUpTo(Screen.Payment.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text("Pay")
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bonnie Green", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("De Nang Bul, Fashion Store, Da Nang", color = Color.Gray, fontSize = 13.sp)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF0056FF), modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Contact Information
            item {
                SectionHeader("Contact Information")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("+84 987 654 321", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("bonnie@email.com", color = Color.Gray, fontSize = 13.sp)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF0056FF), modifier = Modifier.size(20.dp))
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
                MockData.orders.take(2).forEach { order ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = order.product.imageUrl.ifEmpty { "https://via.placeholder.com/50" },
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(order.product.name, fontSize = 13.sp)
                        }
                        Text("$${"%.2f".format(order.totalPrice)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                            .size(40.dp, 24.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Card", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("**** **** **** 1234", modifier = Modifier.weight(1f), fontSize = 13.sp)
                    RadioButton(selected = true, onClick = {})
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
            .background(if (selected) Color(0xFFF0F7FF) else Color.Transparent, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = {})
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(time, color = Color.Gray, fontSize = 12.sp)
        }
        Text(price, fontWeight = FontWeight.Bold)
    }
}
