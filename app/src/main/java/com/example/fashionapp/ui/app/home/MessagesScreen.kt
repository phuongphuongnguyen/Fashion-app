package com.example.fashionapp.ui.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fashionapp.R
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.components.FashionTopBar
import com.example.fashionapp.data.NotificationModel
import com.example.fashionapp.ui.app.settings.LocalAppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    navController: NavController,
    viewModel: NotificationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val settings = LocalAppSettings.current
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            FashionTopBar(
                title = settings.t("Notifications", "Thông báo"),
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Fixed Chatbot Item ──
            ChatbotEntryItem(onClick = { navController.navigate(Screen.Chatbot.route) })
            
            HorizontalDivider(thickness = 8.dp, color = Color(0xFFF7F7F7))

            // ── Notifications List ──
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(settings.t("No notifications", "Không có thông báo nào"), color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(notifications) { item ->
                        NotificationItem(
                            data = item,
                            onClick = { viewModel.markAsRead(item.id) }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatbotEntryItem(onClick: () -> Unit) {
    val settings = LocalAppSettings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chatbot),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Fashion AI Assistant",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = settings.t("Ask me anything about fashion!", "Hỏi tôi bất kỳ câu hỏi nào về thời trang!"),
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_mess),
            contentDescription = null,
            tint = Color(0xFF3669C9),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun NotificationItem(data: NotificationModel, onClick: () -> Unit) {
    val settings = LocalAppSettings.current
    val title = when (data.type) {
        "PAYMENT" -> settings.t("Payment successful 🎉", "Thanh toán thành công 🎉")
        "SHIPPING" -> settings.t("Order updates 🚚", "Theo dõi đơn hàng 🚚")
        "LIKE" -> settings.t("Liked post ❤️", "Yêu thích bài viết ❤️")
        "COMMENT" -> settings.t("Commented on post 💬", "Bình luận bài viết 💬")
        "SAVE" -> settings.t("Saved post 💾", "Lưu bài viết 💾")
        else -> settings.t("Notification 🔔", "Thông báo 🔔")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (!data.isRead) Color(0xFFF6F8FE) else Color.Transparent)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F4FF)),
            contentAlignment = Alignment.Center
        ) {
            Text("🔔", fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = if (!data.isRead) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    text = formatTimeAgo(data.createdAt, settings),
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = data.message,
                color = Color(0xFF4A4A4A),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
        if (!data.isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3669C9))
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

private fun formatTimeAgo(timestamp: com.google.firebase.Timestamp?, settings: com.example.fashionapp.ui.app.settings.AppSettingsViewModel): String {
    val time = timestamp?.toDate()?.time ?: return ""
    val diff = System.currentTimeMillis() - time
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> settings.t("just now", "vừa xong")
        minutes < 60 -> settings.t("${minutes}m ago", "${minutes} phút trước")
        hours < 24 -> settings.t("${hours}h ago", "${hours} giờ trước")
        else -> settings.t("${days}d ago", "${days} ngày trước")
    }
}
