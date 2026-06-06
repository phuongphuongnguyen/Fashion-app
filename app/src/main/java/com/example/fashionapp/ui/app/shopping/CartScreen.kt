package com.example.fashionapp.ui.app.shopping

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.fashionapp.data.CartItem
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.components.FashionTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    viewModel: ShopViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val items = uiState.cartItems
    val existingItemIds = items.map { it.id }.toSet()
    var selectedItemIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var hasInitializedSelection by remember { mutableStateOf(false) }

    LaunchedEffect(existingItemIds) {
        selectedItemIds = if (existingItemIds.isEmpty()) {
            hasInitializedSelection = false
            emptySet()
        } else if (!hasInitializedSelection) {
            hasInitializedSelection = true
            existingItemIds
        } else {
            selectedItemIds.intersect(existingItemIds)
        }
    }

    val selectedItems = items.filter { it.id in selectedItemIds }
    val total = selectedItems.sumOf { it.totalPrice }
    val allSelected = items.isNotEmpty() && selectedItemIds.containsAll(existingItemIds)

    Scaffold(
        topBar = {
            FashionTopBar(
                title = "Shopping Cart",
                onBackClick = { navController.popBackStack() },
                actions = {
                    TextButton(onClick = { /* Handle Edit */ }) {
                        Text("Edit", color = Color(0xFF0057FF), fontWeight = FontWeight.Medium)
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 16.dp, color = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total", color = Color.Gray, fontSize = 12.sp)
                        Text("${selectedItems.size} item(s) selected", color = Color.Gray, fontSize = 11.sp)
                        Text("₫${formatPrice(total)}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Button(
                        onClick = {
                            navController.navigate(Screen.Payment.createRoute(selectedItemIds)) {
                                launchSingleTop = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0057FF)),
                        modifier = Modifier
                            .width(140.dp)
                            .height(48.dp),
                        enabled = selectedItems.isNotEmpty()
                    ) {
                        Text("Checkout", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = Color.White
    ) { padding ->
        if (items.isEmpty() && !uiState.isLoadingCart) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Your cart is empty", color = Color.Gray)
            }
        } else {
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
                        onOngoing = {
                            navController.navigate(Screen.History.createRoute("Ongoing")) {
                                launchSingleTop = true
                            }
                        },
                        onHistory = {
                            navController.navigate(Screen.History.createRoute("History")) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF7F8FB))
                            .clickable {
                                selectedItemIds = if (allSelected) emptySet() else existingItemIds
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { checked ->
                                selectedItemIds = if (checked) existingItemIds else emptySet()
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0057FF)),
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            "Select all",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${selectedItems.size}/${items.size}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
                items(items, key = { it.id }) { item ->
                    CartItemRow(
                        item = item,
                        selected = item.id in selectedItemIds,
                        onSelectedChange = { checked ->
                            selectedItemIds = if (checked) {
                                selectedItemIds + item.id
                            } else {
                                selectedItemIds - item.id
                            }
                        },
                        onUpdateQuantity = { newQty ->
                            viewModel.updateCartQuantity(item.id, newQty)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onUpdateQuantity: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = onSelectedChange,
            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0057FF)),
            modifier = Modifier.size(34.dp)
        )
        Box(modifier = Modifier.size(70.dp)) {
            AsyncImage(
                model = item.product.imageUrl.ifBlank { null },
                contentDescription = item.product.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF5F5F5)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_shopping)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.product.name,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                fontSize = 14.sp,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                "${item.color} | ${item.size}",
                color = Color.Gray,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "₫${formatPrice(item.product.price)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    QuantityButton(onClick = { onUpdateQuantity(item.quantity - 1) }) {
                        Icon(Icons.Default.Remove, null, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        item.quantity.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    QuantityButton(onClick = { onUpdateQuantity(item.quantity + 1) }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private fun formatPrice(price: Double): String {
    return "%.0f".format(price).reversed().chunked(3).joinToString(".").reversed()
}
