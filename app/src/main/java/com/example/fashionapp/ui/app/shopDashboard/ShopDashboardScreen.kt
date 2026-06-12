package com.example.fashionapp.ui.app.shopDashboard
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.fashionapp.data.shop.ShopProductStat
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.components.FashionTopBar
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.theme.AppTheme

private val PrimaryBlueDark = Color(0xFF274B9A)
private val colorRevenue = Color(0xFF274B9A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDashboardScreen(
    navController: NavController,
    viewModel: ShopDashboardViewModel = viewModel()
) {
    val settings = LocalAppSettings.current
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            FashionTopBar(
                title = settings.t("Shop Management", "Quản lý shop")
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary) }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { RevenueHeroCard(state) }
            item { MetricCardsRow(state) }

            item {
                SectionTitle(settings.t("Revenue (Last 7 Days)", "Doanh thu 7 ngày gần nhất"))
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

            item { SectionTitle(settings.t("Shop Products", "Sản phẩm của shop") + " (${state.products.size})") }

            if (state.products.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(settings.t("The shop has no products yet", "Shop chưa có sản phẩm nào"), color = AppTheme.colors.textSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                items(state.products, key = { it.product.id }) { stat ->
                    ShopProductRow(
                        stat = stat,
                        onClick = {
                            navController.navigate(Screen.ProductAnalytics.createRoute(stat.product.id))
                        }
                    )
                }
            }
        }
    }
}

// tổng doanh thu
@Composable
private fun RevenueHeroCard(state: ShopDashboardUiState) {
    val settings = LocalAppSettings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondary, PrimaryBlueDark)))
            .padding(20.dp)
    ) {
        Text(settings.t("Total Revenue", "Tổng doanh thu"), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "₫${formatMoney(state.stats.revenue)}",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            HeroStat(settings.t("Sold", "Đã bán"), state.stats.soldCount.toString())
            HeroDivider()
            HeroStat(settings.t("Orders", "Đơn hàng"), state.stats.orderCount.toString())
            HeroDivider()
            HeroStat(settings.t("Products", "Sản phẩm"), state.stats.productCount.toString())
        }
    }
}

@Composable
private fun RowScope.HeroStat(label: String, value: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
    }
}

@Composable
private fun HeroDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(34.dp)
            .background(Color.White.copy(alpha = 0.25f))
    )
}

// đánh giá, tỉ lệ phản hồi
@Composable
private fun MetricCardsRow(state: ShopDashboardUiState) {
    val settings = LocalAppSettings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = AppTheme.colors.star, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("%.1f".format(state.stats.rating), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.height(4.dp))
            Text(settings.t("${state.stats.reviewCount} reviews", "${state.stats.reviewCount} đánh giá"), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
        }
        MetricCard(modifier = Modifier.weight(1f)) {
            Text("${state.stats.responseRate}%", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            Text(
                state.stats.responseTime.ifBlank { settings.t("Response Rate", "Tỉ lệ phản hồi") },
                fontSize = 12.sp,
                color = AppTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetricCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)
}

// products
@Composable
private fun ShopProductRow(stat: ShopProductStat, onClick: () -> Unit) {
    val settings = LocalAppSettings.current
    val product = stat.product
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = product.imageUrl.ifBlank { null },
            contentDescription = product.name,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.ic_shopping)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                product.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text("₫${formatMoney(product.price)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(settings.t("Sold ${product.soldCount}", "Đã bán ${product.soldCount}"), fontSize = 11.sp, color = AppTheme.colors.textSecondary)
                Text("   •   ", fontSize = 11.sp, color = AppTheme.colors.textSecondary)
                Icon(Icons.Default.Star, null, tint = AppTheme.colors.star, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(2.dp))
                Text("%.1f".format(product.rating), fontSize = 11.sp, color = AppTheme.colors.textSecondary)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                settings.t("Revenue: ", "Doanh thu: ") + "₫${formatMoney(stat.revenue)}",
                fontSize = 11.sp,
                color = colorRevenue,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = AppTheme.colors.textSecondary,
            modifier = Modifier.size(22.dp)
        )
    }
}