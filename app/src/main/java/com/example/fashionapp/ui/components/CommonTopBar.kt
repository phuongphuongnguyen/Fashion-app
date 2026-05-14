package com.example.fashionapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onCartClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        },
        actions = {
            if (showActions) {
                Row(
                    modifier = Modifier.padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((-4).dp)
                ) {
                    IconButton(onClick = onCartClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cart),
                            contentDescription = "Cart",
                            modifier = Modifier.size(38.dp),
                            tint = Color.Unspecified
                        )
                    }

                    IconButton(onClick = onMessClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_mess),
                            contentDescription = "Messages",
                            modifier = Modifier.size(38.dp),
                            tint = Color.Unspecified
                        )
                    }
                    IconButton(onClick = onNotiClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_noti),
                            contentDescription = "Notifications",
                            modifier = Modifier.size(38.dp),
                            tint = Color.Unspecified
                        )
                    }

                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
        // Xử lý khoảng cách với Status Bar hệ thống
        windowInsets = TopAppBarDefaults.windowInsets
    )
}
