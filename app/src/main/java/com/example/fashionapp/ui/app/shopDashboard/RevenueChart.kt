package com.example.fashionapp.ui.app.shopDashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fashionapp.data.shop.DailyRevenuePoint
import com.example.fashionapp.ui.theme.AppTheme

/**
 * Bar chart đơn giản tự vẽ bằng Compose (không cần thư viện ngoài).
 * Mỗi cột = 1 ngày; chiều cao tỉ lệ theo doanh thu so với ngày cao nhất.
 */
@Composable
fun RevenueBarChart(
    data: List<DailyRevenuePoint>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Chưa có dữ liệu doanh thu", color = AppTheme.colors.textSecondary, fontSize = 13.sp)
        }
        return
    }

    val maxRevenue = data.maxOf { it.revenue }.coerceAtLeast(1.0)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { point ->
            val fraction = (point.revenue / maxRevenue).toFloat().coerceIn(0.03f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatShortMoney(point.revenue),
                    fontSize = 9.sp,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                // Vùng cột cao cố định, thanh bên trong fill theo fraction
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(barColor)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = shortDate(point.date),
                    fontSize = 9.sp,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Helpers dùng chung trong package shopdashboard ──

// 945000.0 → "945.000"
internal fun formatMoney(value: Double): String =
    "%.0f".format(value).reversed().chunked(3).joinToString(".").reversed()

// 945000.0 → "945K", 14930000.0 → "14.9M"
internal fun formatShortMoney(value: Double): String = when {
    value >= 1_000_000 -> "%.1fM".format(value / 1_000_000.0)
    value >= 1_000     -> "%.0fK".format(value / 1_000.0)
    else               -> "%.0f".format(value)
}

// "2026-06-09" → "09/06"
internal fun shortDate(isoDate: String): String {
    val parts = isoDate.split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}" else isoDate
}

// "2026-06-09" → "09/06/2026"
internal fun fullDate(isoDate: String): String {
    val parts = isoDate.split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else isoDate
}