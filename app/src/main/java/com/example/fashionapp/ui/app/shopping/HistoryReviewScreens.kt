package com.example.fashionapp.ui.app.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fashionapp.R
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.components.FashionTopBar
import kotlinx.coroutines.delay

private const val ONGOING_DURATION_MILLIS = 10 * 60 * 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    initialTab: String = "Ongoing",
    viewModel: ShopViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val orders = uiState.orders
    var selectedTab by remember(initialTab) {
        mutableStateOf(if (initialTab == "History") "History" else "Ongoing")
    }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowMillis = System.currentTimeMillis()
        }
    }

    val displayedOrders = remember(orders, selectedTab, nowMillis) {
        orders.filter { order ->
            val isOngoing = order.status == "Ongoing" &&
                order.placedAtMillis > 0L &&
                nowMillis - order.placedAtMillis < ONGOING_DURATION_MILLIS

            if (selectedTab == "Ongoing") isOngoing else !isOngoing
        }
    }

    Scaffold(
        topBar = {
            FashionTopBar(
                title = if (selectedTab == "Ongoing") "Ongoing Orders" else "Order History",
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Color.White
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
                    selected = selectedTab,
                    onCart = {
                        navController.navigate(Screen.Cart.route) {
                            popUpTo(Screen.Cart.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onOngoing = { selectedTab = "Ongoing" },
                    onHistory = { selectedTab = "History" }
                )
            }

            if (displayedOrders.isEmpty() && !uiState.isLoadingOrders) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (selectedTab == "Ongoing") "No ongoing orders" else "No order history",
                            color = Color.Gray
                        )
                    }
                }
            } else {
                items(displayedOrders, key = { it.id }) { order ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF9F9F9))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = order.product.imageUrl.ifBlank { null },
                            contentDescription = order.product.name,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F1F1)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.ic_launcher_foreground)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(order.product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Order #${order.id.takeLast(6)}",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(order.orderDate, color = Color(0xFF0057FF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        if (selectedTab == "History") {
                            Button(
                                onClick = {
                                    navController.navigate(Screen.Review.createRoute(order.id)) {
                                        launchSingleTop = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE5EDFF),
                                    contentColor = Color(0xFF0057FF)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Review", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("Ongoing", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
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
    onOngoing: () -> Unit,
    onHistory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 16.dp)
            .background(Color(0xFFF5F5F5), RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SegmentPill("Cart", selected == "Cart", onClick = onCart, modifier = Modifier.weight(1f))
        SegmentPill("Ongoing", selected == "Ongoing", onClick = onOngoing, modifier = Modifier.weight(1f))
        SegmentPill("History", selected == "History", onClick = onHistory, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SegmentPill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Color.Black else Color.Gray,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    navController: NavController,
    orderId: String,
    viewModel: ShopViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val orderToReview = uiState.orders.firstOrNull { it.id == orderId }
    val productToReview = orderToReview?.product
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            FashionTopBar(
                title = "Write a Review",
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Color(0xFFF9F9F9)
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Review Product", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(16.dp))
                    
                    if (productToReview != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = productToReview.imageUrl.ifBlank { null },
                                contentDescription = null,
                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                                error = painterResource(R.drawable.ic_launcher_foreground)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(productToReview.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Order #${orderId.takeLast(6)}", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(5) { index ->
                            val starIndex = index + 1
                            Icon(
                                imageVector = if (starIndex <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = null,
                                tint = if (starIndex <= rating) Color(0xFFFFB300) else Color.Gray,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { rating = starIndex }
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    
                    TextField(
                        value = comment,
                        onValueChange = { comment = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text("Share your thoughts about this product...", color = Color.Gray, fontSize = 14.sp) },
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F5F5),
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            isSubmitting = true
                            viewModel.submitReview(
                                orderId = orderId,
                                rating = rating,
                                comment = comment
                            ) { success ->
                                isSubmitting = false
                                if (success) {
                                    navController.navigate(Screen.ReviewDone.route) {
                                        popUpTo(Screen.Review.route) { inclusive = true }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0057FF)),
                        shape = RoundedCornerShape(25.dp),
                        enabled = productToReview != null && !isSubmitting
                    ) {
                        Text(if (isSubmitting) "Submitting..." else "Submit Review", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDoneScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        delay(1500)
        navController.navigate(Screen.History.createRoute("History")) {
            popUpTo(Screen.History.route) { inclusive = true }
        }
    }

    Scaffold(containerColor = Color(0xAA000000)) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.padding(32.dp).fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    modifier = Modifier.padding(top = 28.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 32.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Success!", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Thank you for your feedback!", color = Color.Gray, fontSize = 14.sp)
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(5) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(30.dp))
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White).padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF0057FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}
