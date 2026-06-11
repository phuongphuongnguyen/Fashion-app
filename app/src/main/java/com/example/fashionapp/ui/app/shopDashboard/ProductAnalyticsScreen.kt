package com.example.fashionapp.ui.app.shopDashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fashionapp.R
import com.example.fashionapp.data.shop.DailyRevenuePoint
import com.example.fashionapp.model.Product
import com.example.fashionapp.navigation.Screen

// ── Palette ──
private val PrimaryBlue = Color(0xFF3669C9)
private val TextDark = Color(0xFF1A1A1A)
private val TextGray = Color(0xFF888888)
private val CardBg = Color(0xFFF7F8FA)
private val GreenRevenue = Color(0xFF00A152)
private val OrangeRating = Color(0xFFF5A623)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductAnalyticsScreen(
    productId: String,
    navController: NavController,
    viewModel: ProductAnalyticsViewModel = viewModel(
        factory = ProductAnalyticsViewModelFactory(productId)
    )
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Phân tích sản phẩm", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = PrimaryBlue) }
            }

            state.product == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text(state.error ?: "Không tìm thấy sản phẩm", color = TextGray) }
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
                            onViewPage = { navController.navigate(Screen.ProductDetail.createRoute(product.id)) }
                        )
                    }
                    item { ProductMetricGrid(product = product, revenue = state.revenue) }

                    item {
                        SectionTitle("Doanh thu theo ngày")
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CardBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(Modifier.padding(16.dp)) {
                                RevenueBarChart(data = state.dailyRevenue, barColor = PrimaryBlue)
                            }
                        }
                    }

                    if (state.dailyRevenue.isNotEmpty()) {
                        item { SectionTitle("Chi tiết theo ngày") }
                        // hiển thị ngày mới nhất lên đầu
                        items(state.dailyRevenue.reversed(), key = { it.date }) { point ->
                            DailyRevenueRow(point)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductHeader(product: Product, onViewPage: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = product.imageUrl.ifBlank { null },
                contentDescription = product.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFEEEEEE)),
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
                    color = TextDark
                )
                Spacer(Modifier.height(6.dp))
                Text("₫${formatMoney(product.price)}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = PrimaryBlue)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Xem trang sản phẩm",
            color = PrimaryBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onViewPage() }
        )
    }
}

// ── Lưới 2x2 chỉ số sản phẩm ──
@Composable
private fun ProductMetricGrid(product: Product, revenue: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnalyticMetricCard(Modifier.weight(1f), "Doanh thu", "₫${formatMoney(revenue)}", GreenRevenue)
            AnalyticMetricCard(Modifier.weight(1f), "Đã bán", product.soldCount.toString(), PrimaryBlue)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnalyticMetricCard(Modifier.weight(1f), "Đánh giá", "%.1f (${product.reviewCount})".format(product.rating), OrangeRating)
            AnalyticMetricCard(Modifier.weight(1f), "Tồn kho", product.stock.toString(), TextDark)
        }
    }
}

@Composable
private fun AnalyticMetricCard(modifier: Modifier, label: String, value: String, valueColor: Color) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Text(
            value,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = TextGray)
    }
}

@Composable
private fun DailyRevenueRow(point: DailyRevenuePoint) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            fullDate(point.date),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextDark,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text("₫${formatMoney(point.revenue)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GreenRevenue)
            Spacer(Modifier.height(2.dp))
            Text("${point.orderCount} đơn • ${point.soldCount} sp", fontSize = 11.sp, color = TextGray)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
}