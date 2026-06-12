package com.example.fashionapp.ui.app.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fashionapp.data.user.UserSession
import com.example.fashionapp.model.User
import com.example.fashionapp.ui.theme.AppTheme
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun RegisterShopScreen(
    onBack: () -> Unit,
    onRegistered: () -> Unit
) {
    val settings = LocalAppSettings.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    // Theme colors (auto light/dark)
    val c = AppTheme.colors
    val bgColor = c.background
    val textColor = c.textPrimary
    val subTextColor = c.textSecondary
    val dividerColor = c.divider
    val PrimaryBlue = MaterialTheme.colorScheme.secondary
    val isDark = settings.isDarkMode

    // Form fields
    var shopName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    // State
    var isRegistering by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var agreedToTerms by remember { mutableStateOf(false) }

    val inputBgColor = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF5F5F5)

    Scaffold(containerColor = bgColor) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))

            // Back arrow
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = settings.t("Back", "Quay lại"),
                    tint = textColor
                )
            }

            // Title
            Text(
                text = settings.t(
                    "Register as a Seller",
                    "Đăng ký mở Cửa hàng",
                    "S'inscrire en tant que vendeur",
                    "販売者として登録",
                    "판매자로 등록",
                    "注册为卖家"
                ),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Text(
                text = settings.t(
                    "Fill in your shop information to get started",
                    "Điền thông tin cửa hàng của bạn để bắt đầu",
                    "Remplissez les informations de votre boutique",
                    "ショップ情報を入力して始めましょう",
                    "매장 정보를 입력하여 시작하세요",
                    "填写您的店铺信息以开始"
                ),
                fontSize = 14.sp,
                color = subTextColor,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Spacer(Modifier.height(8.dp))

            // ── Warning note ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = settings.t(
                            "Once you register as a seller, your account will permanently become a Shop account and cannot be reverted to a regular user account.",
                            "Sau khi đăng ký, tài khoản của bạn sẽ vĩnh viễn trở thành tài khoản Cửa hàng và không thể quay lại tài khoản người dùng thường.",
                            "Une fois inscrit en tant que vendeur, votre compte deviendra définitivement un compte Boutique.",
                            "販売者として登録すると、アカウントは永久にショップアカウントになり、通常のユーザーアカウントに戻すことはできません。",
                            "판매자로 등록하면 계정이 영구적으로 쇼핑 계정이 되며 일반 사용자 계정으로 되돌릴 수 없습니다.",
                            "注册为卖家后，您的账户将永久成为商店账户，无法恢复为普通用户账户。"
                        ),
                        fontSize = 13.sp,
                        color = Color(0xFFE65100),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Shop Name ──
            Text(
                text = settings.t("Shop Name", "Tên cửa hàng", "Nom de la boutique", "ショップ名", "매장 이름", "店铺名称"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            OutlinedTextField(
                value = shopName,
                onValueChange = { shopName = it },
                placeholder = {
                    Text(
                        settings.t("Enter shop name", "Nhập tên cửa hàng", "Entrez le nom", "ショップ名を入力", "매장 이름 입력", "输入店铺名称"),
                        color = subTextColor
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = dividerColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedContainerColor = inputBgColor,
                    unfocusedContainerColor = inputBgColor
                ),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            // ── Description ──
            Text(
                text = settings.t("Description", "Mô tả", "Description", "説明", "설명", "描述"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = {
                    Text(
                        settings.t("Describe your shop...", "Mô tả cửa hàng của bạn...", "Décrivez votre boutique...", "ショップの説明...", "매장 설명...", "描述您的店铺..."),
                        color = subTextColor
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = dividerColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedContainerColor = inputBgColor,
                    unfocusedContainerColor = inputBgColor
                ),
                maxLines = 5
            )

            Spacer(Modifier.height(12.dp))

            // ── Location ──
            Text(
                text = settings.t("Location", "Địa chỉ", "Emplacement", "所在地", "위치", "位置"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                placeholder = {
                    Text(
                        settings.t("e.g. TP. Hồ Chí Minh", "VD: TP. Hồ Chí Minh", "ex: Ho Chi Minh Ville", "例: ホーチミン市", "예: 호치민시", "例: 胡志明市"),
                        color = subTextColor
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = dividerColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedContainerColor = inputBgColor,
                    unfocusedContainerColor = inputBgColor
                ),
                singleLine = true
            )

            Spacer(Modifier.height(20.dp))

            // ── Terms checkbox ──
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = agreedToTerms,
                    onCheckedChange = { agreedToTerms = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = PrimaryBlue,
                        uncheckedColor = subTextColor
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = settings.t(
                        "I agree to the Seller Terms & Conditions",
                        "Tôi đồng ý với Điều khoản dành cho Người bán",
                        "J'accepte les Conditions Générales du Vendeur",
                        "販売者利用規約に同意します",
                        "판매자 이용 약관에 동의합니다",
                        "我同意卖家条款与条件"
                    ),
                    fontSize = 14.sp,
                    color = textColor
                )
            }

            // ── Error message ──
            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFE53935),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            // ── Register button ──
            Button(
                onClick = {
                    // Validate
                    if (shopName.isBlank()) {
                        errorMessage = settings.t(
                            "Please enter a shop name",
                            "Vui lòng nhập tên cửa hàng",
                            "Veuillez entrer un nom de boutique",
                            "ショップ名を入力してください",
                            "매장 이름을 입력해주세요",
                            "请输入店铺名称"
                        )
                        return@Button
                    }
                    if (description.isBlank()) {
                        errorMessage = settings.t(
                            "Please enter a description",
                            "Vui lòng nhập mô tả",
                            "Veuillez entrer une description",
                            "説明を入力してください",
                            "설명을 입력해주세요",
                            "请输入描述"
                        )
                        return@Button
                    }
                    if (location.isBlank()) {
                        errorMessage = settings.t(
                            "Please enter a location",
                            "Vui lòng nhập địa chỉ",
                            "Veuillez entrer un emplacement",
                            "所在地を入力してください",
                            "위치를 입력해주세요",
                            "请输入位置"
                        )
                        return@Button
                    }
                    if (!agreedToTerms) {
                        errorMessage = settings.t(
                            "You must agree to the Terms & Conditions",
                            "Bạn phải đồng ý với Điều khoản & Điều kiện",
                            "Vous devez accepter les Conditions Générales",
                            "利用規約に同意する必要があります",
                            "이용 약관에 동의해야 합니다",
                            "您必须同意条款与条件"
                        )
                        return@Button
                    }

                    isRegistering = true
                    errorMessage = null

                    scope.launch {
                        try {
                            val db = FirebaseFirestore.getInstance()

                            // 1. Use the user's UID as the shop ID so they have permission to write to it
                            val shopId = uid
                            val shopDocRef = db.collection("shops").document(shopId)

                            // 2. Create shop document in Firestore
                            val shopData = hashMapOf(
                                "userId" to uid,
                                "name" to shopName,
                                "description" to description,
                                "location" to location,
                                "logoRef" to "avatar/logo1.jpg",
                                "followerCount" to 28900,
                                "isOfficial" to true,
                                "productCount" to 6,
                                "rating" to 4.8,
                                "responseRate" to 98,
                                "responseTime" to "Trong vài phút",
                                "reviewCount" to 12430,
                                "orderCount" to 36,
                                "soldCount" to 5970,
                                "revenue" to 14019000.0,
                                "createdAt" to Timestamp.now(),
                                "updatedAt" to Timestamp.now()
                            )
                            shopDocRef.set(shopData).await()

                            // 2.5 Initialize dailyRevenue subcollection for the current date with 0 values
                            try {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                val dayId = sdf.format(java.util.Date())
                                val initialDailyRevenue = hashMapOf(
                                    "orderCount" to 0,
                                    "revenue" to 0.0,
                                    "soldCount" to 0,
                                    "updatedAt" to Timestamp.now()
                                )
                                shopDocRef.collection("dailyRevenue").document(dayId).set(initialDailyRevenue).await()
                            } catch (e: Exception) {
                                android.util.Log.e("RegisterShopScreen", "Failed to initialize dailyRevenue subcollection", e)
                            }

                            // 3. Update user document: set role & shopId
                            db.collection("users").document(uid)
                                .update(
                                    mapOf(
                                        "role" to "SHOP",
                                        "shopId" to shopId
                                    )
                                ).await()

                            // 4. Update UserSession in-memory state
                            val currentUser = UserSession.currentUser.value
                            if (currentUser != null) {
                                UserSession.updateCurrentUser(
                                    currentUser.copy(role = "SHOP", shopId = shopId)
                                )
                            }

                            // 5. Navigate back / show success
                            onRegistered()

                        } catch (e: Exception) {
                            errorMessage = e.localizedMessage ?: "Registration failed"
                        } finally {
                            isRegistering = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                enabled = !isRegistering
            ) {
                if (isRegistering) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = settings.t(
                            "Register Shop",
                            "Đăng ký mở Shop",
                            "Créer la boutique",
                            "ショップを登録",
                            "쇼핑 등록",
                            "注册店铺"
                        ),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
