package com.example.fashionapp.ui.app.shopping

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.navigation.NavController
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.data.user.UserSession
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.theme.AppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// ── MoMo Sandbox config ───────────────────────────────────────────────────────
private const val PARTNER_CODE   = "MOMO"
private const val ACCESS_KEY     = "F8BBA842ECF85"
private const val SECRET_KEY     = "K951B6PE1waDMi640xX08PD3vg6EkVlz"
private const val ENDPOINT       = "https://test-payment.momo.vn/v2/gateway/api/create"
private const val QUERY_ENDPOINT = "https://test-payment.momo.vn/v2/gateway/api/query"
private const val REDIRECT_URL   = "https://webhook.site/momo-redirect"
private const val IPN_URL        = "https://webhook.site/momo-ipn"

// Màn hình hiển thị mã QR thanh toán qua ví điện tử MoMo Sandbox và theo dõi trạng thái giao dịch để tự động xác nhận đơn hàng
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomoPaymentScreen(
    amount: Long,
    navController: NavController,
    selectedCartItemIds: Set<String> = emptySet(),
    viewModel: ShopViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
){
    val settings = LocalAppSettings.current
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by UserSession.currentUser.collectAsState()
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val checkoutItems = remember(uiState.cartItems, selectedCartItemIds) {
        if (selectedCartItemIds.isEmpty()) {
            uiState.cartItems
        } else {
            uiState.cartItems.filter { it.id in selectedCartItemIds }
        }
    }
    val checkoutUser = uiState.currentUserProfile ?: currentUser
    val shippingAddress = checkoutUser?.address.orEmpty()
    val hasShippingAddress = shippingAddress.isNotBlank()
    val selectedItemsMissing = selectedCartItemIds.isNotEmpty() &&
        !uiState.isLoadingCart &&
        checkoutItems.size < selectedCartItemIds.size

    var qrCodeUrl        by remember { mutableStateOf<String?>(null) }
    var momoOrderId      by remember { mutableStateOf<String?>(null) }
    var isLoading        by remember { mutableStateOf(true) }
    var errorMsg         by remember { mutableStateOf<String?>(null) }
    var isPaid           by remember { mutableStateOf(false) }
    var isPolling        by remember { mutableStateOf(false) }

    // ── Xin quyền Notification (phải đặt TRONG composable) ──────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted hay không cũng không cần xử lý */ }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ── Bước 1: Tạo QR MoMo ─────────────────────────────────────────────────
    LaunchedEffect(amount, uiState.isLoadingCart, checkoutItems, hasShippingAddress, selectedItemsMissing) {
        if (uiState.isLoadingCart) return@LaunchedEffect
        when {
            checkoutItems.isEmpty() -> {
                errorMsg = settings.t("No products to checkout", "Không có sản phẩm để thanh toán")
                isLoading = false
                return@LaunchedEffect
            }
            selectedItemsMissing -> {
                errorMsg = settings.t("Some selected products are no longer in the cart", "Một số sản phẩm đã chọn không còn trong giỏ hàng")
                isLoading = false
                return@LaunchedEffect
            }
            !hasShippingAddress -> {
                errorMsg = settings.t("Please update your shipping address before paying", "Vui lòng cập nhật địa chỉ giao hàng trước khi thanh toán")
                isLoading = false
                return@LaunchedEffect
            }
            amount <= 0L -> {
                errorMsg = settings.t("Invalid payment amount", "Số tiền thanh toán không hợp lệ")
                isLoading = false
                return@LaunchedEffect
            }
        }
        try {
            val result  = withContext(Dispatchers.IO) { createMomoQR(amount) }
            qrCodeUrl   = result.first
            momoOrderId = result.second
            isLoading   = false

        } catch (e: Exception) {
            errorMsg = e.message
            isLoading = false
            Log.e("MoMo", "Lỗi tạo QR: ${e.message}")
        }
    }

    // ── Bước 2: Polling MoMo 3s/lần ─────────────────────────────────────────
    LaunchedEffect(momoOrderId) {
        val oid = momoOrderId ?: return@LaunchedEffect
        if (!hasShippingAddress || checkoutItems.isEmpty()) return@LaunchedEffect
        isPolling = true
        while (isActive && !isPaid) {
            delay(3000)
            try {
                val code = withContext(Dispatchers.IO) { checkMomoStatus(oid) }
                Log.d("MoMo", "Polling resultCode: $code")
                if (code == 0) {
                    isPaid    = true
                    isPolling = false
                    viewModel.placeOrderFromCart(
                        cartItems = checkoutItems,
                        paymentMethod = "MoMo",
                        paymentStatus = "PAID",
                        shippingAddress = shippingAddress,
                        momoOrderId = oid
                    ) { orderId ->
                        if (orderId == null) return@placeOrderFromCart

                        showPaymentNotification(context, amount, settings)

                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        db.collection("users")
                            .document(uid)
                            .collection("user_notifications")
                            .add(
                                hashMapOf(
                                    "id" to "",
                                    "userId" to uid,
                                    "message" to settings.t("Order ₫${formatAmount(amount)} was paid successfully!", "Đơn hàng ₫${formatAmount(amount)} đã được thanh toán thành công!"),
                                    "type" to "PAYMENT",
                                    "isRead" to false,
                                    "createdAt" to com.google.firebase.Timestamp.now()
                                )
                            ).addOnSuccessListener { docRef ->
                                docRef.update("id", docRef.id)
                            }

                        OrderTrackingScheduler.scheduleTracking(
                            context = context,
                            orderId = orderId,
                            userId  = uid,
                            amount  = amount
                        )

                        navController.navigate(Screen.History.createRoute("Ongoing")) {
                            popUpTo(Screen.Cart.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MoMo", "Polling lỗi: ${e.message}")
            }
        }
    }

    // ── UI ───────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settings.t("MoMo Payment", "Thanh toán MoMo"), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = settings.t("Back", "Quay lại"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFAE2070))
                        Spacer(Modifier.height(12.dp))
                        Text(settings.t("Generating QR Code...", "Đang tạo mã QR..."), color = AppTheme.colors.textSecondary)
                    }
                }

                errorMsg != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(settings.t("❌ Error: ", "❌ Lỗi: ") + errorMsg, color = Color.Red)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { navController.popBackStack() }) {
                            Text(settings.t("Back", "Quay lại"))
                        }
                    }
                }

                isPaid -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("🎉", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            settings.t("Payment successful!", "Thanh toán thành công!"),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFAE2070)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "₫${formatAmount(amount)}",
                            fontSize = 18.sp,
                            color = AppTheme.colors.textSecondary
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = {
                                // Navigate về History, xóa stack về Cart
                                navController.navigate(Screen.History.createRoute("Ongoing")) {
                                    popUpTo(Screen.Cart.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAE2070)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text(settings.t("View order history", "Xem lịch sử đơn hàng"), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    "MoMo",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFAE2070)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(settings.t("Scan code to pay", "Quét mã để thanh toán"), fontSize = 13.sp, color = AppTheme.colors.textSecondary)
                                Spacer(Modifier.height(20.dp))

                                Box(
                                    modifier = Modifier
                                        .size(220.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF5F5F5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val qrBitmap = remember(qrCodeUrl) {
                                        qrCodeUrl?.let { generateQrBitmap(it) }
                                    }
                                    qrBitmap?.let {
                                        Image(
                                            bitmap = it.asImageBitmap(),
                                            contentDescription = "QR MoMo",
                                            modifier = Modifier.size(200.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(20.dp))
                                HorizontalDivider(color = AppTheme.colors.divider)
                                Spacer(Modifier.height(16.dp))

                                Text(settings.t("Amount to pay", "Số tiền thanh toán"), fontSize = 13.sp, color = AppTheme.colors.textSecondary)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "₫${formatAmount(amount)}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFAE2070)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (isPolling) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFAE2070)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(settings.t("Waiting for payment...", "Đang chờ thanh toán..."), fontSize = 13.sp, color = AppTheme.colors.textSecondary)
                            }
                        } else {
                            Text(
                                settings.t("Open MoMo Test app → Scan QR to pay", "Mở app MoMo Test → Quét QR để thanh toán"),
                                fontSize = 13.sp,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Tạo QR MoMo ──────────────────────────────────────────────────────────────
// Gửi yêu cầu khởi tạo giao dịch tới cổng thanh toán MoMo Sandbox và lấy liên kết chứa mã QR thanh toán
private fun createMomoQR(amount: Long): Pair<String, String> {
    val orderId   = "ORDER_${UUID.randomUUID()}"
    val requestId = UUID.randomUUID().toString()
    val orderInfo = "Thanh toán đơn hàng"
    val extraData = ""

    val rawSignature = "accessKey=$ACCESS_KEY" +
            "&amount=$amount" +
            "&extraData=$extraData" +
            "&ipnUrl=$IPN_URL" +
            "&orderId=$orderId" +
            "&orderInfo=$orderInfo" +
            "&partnerCode=$PARTNER_CODE" +
            "&redirectUrl=$REDIRECT_URL" +
            "&requestId=$requestId" +
            "&requestType=captureWallet"

    val signature = hmacSHA256(SECRET_KEY, rawSignature)

    val body = JSONObject().apply {
        put("partnerCode", PARTNER_CODE)
        put("accessKey", ACCESS_KEY)
        put("requestId", requestId)
        put("amount", amount)
        put("orderId", orderId)
        put("orderInfo", orderInfo)
        put("redirectUrl", REDIRECT_URL)
        put("ipnUrl", IPN_URL)
        put("extraData", extraData)
        put("requestType", "captureWallet")
        put("signature", signature)
        put("lang", "vi")
    }.toString()

    val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        setRequestProperty("Content-Type", "application/json")
        doOutput = true
        connectTimeout = 30_000
        readTimeout    = 30_000
    }
    OutputStreamWriter(conn.outputStream).use { it.write(body) }

    val response = try {
        conn.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        conn.errorStream?.bufferedReader()?.readText() ?: throw e
    }

    val json = JSONObject(response)
    if (json.getInt("resultCode") != 0) {
        throw Exception("MoMo lỗi: ${json.getString("message")}")
    }
    return Pair(json.getString("qrCodeUrl"), orderId)
}

// Gửi yêu cầu truy vấn trạng thái thanh toán của đơn hàng theo ID tới cổng Sandbox của MoMo
private fun checkMomoStatus(orderId: String): Int {
    val requestId    = UUID.randomUUID().toString()
    val rawSignature = "accessKey=$ACCESS_KEY" +
            "&orderId=$orderId" +
            "&partnerCode=$PARTNER_CODE" +
            "&requestId=$requestId"
    val signature = hmacSHA256(SECRET_KEY, rawSignature)

    val body = JSONObject().apply {
        put("partnerCode", PARTNER_CODE)
        put("accessKey", ACCESS_KEY)
        put("requestId", requestId)
        put("orderId", orderId)
        put("signature", signature)
        put("lang", "vi")
    }.toString()

    val conn = (URL(QUERY_ENDPOINT).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        setRequestProperty("Content-Type", "application/json")
        doOutput = true
        connectTimeout = 15_000
        readTimeout    = 15_000
    }
    OutputStreamWriter(conn.outputStream).use { it.write(body) }

    val response = try {
        conn.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        conn.errorStream?.bufferedReader()?.readText() ?: return -1
    }
    return JSONObject(response).getInt("resultCode")
}

// Khởi tạo và hiển thị thông báo hệ thống cục bộ (Local Notification) khi người dùng hoàn tất thanh toán đơn hàng
private fun showPaymentNotification(context: Context, amount: Long, settings: com.example.fashionapp.ui.app.settings.AppSettingsViewModel) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val prefix = if (uid.isBlank()) "" else "${uid}_"

    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    val isMasterEnabled = prefs.getBoolean("${prefix}notifications", prefs.getBoolean("notifications", true))
    val isSystemEnabled = prefs.getBoolean("${prefix}system_notifications", prefs.getBoolean("system_notifications", true))
    val isOrderEnabled = prefs.getBoolean("${prefix}order_updates", prefs.getBoolean("order_updates", true))
    if (!isMasterEnabled || !isSystemEnabled || !isOrderEnabled) return

    val channelId = "payment_channel"
    val manager   = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    manager.createNotificationChannel(
        NotificationChannel(channelId, settings.t("Payment", "Thanh toán"), NotificationManager.IMPORTANCE_HIGH)
    )

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(settings.t("Payment successful 🎉", "Thanh toán thành công 🎉"))
        .setContentText(settings.t("Order ₫${formatAmount(amount)} has been paid!", "Đơn hàng ₫${formatAmount(amount)} đã được thanh toán!"))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    manager.notify(System.currentTimeMillis().toInt(), notification)
}

// Sử dụng thư viện ZXing để tạo mã QR dạng ảnh Bitmap hiển thị lên màn hình từ chuỗi dữ liệu URL nhận được
fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
    val writer = QRCodeWriter()
    val matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
    val bmp    = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size)
        for (y in 0 until size)
            bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    return bmp
}

// Sử dụng thuật toán mã hóa HmacSHA256 để tạo chữ ký số (Signature) xác thực tính bảo mật cho giao dịch MoMo
private fun hmacSHA256(secret: String, data: String): String {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
    return mac.doFinal(data.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

// ── Format số tiền ────────────────────────────────────────────────────────────
private fun formatAmount(amount: Long): String =
    amount.toString().reversed().chunked(3).joinToString(".").reversed()
