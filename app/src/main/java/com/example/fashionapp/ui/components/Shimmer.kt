package com.example.fashionapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Brush gradient quét ngang lặp lại — dùng làm background cho placeholder.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
    val sweepWidth = 1600f
    val offset = progress * sweepWidth - 400f
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFFE8E8E8),
            Color(0xFFF4F4F4),
            Color(0xFFE8E8E8),
        ),
        start = Offset(offset - 200f, 0f),
        end   = Offset(offset + 200f, 0f),
    )
}

/**
 * Box xám có shimmer animate. Dùng làm placeholder cho text/image trong skeleton.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(rememberShimmerBrush())
    )
}
