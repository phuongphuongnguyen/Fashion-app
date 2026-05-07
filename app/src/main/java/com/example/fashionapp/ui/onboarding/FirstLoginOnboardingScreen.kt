package com.example.fashionapp.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fashionapp.ui.auth.AuthPrimaryBlue
import com.example.fashionapp.ui.auth.AuthTextDark
import com.example.fashionapp.ui.auth.AuthTextSubtle

private data class OnboardingPage(
    val title: String,
    val body: String,
    val headerGradient: List<Color>
)

private val pages = listOf(
    OnboardingPage(
        title = "Chào bạn",
        body = "Khám phá xu hướng thời trang, gợi ý phối đồ và mua sắm dễ dàng ngay trên điện thoại.",
        headerGradient = listOf(Color(0xFFFFE4EC), Color(0xFFFFB6C1))
    ),
    OnboardingPage(
        title = "Hello",
        body = "Lưu các món yêu thích, theo dõi cửa hàng và nhận cá nhân hóa theo gu của bạn.",
        headerGradient = listOf(Color(0xFFFFB6E1), Color(0xFFFF8FAB))
    ),
    OnboardingPage(
        title = "Mua thông minh",
        body = "Tìm sản phẩm phù hợp size, giá và danh mục—mọi thứ được sắp xếp gọn gàng cho bạn.",
        headerGradient = listOf(Color(0xFFC8E7FF), Color(0xFFFFD6E8))
    ),
    OnboardingPage(
        title = "Ready?",
        body = "Bạn đã sẵn sàng khám phá Fashion App—bắt đầu ngay với các gợi ý được chọn riêng cho bạn.",
        headerGradient = listOf(Color(0xFFB8E0FF), Color(0xFFFFC2E0))
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FirstLoginOnboardingScreen(
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        OnboardingBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(460.dp)
                    ) { page ->
                        val item = pages[page]
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 28.dp,
                                            topEnd = 28.dp
                                        )
                                    )
                                    .background(
                                        Brush.linearGradient(item.headerGradient)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🛍",
                                    fontSize = 64.sp
                                )
                            }
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = 24.dp,
                                    vertical = 28.dp
                                ),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = item.title,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AuthTextDark,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = item.body,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = AuthTextSubtle,
                                    textAlign = TextAlign.Center
                                )
                                if (page == pages.lastIndex) {
                                    Spacer(modifier = Modifier.height(28.dp))
                                    Button(
                                        onClick = onComplete,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AuthPrimaryBlue
                                        )
                                    ) {
                                        Text(
                                            text = "Let's Start",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (selected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) AuthPrimaryBlue
                                else Color(0xFFE0E0E0)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        val blue = AuthPrimaryBlue.copy(alpha = 0.18f)
        drawCircle(
            color = blue,
            radius = size.minDimension * 0.38f,
            center = Offset(0f, size.height * 0.12f)
        )
        drawCircle(
            color = blue.copy(alpha = 0.12f),
            radius = size.minDimension * 0.42f,
            center = Offset(size.width, size.height * 0.92f)
        )
    }
}
