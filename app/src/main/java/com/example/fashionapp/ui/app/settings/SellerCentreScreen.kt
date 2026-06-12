package com.example.fashionapp.ui.app.settings

import android.net.Uri
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
import com.example.fashionapp.ui.theme.AppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerCentreScreen(
    onBack: () -> Unit
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

    // Read-only stats
    var followerCount by remember { mutableStateOf(0) }
    var rating by remember { mutableStateOf(0.0) }
    var productCount by remember { mutableStateOf(0) }

    // States
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val inputBgColor = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF5F5F5)

    // Load existing shop details
    LaunchedEffect(uid) {
        if (uid.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("shops").document(uid).get().await()
            if (doc.exists()) {
                shopName = doc.getString("name").orEmpty()
                description = doc.getString("description").orEmpty()
                location = doc.getString("location").orEmpty()
                
                followerCount = doc.getLong("followerCount")?.toInt() ?: 0
                rating = doc.getDouble("rating") ?: 0.0
                productCount = doc.getLong("productCount")?.toInt() ?: 0
            } else {
                errorMessage = settings.t(
                    "Shop not found",
                    "Không tìm thấy thông tin cửa hàng",
                    "Boutique introuvable",
                    "ショップが見つかりません",
                    "매장을 찾을 수 없습니다",
                    "找不到店铺"
                )
            }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to load shop details"
        } finally {
            isLoading = false
        }
    }

    Scaffold(containerColor = bgColor) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))

            // Back arrow & Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = settings.t("Back", "Quay lại"),
                        tint = textColor
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = settings.t("Seller Centre", "Kênh Người Bán", "Centre Vendeur", "販売者センター", "판매자 센터", "卖家中心"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                // ── Shop Stats Overview (Premium Look) ──
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 20.dp, vertical = 8.dp),
//                    shape = RoundedCornerShape(16.dp),
//                    colors = CardDefaults.cardColors(containerColor = cardColor),
//                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//                ) {
//                    Column(modifier = Modifier.padding(16.dp)) {
//                        Text(
//                            text = settings.t("Shop Stats", "Thống kê Cửa hàng", "Stats de la boutique", "ショップ統計", "상점 통계", "店铺统计"),
//                            fontSize = 15.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = textColor
//                        )
//                        Spacer(Modifier.height(12.dp))
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceAround
//                        ) {
//                            StatItem(
//                                label = settings.t("Followers", "Người theo dõi", "Abonnés", "フォロワー", "팔로워", "粉丝"),
//                                value = followerCount.toString(),
//                                textColor = textColor,
//                                subTextColor = subTextColor
//                            )
//                            StatItem(
//                                label = settings.t("Rating", "Đánh giá", "Note", "評価", "평점", "评分"),
//                                value = "${"%.1f".format(rating)} ⭐",
//                                textColor = textColor,
//                                subTextColor = subTextColor
//                            )
//                            StatItem(
//                                label = settings.t("Products", "Sản phẩm", "Produits", "商品数", "상품 수", "商品数"),
//                                value = productCount.toString(),
//                                textColor = textColor,
//                                subTextColor = subTextColor
//                            )
//                        }
//                    }
//                }
//
//                Spacer(Modifier.height(16.dp))

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
                    onValueChange = { shopName = it; successMessage = null; errorMessage = null },
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
                    onValueChange = { description = it; successMessage = null; errorMessage = null },
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
                    onValueChange = { location = it; successMessage = null; errorMessage = null },
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

                Spacer(Modifier.height(16.dp))

                // Error Message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFE53935),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }

                // Success Message
                if (successMessage != null) {
                    Text(
                        text = successMessage ?: "",
                        color = Color(0xFF2E7D32),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }

                Spacer(Modifier.weight(1f))

                // Save button
                Button(
                    onClick = {
                        if (shopName.isBlank()) {
                            errorMessage = settings.t("Please enter a shop name", "Vui lòng nhập tên cửa hàng", "Veuillez entrer un nom", "ショップ名を入力してください", "매장 이름을 입력해주세요", "请输入店铺名称")
                            return@Button
                        }
                        if (description.isBlank()) {
                            errorMessage = settings.t("Please enter a description", "Vui lòng nhập mô tả", "Veuillez entrer une description", "説明を入力してください", "설명을 입력해주세요", "请输入描述")
                            return@Button
                        }
                        if (location.isBlank()) {
                            errorMessage = settings.t("Please enter a location", "Vui lòng nhập địa chỉ", "Veuillez entrer un emplacement", "所在地を入力してください", "위치를 입력해주세요", "请输入位置")
                            return@Button
                        }

                        isSaving = true
                        errorMessage = null
                        successMessage = null

                        scope.launch {
                            try {
                                val db = FirebaseFirestore.getInstance()

                                // Update shop details
                                db.collection("shops").document(uid)
                                    .update(
                                        mapOf(
                                            "name" to shopName,
                                            "description" to description,
                                            "location" to location,
                                            "updatedAt" to com.google.firebase.Timestamp.now()
                                        )
                                    ).await()

                                // Sync user name as well
                                db.collection("users").document(uid)
                                    .update("name", shopName)
                                    .await()

                                // Update UserSession in-memory state
                                val currentUser = UserSession.currentUser.value
                                if (currentUser != null) {
                                    UserSession.updateCurrentUser(
                                        currentUser.copy(name = shopName)
                                    )
                                }

                                successMessage = settings.t(
                                    "Shop details updated successfully!",
                                    "Cập nhật thông tin cửa hàng thành công!",
                                    "Boutique mise à jour avec succès !",
                                    "ショップ情報が正常に更新されました！",
                                    "매장 정보가 성공적으로 업데이트되었습니다!",
                                    "店铺信息更新成功！"
                                )
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: "Failed to save changes"
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = settings.t(
                                "Save Changes",
                                "Lưu thay đổi",
                                "Enregistrer",
                                "変更を保存",
                                "변경 사항 저장",
                                "保存修改"
                            ),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    textColor: Color,
    subTextColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = subTextColor
        )
    }
}
