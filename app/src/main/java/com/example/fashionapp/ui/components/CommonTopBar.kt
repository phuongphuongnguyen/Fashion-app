package com.example.fashionapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fashionapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    title: String,
    showActions: Boolean = true,
    onNotiClick: () -> Unit = {},
    onMessClick: () -> Unit = {},
    onCartClick: () -> Unit = {},
    isDark: Boolean = false,
    bgColor: Color = Color.White,
    textColor: Color = Color.Black
) {
    val circleBgColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF8F8F8)
    val iconTint = if (isDark) Color.White else Color.Unspecified

    TopAppBar(
        title = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = textColor)
        },
        actions = {
            if (showActions) {
                Row(
                    modifier = Modifier.padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onCartClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(circleBgColor)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cart),
                            contentDescription = "Cart",
                            modifier = Modifier.size(38.dp).padding(6.dp),
                            tint = iconTint
                        )
                    }

                    IconButton(
                        onClick = onMessClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(circleBgColor)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_mess),
                            contentDescription = "Messages",
                            modifier = Modifier.size(38.dp).padding(6.dp),
                            tint = iconTint
                        )
                    }
                    IconButton(
                        onClick = onNotiClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(circleBgColor)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_noti),
                            contentDescription = "Notifications",
                            modifier = Modifier.size(38.dp).padding(6.dp),
                            tint = iconTint
                        )
                    }

                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor),
        windowInsets = TopAppBarDefaults.windowInsets
    )
}
