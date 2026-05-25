package com.example.fashionapp.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val body: String,
    val emoji: String,
    val headerGradient: List<Color>
)

private val pages = listOf(
    OnboardingPage(
        title = "Chào mừng bạn",
        body = "Khám phá xu hướng thời trang, gợi ý phối đồ và mua sắm dễ dàng ngay trên điện thoại.",
        emoji = "\uD83D\uDECD\uFE0F",
        headerGradient = listOf(Color(0xFFFFE4EC), Color(0xFFF8BBD0))
    ),
    OnboardingPage(
        title = "Dành riêng cho bạn",
        body = "Lưu các món yêu thích, theo dõi cửa hàng và nhận gợi ý cá nhân hóa theo gu của bạn.",
        emoji = "❤\uFE0F",
        headerGradient = listOf(Color(0xFFF3E5F5), Color(0xFFE1BEE7))
    ),
    OnboardingPage(
        title = "Mua sắm thông minh",
        body = "Tìm sản phẩm phù hợp size, giá và danh mục — mọi thứ được sắp xếp gọn gàng cho bạn.",
        emoji = "\uD83D\uDD27",
        headerGradient = listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB))
    ),
    OnboardingPage(
        title = "Bạn đã sẵn sàng?",
        body = "Bắt đầu ngay để không bỏ lỡ những bộ sưu tập thời trang tuyệt vời dành riêng cho bạn.",
        emoji = "✨",
        headerGradient = listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3))
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FirstLoginOnboardingScreen(
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Background decorative gradient
        OnboardingBackdrop()

        // ── Top bar: "Bỏ qua" button ──
        if (!isLastPage) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(top = 48.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Bỏ qua",
                    color = AuthTextSubtle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onComplete() }
                )
            }
        }

        // ── Main card (vertically centered) ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.68f)
                .padding(horizontal = 22.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                ) { page ->
                    val item = pages[page]
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Gradient header with icon — fills available space above text
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(
                                    Brush.linearGradient(item.headerGradient)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Circle background for icon
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.emoji,
                                    fontSize = 54.sp
                                )
                            }
                        }

                        // Title and description
                        Column(
                            modifier = Modifier.padding(
                                horizontal = 28.dp,
                                vertical = 24.dp
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = item.title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = AuthTextDark,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = item.body,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = AuthTextSubtle,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // ── Page indicators (inside card, at bottom) ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = tween(300),
                            label = "dotWidth"
                        )
                        val color by animateColorAsState(
                            targetValue = if (isSelected) AuthPrimaryBlue else Color(0xFFD0D0D0),
                            animationSpec = tween(300),
                            label = "dotColor"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .width(width)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                        )
                    }
                }
            }
        }

        // ── Bottom section (pinned to bottom) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 22.dp, vertical = 32.dp)
        ) {
            if (isLastPage) {
                // Last page: full-width "Bắt đầu ngay" button
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuthPrimaryBlue
                    )
                ) {
                    Text(
                        text = "Bắt đầu ngay",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                }
            } else {
                // Pages 1-3: "Bước X/4" on left, arrow FAB on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bước ${pagerState.currentPage + 1}/${pages.size}",
                        color = AuthTextSubtle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        containerColor = AuthPrimaryBlue,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Tiếp theo",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        val blue = AuthPrimaryBlue.copy(alpha = 0.10f)
        val pink = Color(0xFFFF80AB).copy(alpha = 0.08f)

        drawCircle(
            color = blue,
            radius = size.minDimension * 0.45f,
            center = Offset(size.width * 0.15f, size.height * 0.9f)
        )
        drawCircle(
            color = pink,
            radius = size.minDimension * 0.35f,
            center = Offset(size.width * 0.85f, size.height * 0.95f)
        )
    }
}
