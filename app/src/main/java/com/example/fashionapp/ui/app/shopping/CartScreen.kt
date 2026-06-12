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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.theme.AppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    viewModel: ShopViewModel = viewModel()
) {
    val settings = LocalAppSettings.current
    val uiState by viewModel.uiState.collectAsState()
    val items = uiState.cartItems
    val existingItemIds = items.map { it.id }.toSet()
    var selectedItemIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var hasInitializedSelection by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refreshShoppingData()
    }

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
                title = settings.t("Shopping Cart", "Giỏ hàng"),
                onBackClick = { navController.popBackStack() },
                actions = {
                    if (items.isNotEmpty()) {
                        TextButton(onClick = { isEditing = !isEditing }) {
                            Text(
                                if (isEditing) settings.t("Done", "Xong") else settings.t("Edit", "Sửa"),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(shadowElevation = 16.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                     Column {
                        Text(settings.t("Total", "Tổng thanh toán"), color = AppTheme.colors.textSecondary, fontSize = 12.sp)
                        Text(settings.t("${selectedItems.size} item(s) selected", "Đã chọn ${selectedItems.size} sản phẩm"), color = AppTheme.colors.textSecondary, fontSize = 11.sp)
                        Text("₫${formatPrice(total)}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Button(
                        onClick = {
                            if (isEditing) {
                                selectedItemIds.forEach { itemId ->
                                    viewModel.updateCartQuantity(itemId, 0)
                                }
                                selectedItemIds = emptySet()
                                isEditing = false
                            } else {
                                navController.navigate(Screen.Payment.createRoute(selectedItemIds)) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEditing) AppTheme.colors.danger else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .width(140.dp)
                            .height(48.dp),
                        enabled = selectedItems.isNotEmpty()
                    ) {
                        Text(
                            if (isEditing) settings.t("Delete", "Xóa") else settings.t("Checkout", "Thanh toán"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoadingCart) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyCartState(
                    errorMessage = uiState.cartError,
                    onRetry = viewModel::refreshShoppingData
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.cartError?.let { message ->
                    item {
                        CartErrorBanner(
                            message = message,
                            onRetry = viewModel::refreshShoppingData
                        )
                    }
                }
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
                            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            settings.t("Select all", "Chọn tất cả"),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${selectedItems.size}/${items.size}",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                items(items, key = { it.id }) { item ->
                    SwipeToDeleteCartItem(
                        onDelete = {
                            viewModel.updateCartQuantity(item.id, 0)
                            selectedItemIds = selectedItemIds - item.id
                             scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = settings.t("Item removed", "Đã xóa sản phẩm"),
                                    actionLabel = settings.t("Undo", "Hoàn tác")
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreCartItem(item)
                                }
                            }
                        }
                    ) {
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
}

@Composable
private fun SwipeToDeleteCartItem(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val settings = LocalAppSettings.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppTheme.colors.danger)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(settings.t("Delete", "Xóa"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete item",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        content = {
            content()
        }
    )
}

@Composable
private fun EmptyCartState(
    errorMessage: String?,
    onRetry: () -> Unit
) {
    val settings = LocalAppSettings.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Text(
            errorMessage ?: settings.t("Your cart is empty", "Giỏ hàng trống"),
            color = if (errorMessage == null) AppTheme.colors.textSecondary else AppTheme.colors.danger,
            fontSize = 14.sp
        )
        if (errorMessage != null) {
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(settings.t("Retry", "Thử lại"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CartErrorBanner(
    message: String,
    onRetry: () -> Unit
) {
    val settings = LocalAppSettings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF1F1))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            message,
            color = Color(0xFFB3261E),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRetry) {
            Text(settings.t("Retry", "Thử lại"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = onSelectedChange,
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.size(34.dp)
        )
        Box(modifier = Modifier.size(70.dp)) {
            AsyncImage(
                model = item.product.imageUrl.ifBlank { null },
                contentDescription = item.product.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
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
                color = AppTheme.colors.textSecondary,
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    QuantityButton(
                        onClick = { onUpdateQuantity(item.quantity - 1) },
                        enabled = item.quantity > 1
                    ) {
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
private fun QuantityButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private fun formatPrice(price: Double): String {
    return "%.0f".format(price).reversed().chunked(3).joinToString(".").reversed()
}
