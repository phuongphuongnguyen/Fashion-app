//package com.example.fashionapp.ui.auth
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.Button
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//
//@Composable
//fun StartScreen(
//    onGetStarted: () -> Unit,
//    onLoginClick: () -> Unit
//) {
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(AuthBackground)
//            .padding(horizontal = 24.dp, vertical = 36.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Box(
//            modifier = Modifier
//                .background(color = ColorWhite, shape = CircleShape)
//                .padding(28.dp)
//        ) {
//            Text(
//                text = "🛍",
//                fontSize = 36.sp
//            )
//        }
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        Text(
//            text = "Fashion app",
//            fontSize = 34.sp,
//            fontWeight = FontWeight.Bold,
//            color = AuthTextDark
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        Text(
//            text = "Beautiful eCommerce UI Kit\nfor your online store",
//            fontSize = 14.sp,
//            color = AuthTextSubtle
//        )
//
//        Spacer(modifier = Modifier.height(30.dp))
//
//        Button(
//            onClick = onGetStarted,
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(52.dp),
//            shape = RoundedCornerShape(14.dp),
//            colors = ButtonDefaults.buttonColors(containerColor = AuthPrimaryBlue)
//        ) {
//            Text("Let's get started")
//        }
//
//        Spacer(modifier = Modifier.height(18.dp))
//
//        Text(
//            text = "I already have an account",
//            color = AuthPrimaryBlue,
//            fontSize = 13.sp
//        )
//
//        Spacer(modifier = Modifier.height(4.dp))
//
//        Button(
//            onClick = onLoginClick,
//            shape = RoundedCornerShape(10.dp),
//            colors = ButtonDefaults.buttonColors(containerColor = ColorWhite)
//        ) {
//            Text("Login", color = AuthPrimaryBlue)
//        }
//    }
//}
//
//private val ColorWhite = androidx.compose.ui.graphics.Color.White
package com.example.fashionapp.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fashionapp.R
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip

// Màn hình khởi đầu giới thiệu của ứng dụng hỗ trợ điều hướng đăng nhập hoặc bắt đầu khám phá cửa hàng
@Composable
fun StartScreen(
    onGetStarted: () -> Unit,
    onLoginClick: () -> Unit
) {
    val settings = LocalAppSettings.current
    val context = LocalContext.current
    val logoPainter = remember {
        val drawable = ResourcesCompat.getDrawable(context.resources, R.mipmap.ic_launcher, context.theme)
        val bitmap = Bitmap.createBitmap(
            drawable?.intrinsicWidth?.coerceAtLeast(100) ?: 100,
            drawable?.intrinsicHeight?.coerceAtLeast(100) ?: 100,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable?.setBounds(0, 0, canvas.width, canvas.height)
        drawable?.draw(canvas)
        BitmapPainter(bitmap.asImageBitmap())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 32.dp)
            .padding(bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Phần trên căn giữa
        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(color = Color(0xFFE8EEFF)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = logoPainter,
                contentDescription = "App Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Fashion app",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )

        Spacer(modifier = Modifier.height(8.dp))

//        Text(
//            text = settings.t(
//                "Beautiful eCommerce UI Kit\nfor your online store",
//                "Giao diện mua sắm thời trang tuyệt đẹp\ncho cửa hàng trực tuyến của bạn"
//            ),
//            fontSize = 14.sp,
//            color = Color(0xFF9E9E9E),
//            textAlign = TextAlign.Center
//        )

        // Đẩy button xuống dưới
        Spacer(modifier = Modifier.weight(1f))

        // Button Let's get started
        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4461F2))
        ) {
            Text(
                text = settings.t("Let's get started", "Bắt đầu ngay"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // "I already have an account →"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onLoginClick() }
        ) {
            Text(
                text = settings.t("I already have an account", "Tôi đã có tài khoản"),
                color = Color(0xFF9E9E9E),
                fontSize = 14.sp
            )
        }
    }
}