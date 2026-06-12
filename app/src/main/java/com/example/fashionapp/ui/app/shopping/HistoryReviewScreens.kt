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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import kotlinx.coroutines.delay

private const val ONGOING_DURATION_MILLIS = 10 * 60 * 1000L
private const val REVIEW_EDIT_WINDOW_MILLIS = 7L * 24 * 60 * 60 * 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    initialTab: String = "Ongoing",
    viewModel: ShopViewModel = viewModel()
) {
    val settings = LocalAppSettings.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val orders = uiState.orders
    var selectedTab by remember(initialTab) {
        mutableStateOf(if (initialTab == "History") "History" else "Ongoing")
    }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var pendingCancelOrderId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowMillis = System.currentTimeMillis()
        }
    }

    LaunchedEffect(uiState.orderError) {
        uiState.orderError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeOrderError()
        }
    }

    val displayedOrders = remember(orders, selectedTab, nowMillis) {
        orders.filter { order ->
            // Đơn đã giao (Delivered) → luôn thuộc History
            // Đơn đang xử lý (Ongoing) + chưa quá timeout → thuộc Ongoing
            // Còn lại → History
            val isOngoing = order.status == "Ongoing" &&
                order.placedAtMillis > 0L &&
                nowMillis - order.placedAtMillis < ONGOING_DURATION_MILLIS

            if (selectedTab == "Ongoing") isOngoing else !isOngoing
        }
    }

    pendingCancelOrderId?.let { orderId ->
        AlertDialog(
            onDismissRequest = { pendingCancelOrderId = null },
            title = { Text(settings.t("Cancel order?", "Hủy đơn hàng?")) },
            text = { Text(settings.t("This will cancel the order, restore product stock, and refund paid orders.", "Hành động này sẽ hủy đơn hàng, khôi phục tồn kho sản phẩm và hoàn tiền cho các đơn hàng đã thanh toán.")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingCancelOrderId = null
                        viewModel.cancelOrder(context, orderId)
                    }
                ) {
                    Text(settings.t("Cancel order", "Hủy đơn hàng"), color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancelOrderId = null }) {
                    Text(settings.t("Keep order", "Giữ đơn hàng"))
                }
            },
            containerColor = Color.White
        )
    }

    Scaffold(
        topBar = {
            FashionTopBar(
                title = if (selectedTab == "Ongoing") settings.t("Ongoing Orders", "Đơn hàng đang xử lý") else settings.t("Order History", "Lịch sử đơn hàng"),
                onBackClick = { navController.popBackStack() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            if (selectedTab == "Ongoing") settings.t("No ongoing orders", "Không có đơn hàng đang xử lý") else settings.t("No order history", "Không có lịch sử đơn hàng"),
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
                            error = painterResource(R.drawable.ic_shopping)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(order.product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                settings.t("Order #", "Đơn hàng #") + order.id.takeLast(6),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(order.orderDate, color = Color(0xFF0057FF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        if (selectedTab == "History") {
                            val review = uiState.reviewsByOrderId[order.id]
                            val canEditReview = review?.let {
                                canEditReview(it, nowMillis)
                            } ?: true
                            val reviewActionText = when {
                                review == null -> settings.t("Review", "Đánh giá")
                                canEditReview -> settings.t("Edit Review", "Sửa đánh giá")
                                else -> settings.t("Reviewed", "Đã đánh giá")
                            }
                            val reviewStatusText = when {
                                review == null -> settings.t("Not reviewed", "Chưa đánh giá")
                                canEditReview -> settings.t("Reviewed - 1 edit left", "Đã đánh giá - Còn 1 lần sửa")
                                review.editCount >= 1 -> settings.t("Reviewed - edit used", "Đã đánh giá - Đã dùng lượt sửa")
                                else -> settings.t("Reviewed - edit expired", "Đã đánh giá - Hết hạn sửa")
                            }
                            val reviewActionColor = if (review == null || canEditReview) {
                                Color(0xFF0057FF)
                            } else {
                                Color.Gray
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Button(
                                    onClick = {
                                        navController.navigate(Screen.Review.createRoute(order.id)) {
                                            launchSingleTop = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE5EDFF),
                                        contentColor = Color(0xFF0057FF),
                                        disabledContainerColor = Color(0xFFF1F1F1),
                                        disabledContentColor = Color.Gray
                                     ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(34.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    enabled = canEditReview
                                ) {
                                    Text(
                                        reviewActionText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    reviewStatusText,
                                    color = reviewActionColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(settings.t("Ongoing", "Đang xử lý"), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(6.dp))
                                Button(
                                    onClick = { pendingCancelOrderId = order.id },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFFEBEE),
                                        contentColor = Color(0xFFE53935)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(settings.t("Cancel", "Hủy"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
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
    val settings = LocalAppSettings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 16.dp)
            .background(Color(0xFFF5F5F5), RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SegmentPill(settings.t("Cart", "Giỏ hàng"), selected == "Cart", onClick = onCart, modifier = Modifier.weight(1f))
        SegmentPill(settings.t("Ongoing", "Đang xử lý"), selected == "Ongoing", onClick = onOngoing, modifier = Modifier.weight(1f))
        SegmentPill(settings.t("History", "Lịch sử"), selected == "History", onClick = onHistory, modifier = Modifier.weight(1f))
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
    val settings = LocalAppSettings.current
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val orderToReview = uiState.orders.firstOrNull { it.id == orderId }
    val productToReview = orderToReview?.product
    val existingReview = uiState.reviewsByOrderId[orderId]
    val nowMillis = remember { System.currentTimeMillis() }
    val canEditReview = existingReview?.let {
        canEditReview(it, nowMillis)
    } ?: true
    val isEditingReview = existingReview != null && canEditReview
    val isLockedReview = existingReview != null && !canEditReview
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.reviewError) {
        uiState.reviewError?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeReviewError()
        }
    }

    LaunchedEffect(existingReview?.id, existingReview?.rating, existingReview?.comment) {
        existingReview?.let {
            rating = it.rating.coerceIn(1, 5)
            comment = it.comment
        }
    }

    Scaffold(
        topBar = {
            FashionTopBar(
                title = when {
                    existingReview == null -> settings.t("Write a Review", "Viết đánh giá")
                    isEditingReview -> settings.t("Edit Review", "Sửa đánh giá")
                    else -> settings.t("Review Submitted", "Đã gửi đánh giá")
                },
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
                    Text(
                        when {
                            existingReview == null -> settings.t("Review Product", "Đánh giá sản phẩm")
                            isEditingReview -> settings.t("Edit Product Review", "Sửa đánh giá sản phẩm")
                            else -> settings.t("Your Product Review", "Đánh giá của bạn")
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    if (productToReview != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = productToReview.imageUrl.ifBlank { null },
                                contentDescription = null,
                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                                error = painterResource(R.drawable.ic_shopping)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(productToReview.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(settings.t("Order #", "Đơn hàng #") + orderId.takeLast(6), color = Color.Gray, fontSize = 12.sp)
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
                                    .clickable(enabled = canEditReview) { rating = starIndex }
                            )
                        }
                    }

                    if (isEditingReview) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            settings.t("You can edit this review once within 7 days.", "Bạn có thể chỉnh sửa đánh giá này một lần trong vòng 7 ngày."),
                            color = Color(0xFF0057FF),
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (isLockedReview) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            when {
                                existingReview?.editCount ?: 0 >= 1 -> settings.t("Review can only be edited once.", "Chỉ có thể chỉnh sửa đánh giá một lần.")
                                else -> settings.t("Review can only be edited within 7 days.", "Chỉ có thể chỉnh sửa đánh giá trong vòng 7 ngày.")
                            },
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    
                    TextField(
                        value = comment,
                        onValueChange = { comment = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text(settings.t("Share your thoughts about this product...", "Chia sẻ suy nghĩ của bạn về sản phẩm này..."), color = Color.Gray, fontSize = 14.sp) },
                        enabled = canEditReview,
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
                        enabled = productToReview != null && canEditReview && !isSubmitting && !uiState.isSubmittingReview
                    ) {
                        Text(
                            when {
                                isSubmitting || uiState.isSubmittingReview -> settings.t("Submitting...", "Đang gửi...")
                                existingReview == null -> settings.t("Submit Review", "Gửi đánh giá")
                                isEditingReview -> settings.t("Save Edit", "Lưu thay đổi")
                                else -> settings.t("Reviewed", "Đã đánh giá")
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun canEditReview(
    review: com.example.fashionapp.data.ProductReview,
    nowMillis: Long
): Boolean {
    val reviewCreatedAt = review.createdAtMillis.takeIf { it > 0L } ?: review.updatedAtMillis
    return review.editCount < 1 &&
        reviewCreatedAt > 0L &&
        nowMillis - reviewCreatedAt <= REVIEW_EDIT_WINDOW_MILLIS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDoneScreen(navController: NavController) {
    val settings = LocalAppSettings.current
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
                        Text(settings.t("Success!", "Thành công!"), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(settings.t("Thank you for your feedback!", "Cảm ơn ý kiến đóng góp của bạn!"), color = Color.Gray, fontSize = 14.sp)
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
