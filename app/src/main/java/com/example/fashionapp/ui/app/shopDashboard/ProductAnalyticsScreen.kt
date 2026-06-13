package com.example.fashionapp.ui.app.shopDashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fashionapp.R
import com.example.fashionapp.data.shop.DailyRevenuePoint
import com.example.fashionapp.model.Product
import com.example.fashionapp.model.ProductVariant
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.theme.AppTheme

private val GreenRevenue = Color(0xFF2ECC71)
private val OrangeRating = Color(0xFFFF9F43)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductAnalyticsScreen(
    productId: String,
    navController: NavController,
    viewModel: ProductAnalyticsViewModel = viewModel(
        factory = ProductAnalyticsViewModelFactory(productId)
    )
) {
    val settings = LocalAppSettings.current
    val state by viewModel.uiState.collectAsState()
    
    var showMenu by remember { mutableStateOf(false) }
    var showPriceDialog by remember { mutableStateOf(false) }
    var showStockDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(settings.t("Product Analytics", "Phân tích sản phẩm"), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = settings.t("Back", "Quay lại"))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary) }
            }

            state.product == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text(state.error ?: settings.t("Product not found", "Không tìm thấy sản phẩm"), color = AppTheme.colors.textSecondary) }
            }

            else -> {
                val product = state.product!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ProductHeader(
                            product = product,
                            onViewPage = { navController.navigate(Screen.ProductDetail.createRoute(product.id)) },
                            showMenu = showMenu,
                            onShowMenu = { showMenu = true },
                            onDismissMenu = { showMenu = false },
                            onChangePrice = {
                                showMenu = false
                                showPriceDialog = true
                            },
                            onChangeStock = {
                                showMenu = false
                                showStockDialog = true
                            }
                        )
                    }
                    item { ProductMetricGrid(product = product, revenue = state.revenue) }

                    item {
                        SectionTitle(settings.t("Daily Revenue", "Doanh thu theo ngày"))
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(Modifier.padding(16.dp)) {
                                RevenueBarChart(data = state.dailyRevenue, barColor = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }

                    if (state.dailyRevenue.isNotEmpty()) {
                        item { SectionTitle(settings.t("Daily Details", "Chi tiết theo ngày")) }
                        // hiển thị ngày mới nhất lên đầu
                        items(state.dailyRevenue.reversed(), key = { it.date }) { point ->
                            DailyRevenueRow(point)
                        }
                    }
                }
            }
        }
    }

    if (showPriceDialog && state.product != null) {
        ChangePriceDialog(
            currentPrice = state.product!!.price,
            onDismiss = { showPriceDialog = false },
            onConfirm = { newPrice ->
                viewModel.updatePrice(newPrice)
                showPriceDialog = false
            }
        )
    }

    if (showStockDialog && state.product != null) {
        ChangeStockDialog(
            variants = state.product!!.variants,
            onDismiss = { showStockDialog = false },
            onConfirm = { newVariants ->
                viewModel.updateStock(newVariants)
                showStockDialog = false
            }
        )
    }
}

@Composable
private fun ChangePriceDialog(
    currentPrice: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val settings = LocalAppSettings.current
    var priceStr by remember { mutableStateOf(currentPrice.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(settings.t("Change Price", "Thay đổi giá")) },
        text = {
            OutlinedTextField(
                value = priceStr,
                onValueChange = { if (it.all { c -> c.isDigit() }) priceStr = it },
                label = { Text(settings.t("New Price", "Giá mới")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(priceStr.toDoubleOrNull() ?: currentPrice) }) {
                Text(settings.t("Confirm", "Xác nhận"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(settings.t("Cancel", "Hủy"))
            }
        }
    )
}

@Composable
private fun ChangeStockDialog(
    variants: List<ProductVariant>,
    onDismiss: () -> Unit,
    onConfirm: (List<ProductVariant>) -> Unit
) {
    val settings = LocalAppSettings.current
    var editedVariants by remember { mutableStateOf(variants) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(settings.t("Change Stock", "Thay đổi kho")) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                editedVariants.forEachIndexed { index, variant ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${variant.size} - ${variant.color}",
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp
                        )
                        OutlinedTextField(
                            value = variant.stock.toString(),
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() }) {
                                    val newStock = newValue.toIntOrNull() ?: 0
                                    editedVariants = editedVariants.toMutableList().apply {
                                        this[index] = variant.copy(stock = newStock)
                                    }
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(editedVariants) }) {
                Text(settings.t("Confirm", "Xác nhận"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(settings.t("Cancel", "Hủy"))
            }
        }
    )
}

@Composable
private fun ProductHeader(
    product: Product,
    onViewPage: () -> Unit,
    showMenu: Boolean,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onChangePrice: () -> Unit,
    onChangeStock: () -> Unit
) {
    val settings = LocalAppSettings.current
    Column {
        Row(verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = product.imageUrl.ifBlank { null },
                contentDescription = product.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_shopping)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(6.dp))
                Text("₫${formatMoney(product.price)}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.secondary)
            }

            Box {
                IconButton(onClick = onShowMenu) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = AppTheme.colors.textSecondary
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = onDismissMenu
                ) {
                    DropdownMenuItem(
                        text = { Text(settings.t("Change Price", "Thay đổi giá")) },
                        onClick = onChangePrice
                    )
                    DropdownMenuItem(
                        text = { Text(settings.t("Change Stock", "Thay đổi kho")) },
                        onClick = onChangeStock
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            settings.t("View product page", "Xem trang sản phẩm"),
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onViewPage() }
        )
    }
}

@Composable
private fun ProductMetricGrid(product: Product, revenue: Double) {
    val settings = LocalAppSettings.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnalyticMetricCard(
                modifier = Modifier.weight(1f),
                label = settings.t("Total Revenue", "Tổng doanh thu"),
                value = "₫${formatMoney(revenue)}",
                color = GreenRevenue
            )
            AnalyticMetricCard(
                modifier = Modifier.weight(1f),
                label = settings.t("Total Sold", "Đã bán"),
                value = product.soldCount.toString(),
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnalyticMetricCard(
                modifier = Modifier.weight(1f),
                label = settings.t("Rating", "Đánh giá"),
                value = product.rating.toString(),
                color = OrangeRating
            )
            AnalyticMetricCard(
                modifier = Modifier.weight(1f),
                label = settings.t("Stock", "Tồn kho"),
                value = product.stock.toString(),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AnalyticMetricCard(
    modifier: Modifier,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(label, fontSize = 13.sp, color = AppTheme.colors.textSecondary)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun DailyRevenueRow(point: DailyRevenuePoint) {
    val settings = LocalAppSettings.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(point.date, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "${point.orderCount} " + settings.t("orders", "đơn hàng"),
                    fontSize = 12.sp,
                    color = AppTheme.colors.textSecondary
                )
            }
            Text(
                "₫${formatMoney(point.revenue)}",
                fontWeight = FontWeight.Bold,
                color = GreenRevenue,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
}
