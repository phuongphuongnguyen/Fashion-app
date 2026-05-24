package com.example.fashionapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.fashionapp.R

@Composable
fun DoubleTapPhotoLikeAnimation(
    onDoubleTap: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        onDoubleTap()

                        scope.launch {
                            // reset
                            scale.snapTo(0f)
                            alpha.snapTo(1f)

                            // pop effect
                            scale.animateTo(
                                targetValue = 1.2f,
                                animationSpec = spring(
                                    dampingRatio = 0.4f
                                )
                            )

                            // settle
                            scale.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(100)
                            )

                            // fade out
                            alpha.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(300)
                            )
                        }
                    }
                )
            }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_like_full),
            contentDescription = "Like",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .scale(scale.value)
                .graphicsLayer {
                    this.alpha = alpha.value
                }
        )
    }
}