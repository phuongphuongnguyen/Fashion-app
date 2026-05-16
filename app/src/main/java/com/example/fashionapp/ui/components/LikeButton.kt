package com.example.fashionapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.fashionapp.R
import com.example.fashionapp.model.Post

@Composable
fun AnimLikeButton(
    post: Post,
    onLikeClick: (Post) -> Unit
) {
    // 1. Dùng Animatable cho Float (Scale) - Cái này bạn làm đúng
    val scale = remember { Animatable(1f) }

    val isLiked = post.isLiked

    // 2. Dùng animateColorAsState cho màu sắc (Sửa lỗi mismatch ở đây)
    // Nó sẽ tự động tạo hoạt ảnh khi targetValue thay đổi
    val animatedColor by animateColorAsState(
        targetValue = if (isLiked) Color(0xFFE53935) else Color.Gray,
        animationSpec = tween(durationMillis = 300),
        label = "LikeColorAnimation"
    )

    // 3. LaunchedEffect chỉ dùng để xử lý hiệu ứng Scale khi nhấn Like
    LaunchedEffect(isLiked) {
        if (isLiked) {
            // Hiệu ứng nảy (Bouncing effect)
            scale.animateTo(0.7f, spring(dampingRatio = 0.4f))
            scale.animateTo(1.2f, spring(dampingRatio = 0.3f))
            scale.animateTo(1f, tween(100))
        } else {
            // Khi bỏ like thì về kích thước cũ ngay lập tức
            scale.snapTo(1f)
        }
    }

    Box(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp), // ripple Material 3
                onClick = { onLikeClick(post) }
            )
            .padding(10.dp)
            .size(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(
                id = if (isLiked) R.drawable.ic_filled_favorite
                else R.drawable.ic_saved
            ),
            contentDescription = "Like",
            tint = animatedColor,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .size(24.dp)
        )
    }
}