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
import com.example.fashionapp.data.user.UserSession
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.components.FashionTopBar
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.theme.AppTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth

// Màn hình thanh toán và đặt hàng, xử lý thông tin nhận hàng, lựa chọn phương thức thanh toán (Visa/Momo) và tạo đơn hàng trong hệ thống
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    navController: NavController,
    selectedCartItemIds: Set<String> = emptySet(),
    viewModel: ShopViewModel = viewModel()
) {
    val settings = LocalAppSettings.current
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by UserSession.currentUser.collectAsState()
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
    val checkoutUser = uiState.currentUserProfile ?: currentUser
    val recipientName = checkoutUser?.name?.takeIf { it.isNotBlank() } ?: settings.t("Customer", "Khách hàng")
    val recipientPhone = checkoutUser?.phoneNumber.orEmpty()
    val shippingAddress = checkoutUser?.address.orEmpty()
    val hasShippingAddress = shippingAddress.isNotBlank()
    val selectedItemsMissing = selectedCartItemIds.isNotEmpty() &&
        !uiState.isLoadingCart &&
        checkoutItems.size < selectedCartItemIds.size
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE) }
    val savedCardNumber = prefs.getString("pay_card", "") ?: ""
    val cleanCardNumber = savedCardNumber.filter { it.isDigit() }
    val visaSubtitle = if (cleanCardNumber.length >= 4) {
        "**** **** **** ${cleanCardNumber.takeLast(4)}"
    } else {
        "**** **** **** 4567"
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            FashionTopBar(
                title = settings.t("Checkout", "Thanh toán"),
                onBackClick = { navController.popBackStack() }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 16.dp, color = MaterialTheme.colorScheme.surface) {
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
                        Text(settings.t("Order Total", "Tổng thanh toán"), color = AppTheme.colors.textSecondary, fontSize = 14.sp)
                        Text(
                            "₫${formatPrice(total)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
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
                                val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                                viewModel.placeOrderFromCart(
                                    cartItems = checkoutItems,
                                    paymentMethod = selectedPaymentMethod,
                                    paymentStatus = "PAID",
                                    shippingMethod = selectedShippingMethod,
                                    shippingFee = shippingFee,
                                    shippingAddress = shippingAddress
                                ) { orderId ->
                                    if (orderId != null) {
                                        OrderTrackingScheduler.showPaymentNotification(context, total.toLong())
                                        OrderTrackingScheduler.scheduleTracking(
                                            context = context,
                                            orderId = orderId,
                                            userId = uid,
                                            amount = total.toLong()
                                        )
                                        navController.navigate(Screen.History.createRoute("Ongoing")) {
                                            popUpTo(Screen.Payment.route) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = checkoutItems.isNotEmpty() &&
                            hasShippingAddress &&
                            !uiState.isLoadingCart &&
                            !uiState.isPlacingOrder
                    ) {
                        Text(
                            if (uiState.isPlacingOrder) settings.t("Placing Order...", "Đang đặt hàng...") else settings.t("Place Order", "Đặt hàng"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            if (uiState.isLoadingCart) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                return@LazyColumn
            }

            if (checkoutItems.isEmpty()) {
                item {
                    CheckoutMessage(
                        message = if (selectedCartItemIds.isEmpty()) {
                            settings.t("Your cart is empty", "Giỏ hàng trống")
                        } else {
                            settings.t("Selected items are no longer available", "Sản phẩm được chọn hiện không còn khả dụng")
                        },
                        onBackToCart = { navController.popBackStack() }
                    )
                }
                return@LazyColumn
            }

            if (selectedItemsMissing) {
                item {
                    PaymentMessage(settings.t("Some selected items are no longer available.", "Một số sản phẩm được chọn không còn khả dụng."))
                }
            }

            uiState.orderError?.let { message ->
                item {
                    PaymentMessage(message)
                }
            }

            // Shipping Address
            item {
                SectionHeader(settings.t("Shipping Address", "Địa chỉ giao hàng"))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(recipientName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        if (recipientPhone.isNotBlank()) {
                            Text(recipientPhone, color = AppTheme.colors.textSecondary, fontSize = 12.sp)
                            Spacer(Modifier.height(2.dp))
                        }
                        Text(
                            shippingAddress.ifBlank { settings.t("Add a shipping address in your profile", "Thêm địa chỉ giao hàng trong trang cá nhân") },
                            color = if (hasShippingAddress) AppTheme.colors.textSecondary else Color(0xFFB3261E),
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                navController.navigate(Screen.Settings.createRoute("shipping"))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Items Summary
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(settings.t("Items", "Sản phẩm"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${checkoutItems.size}", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.ic_shopping)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.product.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text("${item.color} | ${item.size} x ${item.quantity}", fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                        }
                        Text("₫${formatPrice(item.totalPrice)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Shipping Options
            item {
                SectionHeader(settings.t("Shipping Options", "Tùy chọn giao hàng"))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShippingOption(
                        id = "standard",
                        selectedId = selectedShippingMethod,
                        title = settings.t("Standard Delivery", "Giao hàng tiêu chuẩn"),
                        time = settings.t("Arrival in 3-5 days", "Nhận hàng sau 3-5 ngày"),
                        price = settings.t("FREE", "Miễn phí"),
                        onSelected = { selectedShippingMethod = it }
                    )
                    ShippingOption(
                        id = "express",
                        selectedId = selectedShippingMethod,
                        title = settings.t("Express Delivery", "Giao hàng hỏa tốc"),
                        time = settings.t("Arrival in 1-2 days", "Nhận hàng sau 1-2 ngày"),
                        price = "₫50.000",
                        onSelected = { selectedShippingMethod = it }
                    )
                }
            }

            // Payment Method
            item {
                SectionHeader(settings.t("Payment Method", "Phương thức thanh toán"))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PaymentMethodOption(
                        id = "visa",
                        selectedId = selectedPaymentMethod,
                        badgeText = "VISA",
                        badgeColor = MaterialTheme.colorScheme.surfaceVariant,
                        badgeTextColor = MaterialTheme.colorScheme.primary,
                        title = "Visa",
                        subtitle = visaSubtitle,
                        onSelected = { selectedPaymentMethod = it }
                    )
                    PaymentMethodOption(
                        id = "momo",
                        selectedId = selectedPaymentMethod,
                        badgeText = "MoMo",
                        badgeColor = Color(0xFFA50064),
                        badgeTextColor = Color.White,
                        title = "MoMo",
                        subtitle = settings.t("Pay with MoMo e-wallet", "Thanh toán bằng ví điện tử MoMo"),
                        onSelected = { selectedPaymentMethod = it }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun CheckoutMessage(
    message: String,
    onBackToCart: () -> Unit
) {
    val settings = LocalAppSettings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(message, color = AppTheme.colors.textSecondary, fontSize = 14.sp)
        Button(
            onClick = onBackToCart,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(settings.t("Back to Cart", "Quay lại giỏ hàng"), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PaymentMessage(message: String) {
    Text(
        message,
        color = Color(0xFFB3261E),
        fontSize = 13.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF1F1), RoundedCornerShape(12.dp))
            .padding(12.dp)
    )
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
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable { onSelected(id) }
            .padding(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(52.dp, 32.dp),
            shape = RoundedCornerShape(6.dp),
            color = badgeColor,
            border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Black, color = badgeTextColor)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 12.sp, color = AppTheme.colors.textSecondary)
        }
        Icon(
            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else AppTheme.colors.textSecondary,
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
        color = MaterialTheme.colorScheme.onBackground
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
    val settings = LocalAppSettings.current
    val selected = id == selectedId
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable { onSelected(id) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else AppTheme.colors.textSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(time, color = if (selected) MaterialTheme.colorScheme.primary else AppTheme.colors.textSecondary, fontSize = 12.sp)
        }
        val isFree = price == settings.t("FREE", "Miễn phí")
        Text(price, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isFree) AppTheme.colors.success else MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatPrice(price: Double): String {
    return "%.0f".format(price).reversed().chunked(3).joinToString(".").reversed()
}
