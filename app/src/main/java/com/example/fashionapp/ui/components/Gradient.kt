package com.example.fashionapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Overlay gradient theo đường chéo lên nội dung (giống Instagram tint)
 */
fun Modifier.diagonalGradientTint(
    colors: List<Color>,
    blendMode: BlendMode = BlendMode.SrcAtop
) = drawWithContent {
    drawContent()

    val brush = Brush.linearGradient(
        colors = colors,
        start = Offset(0f, 0f),
        end = Offset(size.width, size.height)
    )

    drawRect(
        brush = brush,
        blendMode = blendMode
    )
}

/**
 * Background gradient có thể dịch chuyển (dùng cho shimmer / animation)
 */
fun Modifier.offsetGradientBackground(
    colors: List<Color>,
    offset: Float = 0f
) = drawBehind {
    drawRect(
        brush = Brush.horizontalGradient(
            colors = colors,
            startX = -offset,
            endX = size.width - offset,
            tileMode = TileMode.Mirror
        )
    )
}

/**
 * Border gradient chéo (ổn định, không dùng Offset.Infinite)
 */
fun Modifier.diagonalGradientBorder(
    colors: List<Color>,
    borderSize: Dp = 2.dp,
    shape: Shape
) = drawBehind {

    val strokeWidth = borderSize.toPx()

    val brush = Brush.linearGradient(
        colors = colors,
        start = Offset(0f, 0f),
        end = Offset(size.width, size.height)
    )

    // NOTE: drawRoundRect dùng cho shape bo góc cơ bản
    drawRoundRect(
        brush = brush,
        style = Stroke(width = strokeWidth)
    )
}

/**
 * Border gradient fade in/out (mượt + nhẹ hơn)
 */
fun Modifier.fadeInDiagonalGradientBorder(
    showBorder: Boolean,
    colors: List<Color>,
    borderSize: Dp = 2.dp,
    shape: Shape
) = composed {

    val alpha = animateFloatAsState(
        targetValue = if (showBorder) 1f else 0f,
        animationSpec = tween(300),
        label = "border_alpha"
    )

    val animatedColors = colors.map {
        it.copy(alpha = alpha.value)
    }

    this.then(
        Modifier.diagonalGradientBorder(
            colors = animatedColors,
            borderSize = borderSize,
            shape = shape
        )
    )
}