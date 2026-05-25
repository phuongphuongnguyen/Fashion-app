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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(navController: NavController) {
    Scaffold(
        topBar = {
            FashionTopBar(
                title = "Notifications",
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
            val notifications = listOf(
                NotificationData("Order Success", "Your order #12345 has been placed successfully.", "2m ago"),
                NotificationData("Flash Sale", "Flash sale starts in 30 minutes. Don't miss out!", "1h ago"),
                NotificationData("New Collection", "New summer collection is now available.", "5h ago"),
                NotificationData("Shipping Update", "Your package is on its way to your address.", "1d ago"),
                NotificationData("Promotion", "Get 20% off on your next purchase.", "2d ago")
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(notifications) { item ->
                    NotificationItem(item)
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))
                }
            }
        }
    }
}

@Composable
private fun ChatbotEntryItem(onClick: () -> Unit) {
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
                text = "Ask me anything about fashion!",
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
private fun NotificationItem(data: NotificationData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                    text = data.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    text = data.time,
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
    }
}

data class NotificationData(
    val title: String,
    val message: String,
    val time: String
)
