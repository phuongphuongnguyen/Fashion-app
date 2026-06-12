package com.example.fashionapp.ui.app.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fashionapp.data.StorageUrlResolver
import com.example.fashionapp.data.user.UserSession
import com.example.fashionapp.model.User
import com.example.fashionapp.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ── Palette ──
private val PrimaryBlue   = Color(0xFF3669C9)
private val LightBlue     = Color(0xFFEEF2FF)
private val DarkSelectionBg = Color(0xFF1D2D50)
private val DangerRed     = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, initialSubScreen: String? = null) {
    val settings = LocalAppSettings.current
    val sessionUser by UserSession.currentUser.collectAsState()
    val scrollState = rememberScrollState()

    // sub-screen routing inside Settings
    var currentSubScreen by remember {
        mutableStateOf(
            initialSubScreen?.uppercase()?.let {
                try { SubScreen.valueOf(it) } catch(e: Exception) { null }
            }
        )
    }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // Dynamic Theme Colors
    val isDark = settings.isDarkMode
    val bgColor = if (isDark) Color(0xFF121212) else Color(0xFFF7F8FA)
    val cardColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1A1A2E)
    val subTextColor = if (isDark) Color(0xFFB0B0B0) else Color.Gray
    val dividerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE)
    val topBarBgColor = if (isDark) Color(0xFF1E1E1E) else Color.White

    AnimatedContent(
        targetState = currentSubScreen,
        transitionSpec = {
            if (targetState != null) {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            } else {
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            }
        },
        label = "settings_nav"
    ) { subScreen ->
        when (subScreen) {
            SubScreen.PROFILE -> ProfileSubScreen(
                onBack = { currentSubScreen = null },
                isDark = isDark,
                bgColor = bgColor,
                cardColor = cardColor,
                textColor = textColor,
                subTextColor = subTextColor,
                dividerColor = dividerColor
            )
            SubScreen.SHIPPING -> ShippingSubScreen(
                onBack = {
                    if (initialSubScreen?.uppercase() == "SHIPPING") {
                        navController.popBackStack()
                    } else {
                        currentSubScreen = null
                    }
                },
                isDark = isDark,
                bgColor = bgColor,
                cardColor = cardColor,
                textColor = textColor,
                subTextColor = subTextColor,
                dividerColor = dividerColor
            )
            SubScreen.PAYMENT -> PaymentSubScreen(
                onBack = { currentSubScreen = null },
                isDark = isDark,
                bgColor = bgColor,
                cardColor = cardColor,
                textColor = textColor,
                subTextColor = subTextColor,
                dividerColor = dividerColor
            )
            SubScreen.LANGUAGE   -> LanguageScreen(
                onBack = { currentSubScreen = null },
                isDark = isDark,
                bgColor = bgColor,
                cardColor = cardColor,
                textColor = textColor,
                subTextColor = subTextColor,
                dividerColor = dividerColor,
                topBarBgColor = topBarBgColor
            )
            SubScreen.COUNTRY    -> CountryScreen(
                onBack = { currentSubScreen = null },
                isDark = isDark,
                bgColor = bgColor,
                cardColor = cardColor,
                textColor = textColor,
                subTextColor = subTextColor,
                dividerColor = dividerColor,
                topBarBgColor = topBarBgColor
            )
            SubScreen.NOTIFICATIONS -> NotificationScreen(
                onBack = { currentSubScreen = null },
                isDark = isDark,
                bgColor = bgColor,
                cardColor = cardColor,
                textColor = textColor,
                subTextColor = subTextColor,
                dividerColor = dividerColor,
                topBarBgColor = topBarBgColor
            )
            SubScreen.ABOUT      -> AboutScreen(
                onBack = { currentSubScreen = null },
                isDark = isDark,
                bgColor = bgColor,
                cardColor = cardColor,
                textColor = textColor,
                subTextColor = subTextColor,
                dividerColor = dividerColor,
                topBarBgColor = topBarBgColor
            )
            SubScreen.REGISTER_SHOP -> RegisterShopScreen(
                onBack = { currentSubScreen = null },
                onRegistered = { currentSubScreen = null },
                isDark = isDark,
                bgColor = bgColor,
                cardColor = cardColor,
                textColor = textColor,
                subTextColor = subTextColor,
                dividerColor = dividerColor
            )
            SubScreen.SELLER_CENTRE -> SellerCentreScreen(
                onBack = { currentSubScreen = null },
                isDark = isDark,
                bgColor = bgColor,
                cardColor = cardColor,
                textColor = textColor,
                subTextColor = subTextColor,
                dividerColor = dividerColor
            )
            null -> {
                // ── Main settings list ──
                Scaffold(
                    containerColor = bgColor
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(scrollState)
                    ) {
                        Spacer(Modifier.height(12.dp))

                        // ── Back arrow ──
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textColor
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // ── Title ──
                        Text(
                            text = settings.t("Settings", "Cài đặt", "Paramètres", "設定", "설정", "设置"),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )

                        Spacer(Modifier.height(28.dp))

                        // ══ Personal ══
                        FlatSectionHeader(
                            title = settings.t("Personal", "Cá nhân", "Personnel", "個人", "개인", "个人"),
                            textColor = textColor
                        )
                        FlatSettingRow(
                            label = settings.t("Profile", "Hồ sơ", "Profil", "プロフィール", "프로필", "个人资料"),
                            onClick = { currentSubScreen = SubScreen.PROFILE },
                            textColor = textColor, subTextColor = subTextColor,
                            dividerColor = dividerColor
                        )
                        FlatSettingRow(
                            label = settings.t("Shipping Address", "Địa chỉ giao hàng", "Adresse de livraison", "配送先住所", "배송 주소", "收货地址"),
                            onClick = { currentSubScreen = SubScreen.SHIPPING },
                            textColor = textColor, subTextColor = subTextColor,
                            dividerColor = dividerColor
                        )
                        FlatSettingRow(
                            label = settings.t("Payment methods", "Phương thức thanh toán", "Modes de paiement", "支払い方法", "결제 수단", "支付方式"),
                            onClick = { currentSubScreen = SubScreen.PAYMENT },
                            textColor = textColor, subTextColor = subTextColor,
                            dividerColor = dividerColor
                        )
                        FlatSettingRow(
                            label = settings.t("Notifications", "Thông báo", "Notifications", "通知", "알림", "通知"),
                            onClick = { currentSubScreen = SubScreen.NOTIFICATIONS },
                            textColor = textColor, subTextColor = subTextColor,
                            dividerColor = dividerColor
                        )
                        // ── Dark Mode toggle in Personal ──
                        FlatSwitchRow(
                            label = settings.t("Dark Mode", "Chế độ tối", "Mode sombre", "ダークモード", "다크 모드", "深色模式"),
                            checked = settings.isDarkMode,
                            onCheckedChange = { settings.updateDarkMode(it) },
                            textColor = textColor,
                            dividerColor = dividerColor
                        )

                        Spacer(Modifier.height(24.dp))

                        // ══ Shop ══
                        FlatSectionHeader(
                            title = settings.t("Shop", "Cửa hàng", "Boutique", "ショップ", "쇼핑", "商店"),
                            textColor = textColor
                        )
                        if (sessionUser?.role?.equals("shop", ignoreCase = true) == true) {
                            FlatSettingRow(
                                label = settings.t("Seller Centre", "Kênh Người Bán", "Centre Vendeur", "販売者センター", "판매자 센터", "卖家中心"),
                                onClick = { currentSubScreen = SubScreen.SELLER_CENTRE },
                                textColor = textColor, subTextColor = subTextColor,
                                dividerColor = dividerColor
                            )
                        } else {
                            FlatSettingRow(
                                label = settings.t("Register as a Seller", "Đăng ký mở Cửa hàng", "S'inscrire comme vendeur", "販売者として登録", "판매자로 등록", "注册为卖家"),
                                onClick = { currentSubScreen = SubScreen.REGISTER_SHOP },
                                textColor = textColor, subTextColor = subTextColor,
                                dividerColor = dividerColor
                            )
                        }
                        FlatSettingRow(
                            label = settings.t("Country", "Quốc gia", "Pays", "国", "국가", "国家"),
                            value = settings.country,
                            onClick = { currentSubScreen = SubScreen.COUNTRY },
                            textColor = textColor, subTextColor = subTextColor,
                            dividerColor = dividerColor
                        )
                        FlatSettingRow(
                            label = settings.t("Terms and Conditions", "Điều khoản và Điều kiện", "Conditions générales", "利用規約", "이용 약관", "条款与条件"),
                            onClick = { /* Show T&C */ },
                            textColor = textColor, subTextColor = subTextColor,
                            dividerColor = dividerColor
                        )

                        Spacer(Modifier.height(24.dp))

                        // ══ Account ══
                        FlatSectionHeader(
                            title = settings.t("Account", "Tài khoản", "Compte", "アカウント", "계정", "账户"),
                            textColor = textColor
                        )
                        FlatSettingRow(
                            label = settings.t("Language", "Ngôn ngữ", "Langue", "言語", "언어", "语言"),
                            value = settings.language.nativeName,
                            onClick = { currentSubScreen = SubScreen.LANGUAGE },
                            textColor = textColor, subTextColor = subTextColor,
                            dividerColor = dividerColor
                        )
                        FlatSettingRow(
                            label = settings.t("About app", "Về app", "À propos de app", "Appについて", "App 정보", "关于 App"),
                            onClick = { currentSubScreen = SubScreen.ABOUT },
                            textColor = textColor, subTextColor = subTextColor,
                            dividerColor = dividerColor
                        )
                        FlatSettingRow(
                            label = settings.t("Log out", "Đăng xuất", "Se déconnecter", "ログアウト", "로그아웃", "退出登录"),
                            onClick = {
                                showLogoutConfirmDialog = true
                            },
                            textColor = textColor, subTextColor = subTextColor,
                            dividerColor = dividerColor
                        )

                        Spacer(Modifier.height(20.dp))

                        // ── Delete Account ──
                        Text(
                            text = settings.t("Delete My Account", "Xóa tài khoản của tôi", "Supprimer mon compte", "アカウントを削除", "계정 삭제", "删除我的账户"),
                            color = DangerRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier
                                .clickable { showDeleteConfirmDialog = true }
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        )

                        Spacer(Modifier.height(24.dp))

                        // ── Version info ──
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 32.dp)
                        ) {
                            Text(
                                "Fashion app",
                                fontSize = 18.sp,
                                color = textColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                settings.t("Version 1.0 April, 2020", "Phiên bản 1.0 Tháng 4, 2020", "Version 1.0 Avril 2020", "バージョン 1.0 2020年4月", "버전 1.0 2020년 4월", "版本 1.0 2020年4月"),
                                fontSize = 13.sp,
                                color = subTextColor
                            )
                        }
                    }

                    if (showDeleteConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirmDialog = false },
                            title = {
                                Text(
                                    text = settings.t("Delete Account?", "Xóa tài khoản?", "Supprimer le compte?", "アカウントを削除しますか？", "계정을 삭제하시겠습니까?", "删除账户？"),
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            },
                            text = {
                                Text(
                                    text = settings.t(
                                        "Are you sure you want to delete your account? This action is permanent and cannot be undone.",
                                        "Bạn có chắc chắn muốn xóa tài khoản không? Hành động này là vĩnh viễn và không thể hoàn tác.",
                                        "Êtes-vous sûr de vouloir supprimer votre compte? Cette action est irréversible.",
                                        "アカウントを削除してもよろしいですか？この操作は取り消せません。",
                                        "계정을 삭제하시겠습니까? 이 작업은 영구적이며 취소할 수 없습니다.",
                                        "您确定要删除账户吗？此操作是永久性的，且无法撤销。"
                                    ),
                                    color = textColor
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showDeleteConfirmDialog = false
                                        FirebaseAuth.getInstance().currentUser?.delete()?.addOnCompleteListener {
                                            FirebaseAuth.getInstance().signOut()
                                            navController.navigate(Screen.Start.route) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = settings.t("Delete", "Xóa", "Supprimer", "削除", "삭제", "删除"),
                                        color = DangerRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                                    Text(
                                        text = settings.t("Cancel", "Hủy", "Annuler", "キャンセル", "취소", "取消"),
                                        color = subTextColor
                                    )
                                }
                            },
                            containerColor = cardColor
                        )
                    }

                    if (showLogoutConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showLogoutConfirmDialog = false },
                            title = {
                                Text(
                                    text = settings.t("Log out?", "Đăng xuất?", "Se déconnecter?", "ログアウトしますか？", "로그아웃하시겠습니까?", "确定退出登录？"),
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            },
                            text = {
                                Text(
                                    text = settings.t(
                                        "Are you sure you want to log out of your account?",
                                        "Bạn có chắc chắn muốn đăng xuất khỏi tài khoản của mình không?",
                                        "Êtes-vous sûr de vouloir vous déconnecter de votre compte?",
                                        "アカウントからログアウトしてもよろしいですか？",
                                        "계정에서 로그아웃하시겠습니까?",
                                        "您确定要退出当前账户吗？"
                                    ),
                                    color = textColor
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showLogoutConfirmDialog = false
                                        FirebaseAuth.getInstance().signOut()
                                        UserSession.clear()
                                        navController.navigate(Screen.Start.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = settings.t("Log out", "Đăng xuất", "Se déconnecter", "ログアウト", "로그아웃", "退出登录"),
                                        color = DangerRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                                    Text(
                                        text = settings.t("Cancel", "Hủy", "Annuler", "キャンセル", "취소", "取消"),
                                        color = subTextColor
                                    )
                                }
                            },
                            containerColor = cardColor
                        )
                    }
                }
            }
        }
    }
}

// ── Sub-screen enum ──
private enum class SubScreen { PROFILE, SHIPPING, PAYMENT, LANGUAGE, COUNTRY, NOTIFICATIONS, ABOUT, REGISTER_SHOP, SELLER_CENTRE }

// ══════════════════════════════════════════
// ── Sub Screens ──
// ══════════════════════════════════════════

// ── Profile Sub-Screen ──
@Composable
private fun ProfileSubScreen(
    onBack: () -> Unit,
    isDark: Boolean,
    bgColor: Color,
    cardColor: Color,
    textColor: Color,
    subTextColor: Color,
    dividerColor: Color
) {
    val settings = LocalAppSettings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    // Đọc displayName & avatar từ UserSession (Firestore) để hiển thị đúng
    val sessionUser by UserSession.currentUser.collectAsState()
    var displayName by remember { mutableStateOf(sessionUser?.name ?: user?.displayName ?: "") }
    val email = user?.email ?: ""

    // photoUri: null = dùng avatar hiện tại từ Firestore, non-null = ảnh mới user vừa chọn
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    val currentAvatarUrl = sessionUser?.avatarUrl.orEmpty()

    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) pickedUri = uri }

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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
            }

            // Title
            Text(
                text = settings.t("Settings", "Cài đặt", "Paramètres", "設定", "설정", "设置"),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Text(
                text = settings.t("Your Profile", "Hồ sơ của bạn", "Votre profil", "あなたのプロフィール", "내 프로필", "您的资料"),
                fontSize = 14.sp,
                color = subTextColor,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .size(90.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                // Hiện ảnh mới pick hoặc ảnh hiện tại từ Firestore
                val avatarModel: Any? = pickedUri ?: currentAvatarUrl.takeIf { it.isNotBlank() }
                if (avatarModel != null) {
                    AsyncImage(
                        model = avatarModel,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(2.dp, PrimaryBlue, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF2A2A2A) else Color(0xFFE0E0E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = subTextColor,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                // Edit badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Change photo",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Name field
            FlatTextField(
                value = displayName,
                onValueChange = { displayName = it },
                placeholder = settings.t("Name", "Tên", "Nom", "名前", "이름", "姓名"),
                textColor = textColor,
                subTextColor = subTextColor,
                dividerColor = dividerColor
            )

            // Email field (read-only)
            FlatTextField(
                value = email,
                onValueChange = {},
                placeholder = settings.t("Email", "Email", "Email", "メール", "이메일", "邮箱"),
                textColor = subTextColor,
                subTextColor = subTextColor,
                dividerColor = dividerColor,
                readOnly = true
            )

            // Password field (masked, read-only)
            FlatTextField(
                value = "••••••••••",
                onValueChange = {},
                placeholder = settings.t("Password", "Mật khẩu", "Mot de passe", "パスワード", "비밀번호", "密码"),
                textColor = subTextColor,
                subTextColor = subTextColor,
                dividerColor = dividerColor,
                readOnly = true
            )

            if (saveError != null) {
                Text(
                    text = saveError ?: "",
                    color = DangerRed,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            if (showSuccess) {
                Text(
                    text = settings.t("Saved successfully!", "Đã lưu thành công!", "Enregistré!", "保存しました!", "저장됨!", "已保存!"),
                    color = Color(0xFF2E7D32),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            // Save button
            Button(
                onClick = {
                    val uid = user?.uid ?: return@Button
                    isSaving = true
                    saveError = null
                    showSuccess = false
                    scope.launch {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val storage = FirebaseStorage.getInstance()

                            // 1. Upload ảnh mới lên Storage nếu user chọn ảnh mới.
                            //    Firestore chỉ lưu avatarRef = storage path; URL hiển thị resolve khi đọc.
                            var newAvatarRef: String? = null
                            var displayAvatarUrl: String = currentAvatarUrl
                            if (pickedUri != null) {
                                val storagePath = "avatar/$uid.jpg"
                                val ref = storage.reference.child(storagePath)
                                ref.putFile(pickedUri!!).await()
                                // Xoá cache URL cũ để StorageUrlResolver lấy URL mới
                                StorageUrlResolver.clearCache()
                                newAvatarRef = storagePath
                                displayAvatarUrl = ref.downloadUrl.await().toString()
                            }

                            // 2. Cập nhật Firestore users/{uid}
                            val updates = mutableMapOf<String, Any>("name" to displayName)
                            if (newAvatarRef != null) {
                                updates["avatarRef"] = newAvatarRef
                            }
                            db.collection("users").document(uid)
                                .update(updates as Map<String, Any>)
                                .await()

                            // 3. Cập nhật UserSession để các màn hình khác thấy ngay
                            val updatedUser = (sessionUser ?: User(id = uid)).copy(
                                name = displayName,
                                avatarUrl = displayAvatarUrl.ifBlank { currentAvatarUrl }
                            )
                            UserSession.updateCurrentUser(updatedUser)

                            // 4. Cập nhật Firebase Auth displayName
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName)
                                .build()
                            user.updateProfile(profileUpdates).await()

                            pickedUri = null
                            showSuccess = true
                        } catch (e: Exception) {
                            saveError = e.localizedMessage ?: "Error saving profile"
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
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        settings.t("Save Changes", "Lưu thay đổi", "Enregistrer", "保存", "저장", "保存"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── Shipping Address Sub-Screen ──
@Composable
private fun ShippingSubScreen(
    onBack: () -> Unit,
    isDark: Boolean,
    bgColor: Color,
    cardColor: Color,
    textColor: Color,
    subTextColor: Color,
    dividerColor: Color
) {
    val settings = LocalAppSettings.current
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
    
    val sessionUser by UserSession.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    
    var fullName by remember { mutableStateOf(sessionUser?.name ?: prefs.getString("ship_name", "") ?: "") }
    var address by remember { mutableStateOf(sessionUser?.address ?: prefs.getString("ship_address", "") ?: "") }
    var city by remember { mutableStateOf(prefs.getString("ship_city", "") ?: "") }
    var zipCode by remember { mutableStateOf(prefs.getString("ship_zip", "") ?: "") }
    var phone by remember { mutableStateOf(sessionUser?.phoneNumber ?: prefs.getString("ship_phone", "") ?: "") }
    var saved by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(sessionUser) {
        sessionUser?.let {
            if (fullName.isBlank()) fullName = it.name
            if (address.isBlank()) address = it.address
            if (phone.isBlank()) phone = it.phoneNumber
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

            IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
            }

            Text(
                text = settings.t("Settings", "Cài đặt", "Paramètres", "設定", "설정", "设置"),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Text(
                text = settings.t("Shipping Address", "Địa chỉ giao hàng", "Adresse de livraison", "配送先住所", "배송 주소", "收货地址"),
                fontSize = 14.sp,
                color = subTextColor,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Spacer(Modifier.height(24.dp))

            FlatTextField(value = fullName, onValueChange = { fullName = it; saved = false },
                placeholder = settings.t("Full Name", "Họ và tên", "Nom complet", "氏名", "이름", "全名"),
                textColor = textColor, subTextColor = subTextColor, dividerColor = dividerColor)
            FlatTextField(value = address, onValueChange = { address = it; saved = false },
                placeholder = settings.t("Address", "Địa chỉ", "Adresse", "住所", "주소", "地址"),
                textColor = textColor, subTextColor = subTextColor, dividerColor = dividerColor)
            FlatTextField(value = city, onValueChange = { city = it; saved = false },
                placeholder = settings.t("City", "Thành phố", "Ville", "都市", "도시", "城市"),
                textColor = textColor, subTextColor = subTextColor, dividerColor = dividerColor)
            FlatTextField(value = zipCode, onValueChange = { zipCode = it; saved = false },
                placeholder = settings.t("Zip Code", "Mã bưu điện", "Code postal", "郵便番号", "우편번호", "邮编"),
                textColor = textColor, subTextColor = subTextColor, dividerColor = dividerColor,
                keyboardType = KeyboardType.Number)
            FlatTextField(value = phone, onValueChange = { phone = it; saved = false },
                placeholder = settings.t("Phone Number", "Số điện thoại", "Téléphone", "電話番号", "전화번호", "电话号码"),
                textColor = textColor, subTextColor = subTextColor, dividerColor = dividerColor,
                keyboardType = KeyboardType.Phone)

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@Button
                    isSaving = true
                    scope.launch {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val updates = mapOf<String, Any>(
                                "name" to fullName,
                                "address" to address,
                                "phoneNumber" to phone
                            )
                            db.collection("users").document(uid)
                                .update(updates)
                                .await()

                            val updatedUser = (sessionUser ?: User(id = uid)).copy(
                                name = fullName,
                                address = address,
                                phoneNumber = phone
                            )
                            UserSession.updateCurrentUser(updatedUser)

                            prefs.edit()
                                .putString("ship_name", fullName)
                                .putString("ship_address", address)
                                .putString("ship_city", city)
                                .putString("ship_zip", zipCode)
                                .putString("ship_phone", phone)
                                .apply()

                            saved = true
                        } catch (e: Exception) {
                            // Handle error
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
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        if (saved) settings.t("Saved ✓", "Đã lưu ✓", "Enregistré ✓", "保存済み ✓", "저장됨 ✓", "已保存 ✓")
                        else settings.t("Save Address", "Lưu địa chỉ", "Enregistrer", "保存", "저장", "保存地址"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── Payment Method Sub-Screen ──
@Composable
private fun PaymentSubScreen(
    onBack: () -> Unit,
    isDark: Boolean,
    bgColor: Color,
    cardColor: Color,
    textColor: Color,
    subTextColor: Color,
    dividerColor: Color
) {
    val settings = LocalAppSettings.current
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
    var cardNumber by remember { mutableStateOf(prefs.getString("pay_card", "") ?: "") }
    var cardHolder by remember { mutableStateOf(prefs.getString("pay_holder", "") ?: "") }
    var expiry by remember { mutableStateOf(prefs.getString("pay_expiry", "") ?: "") }
    var cvv by remember { mutableStateOf(prefs.getString("pay_cvv", "") ?: "") }
    var saved by remember { mutableStateOf(false) }

    Scaffold(containerColor = bgColor) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))

            IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
            }

            Text(
                text = settings.t("Settings", "Cài đặt", "Paramètres", "設定", "설정", "设置"),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Text(
                text = settings.t("Payment Methods", "Phương thức thanh toán", "Modes de paiement", "支払い方法", "결제 수단", "支付方式"),
                fontSize = 14.sp,
                color = subTextColor,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Spacer(Modifier.height(24.dp))

            FlatTextField(value = cardNumber, onValueChange = { cardNumber = it },
                placeholder = settings.t("Card Number", "Số thẻ", "Numéro de carte", "カード番号", "카드 번호", "卡号"),
                textColor = textColor, subTextColor = subTextColor, dividerColor = dividerColor,
                keyboardType = KeyboardType.Number)
            FlatTextField(value = cardHolder, onValueChange = { cardHolder = it },
                placeholder = settings.t("Card Holder Name", "Tên chủ thẻ", "Nom du titulaire", "カード名義人", "카드 소유자", "持卡人姓名"),
                textColor = textColor, subTextColor = subTextColor, dividerColor = dividerColor)
            FlatTextField(value = expiry, onValueChange = { expiry = it },
                placeholder = settings.t("Expiry Date (MM/YY)", "Ngày hết hạn (MM/YY)", "Date d'expiration", "有効期限", "만료일", "有效期"),
                textColor = textColor, subTextColor = subTextColor, dividerColor = dividerColor)
            FlatTextField(value = cvv, onValueChange = { cvv = it },
                placeholder = "CVV",
                textColor = textColor, subTextColor = subTextColor, dividerColor = dividerColor,
                keyboardType = KeyboardType.NumberPassword)

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    prefs.edit()
                        .putString("pay_card", cardNumber)
                        .putString("pay_holder", cardHolder)
                        .putString("pay_expiry", expiry)
                        .putString("pay_cvv", cvv)
                        .apply()
                    saved = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(
                    if (saved) settings.t("Saved ✓", "Đã lưu ✓", "Enregistré ✓", "保存済み ✓", "저장됨 ✓", "已保存 ✓")
                    else settings.t("Save Payment", "Lưu thanh toán", "Enregistrer", "保存", "저장", "保存"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


@Composable
private fun LanguageScreen(
    onBack: () -> Unit, isDark: Boolean, bgColor: Color, cardColor: Color,
    textColor: Color, subTextColor: Color, dividerColor: Color, topBarBgColor: Color
) {
    val settings = LocalAppSettings.current
    Scaffold(containerColor = bgColor) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                FlatSubScreenHeader(
                    onBack = onBack,
                    title = settings.t("Settings", "Cài đặt", "Paramètres", "設定", "설정", "设置"),
                    subtitle = settings.t("Language", "Ngôn ngữ", "Langue", "言語", "언어", "语言"),
                    textColor = textColor, subTextColor = subTextColor
                )
            }
            items(AppLanguage.values()) { lang ->
                val selected = settings.language == lang
                FlatSelectableRow(
                    selected = selected,
                    onClick = { settings.updateLanguage(lang) },
                    textColor = textColor, subTextColor = subTextColor, dividerColor = dividerColor
                ) {
                    Text(lang.flag, fontSize = 24.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(lang.nativeName, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = textColor)
                        Text(lang.displayName, fontSize = 13.sp, color = subTextColor)
                    }
                    if (selected) {
                        Icon(Icons.Default.CheckCircle, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyScreen(
    onBack: () -> Unit, isDark: Boolean, bgColor: Color, cardColor: Color,
    textColor: Color, subTextColor: Color, dividerColor: Color, topBarBgColor: Color
) {
    val settings = LocalAppSettings.current
    Scaffold(containerColor = bgColor) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                FlatSubScreenHeader(
                    onBack = onBack,
                    title = settings.t("Settings", "Cài đặt", "Paramètres", "設定", "설정", "设置"),
                    subtitle = settings.t("Currency", "Tiền tệ", "Devise", "通貨", "통화", "货币"),
                    textColor = textColor, subTextColor = subTextColor
                )
            }
            items(AppCurrency.values()) { cur ->
                val selected = settings.currency == cur
                FlatSelectableRow(
                    selected = selected,
                    onClick = { settings.updateCurrency(cur) },
                    textColor = textColor, subTextColor = subTextColor, dividerColor = dividerColor
                ) {
                    Text(cur.symbol, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(cur.displayName, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = textColor)
                        Text(cur.code, fontSize = 13.sp, color = subTextColor)
                    }
                    if (selected) {
                        Icon(Icons.Default.CheckCircle, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SizeSystemScreen(
    onBack: () -> Unit, isDark: Boolean, bgColor: Color, cardColor: Color,
    textColor: Color, subTextColor: Color, dividerColor: Color, topBarBgColor: Color
) {
    val settings = LocalAppSettings.current
    val descriptions = mapOf(
        SizeSystem.UK to settings.t("Standard UK sizing (e.g. 6, 8, 10)", "Bảng size chuẩn Anh (ví dụ: 6, 8, 10)", "Taille standard Royaume-Uni (ex. 6, 8, 10)", "英国標準サイズ (例 6, 8, 10)", "영국 표준 사이즈 (예: 6, 8, 10)", "英国标准尺码 (例如 6, 8, 10)"),
        SizeSystem.US to settings.t("Standard US sizing (e.g. 2, 4, 6)", "Bảng size chuẩn Mỹ (ví dụ: 2, 4, 6)", "Taille standard États-Unis (ex. 2, 4, 6)", "米国標準サイズ (例 2, 4, 6)", "미국 표준 사이즈 (예: 2, 4, 6)", "美国标准尺码 (例如 2, 4, 6)"),
        SizeSystem.EU to settings.t("European sizing (e.g. 36, 38, 40)", "Bảng size chuẩn châu Âu (ví dụ: 36, 38, 40)", "Taille européenne (ex. 36, 38, 40)", "欧州サイズ (例 36, 38, 40)", "유럽 사이즈 (예: 36, 38, 40)", "欧洲尺码 (例如 36, 38, 40)"),
        SizeSystem.ASIAN to settings.t("Asian sizing (e.g. S, M, L, XL)", "Bảng size chuẩn châu Á (ví dụ: S, M, L, XL)", "Taille asiatique (ex. S, M, L, XL)", "アジアサイズ (例 S, M, L, XL)", "아시아 사이즈 (예: S, M, L, XL)", "亚洲尺码 (例如 S, M, L, XL)")
    )
    Scaffold(containerColor = bgColor) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            FlatSubScreenHeader(
                onBack = onBack,
                title = settings.t("Settings", "Cài đặt", "Paramètres", "設定", "설정", "设置"),
                subtitle = settings.t("Size System", "Hệ size", "Système de taille", "サイズ表記", "사이즈 체계", "尺码系统"),
                textColor = textColor, subTextColor = subTextColor
            )
            SizeSystem.values().forEach { size ->
                val selected = settings.sizeSystem == size
                FlatSelectableRow(
                    selected = selected,
                    onClick = { settings.updateSizeSystem(size) },
                    textColor = textColor, subTextColor = subTextColor, dividerColor = dividerColor
                ) {
                    Text(size.label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${size.label} Sizes", fontWeight = FontWeight.Medium, fontSize = 15.sp, color = textColor)
                        Text(descriptions[size] ?: "", fontSize = 12.sp, color = subTextColor)
                    }
                    if (selected) {
                        Icon(Icons.Default.CheckCircle, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryScreen(
    onBack: () -> Unit, isDark: Boolean, bgColor: Color, cardColor: Color,
    textColor: Color, subTextColor: Color, dividerColor: Color, topBarBgColor: Color
) {
    val settings = LocalAppSettings.current
    var search by remember { mutableStateOf("") }
    val filtered = countryList.filter { it.contains(search, ignoreCase = true) }

    Scaffold(containerColor = bgColor) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            FlatSubScreenHeader(
                onBack = onBack,
                title = settings.t("Settings", "Cài đặt", "Paramètres", "設定", "설정", "设置"),
                subtitle = settings.t("Country", "Quốc gia", "Pays", "国", "국가", "国家"),
                textColor = textColor, subTextColor = subTextColor
            )
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text(settings.t("Search country...", "Tìm quốc gia...", "Rechercher un pays...", "国を検索...", "국가 검색...", "搜索国家...")) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = subTextColor) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = PrimaryBlue.copy(alpha = 0.4f),
                    focusedTextColor = textColor, unfocusedTextColor = textColor,
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                )
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(filtered) { c ->
                    val selected = settings.country == c
                    FlatSelectableRow(
                        selected = selected,
                        onClick = { settings.updateCountry(c) },
                        textColor = textColor, subTextColor = subTextColor, dividerColor = dividerColor
                    ) {
                        Text(c, modifier = Modifier.weight(1f), fontSize = 15.sp, color = textColor)
                        if (selected) {
                            Icon(Icons.Default.CheckCircle, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationScreen(
    onBack: () -> Unit, isDark: Boolean, bgColor: Color, cardColor: Color,
    textColor: Color, subTextColor: Color, dividerColor: Color, topBarBgColor: Color
) {
    val settings = LocalAppSettings.current
    Scaffold(containerColor = bgColor) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            FlatSubScreenHeader(
                onBack = onBack,
                title = settings.t("Settings", "Cài đặt", "Paramètres", "設定", "설정", "设置"),
                subtitle = settings.t("Notifications", "Thông báo", "Notifications", "通知", "알림", "通知"),
                textColor = textColor, subTextColor = subTextColor
            )
            FlatSwitchRow(
                label = settings.t("Enable Notifications", "Bật thông báo", "Activer les notifications", "通知を有効化", "알림 허용", "启用通知"),
                checked = settings.notificationsEnabled,
                onCheckedChange = { settings.updateNotifications(it) },
                textColor = textColor, dividerColor = dividerColor
            )
            AnimatedVisibility(visible = settings.notificationsEnabled) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    FlatSwitchRow(
                        label = settings.t("Device Notifications", "Thông báo thiết bị", "Notifications de l'appareil", "デバイス通知", "기기 알림", "设备通知"),
                        checked = settings.systemNotificationsEnabled,
                        onCheckedChange = { settings.updateSystemNotifications(it) },
                        textColor = textColor, dividerColor = dividerColor
                    )
                    FlatSwitchRow(
                        label = settings.t("Order Updates", "Cập nhật đơn hàng", "Mises à jour de commande", "注文状況の更新", "주문 업데이트", "订单状态更新"),
                        checked = settings.orderUpdatesEnabled,
                        onCheckedChange = { settings.updateOrderUpdates(it) },
                        textColor = textColor, dividerColor = dividerColor
                    )
                    FlatSwitchRow(
                        label = settings.t("Social Interactions", "Tương tác bài viết", "Interactions sociales", "ソーシャルインタラクション", "소셜 상호작용", "社交互动"),
                        checked = settings.socialInteractionsEnabled,
                        onCheckedChange = { settings.updateSocialInteractions(it) },
                        textColor = textColor, dividerColor = dividerColor
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutScreen(
    onBack: () -> Unit, isDark: Boolean, bgColor: Color, cardColor: Color,
    textColor: Color, subTextColor: Color, dividerColor: Color, topBarBgColor: Color
) {
    val settings = LocalAppSettings.current
    Scaffold(containerColor = bgColor) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            FlatSubScreenHeader(
                onBack = onBack,
                title = settings.t("Settings", "Cài đặt", "Paramètres", "設定", "설정", "设置"),
                subtitle = settings.t("About", "Giới thiệu", "À propos", "アプリについて", "정보", "关于"),
                textColor = textColor, subTextColor = subTextColor
            )

            // App info rows
            FlatInfoRow(
                label = settings.t("Developer", "Nhà phát triển", "Développeur", "開発元", "개발자", "开发者"),
                value = "Team7",
                textColor = textColor, subTextColor = subTextColor, dividerColor = dividerColor
            )
            FlatInfoRow(
                label = settings.t("University", "Trường", "Université", "大学", "대학교", "学校"),
                value = "UET",
                textColor = textColor, subTextColor = subTextColor, dividerColor = dividerColor
            )
            FlatInfoRow(
                label = settings.t("Build", "Phiên bản build", "Build", "ビルド", "빌드", "构建版本"),
                value = "2026.05.07",
                textColor = textColor, subTextColor = subTextColor, dividerColor = dividerColor
            )
            FlatInfoRow(
                label = settings.t("Platform", "Nền tảng", "Plateforme", "プラットフォーム", "플랫폼", "平台"),
                value = "Android",
                textColor = textColor, subTextColor = subTextColor, dividerColor = dividerColor
            )

            Spacer(Modifier.height(28.dp))
            Text(
                settings.t(
                    "© 2026 Fashion App. All rights reserved.",
                    "© 2026 Fashion App. Tất cả quyền được bảo lưu.",
                    "© 2026 Fashion App. Tous droits réservés.",
                    "© 2026 Fashion App. All rights reserved.",
                    "© 2026 Fashion App. All rights reserved.",
                    "© 2026 Fashion App. 版权所有。"
                ),
                fontSize = 12.sp,
                color = subTextColor,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

// ══════════════════════════════════════════
// ── Shared UI Components ──
// ══════════════════════════════════════════

@Composable
private fun SettingsSectionCard(
    title: String,
    cardColor: Color,
    subTextColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = title.uppercase(java.util.Locale.ROOT),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = subTextColor,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun ArrowSettingRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String? = null,
    onClick: () -> Unit,
    textColor: Color,
    subTextColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
        if (value != null) {
            Text(value, fontSize = 14.sp, color = subTextColor, modifier = Modifier.padding(end = 4.dp))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = subTextColor.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SwitchSettingRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryBlue)
        )
    }
}

@Composable
private fun SelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    cardColor: Color,
    content: @Composable () -> Unit
) {
    val borderColor = if (selected) PrimaryBlue else Color.Transparent
    val containerColor = if (selected) {
        if (isDark) DarkSelectionBg else LightBlue
    } else {
        cardColor
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 1.dp)
    ) {
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String, textColor: Color, subTextColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = subTextColor)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}

@Composable
private fun RowDivider(dividerColor: Color) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = dividerColor
    )
}

// ── Flat-style components (matching screenshot design) ──

@Composable
private fun FlatSectionHeader(title: String, textColor: Color) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun FlatSettingRow(
    label: String,
    value: String? = null,
    onClick: () -> Unit,
    textColor: Color,
    subTextColor: Color,
    dividerColor: Color
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                color = textColor
            )
            if (value != null) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = subTextColor,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = subTextColor.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            thickness = 0.5.dp,
            color = dividerColor
        )
    }
}

@Composable
private fun FlatSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color,
    dividerColor: Color
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                color = textColor
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryBlue
                )
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            thickness = 0.5.dp,
            color = dividerColor
        )
    }
}

@Composable
private fun FlatTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textColor: Color,
    subTextColor: Color,
    dividerColor: Color,
    readOnly: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = subTextColor, fontSize = 15.sp) },
        readOnly = readOnly,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = textColor),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = PrimaryBlue.copy(alpha = 0.4f),
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

// ── Flat sub-screen header (reusable for all sub-screens) ──
@Composable
private fun FlatSubScreenHeader(
    onBack: () -> Unit,
    title: String,
    subtitle: String,
    textColor: Color,
    subTextColor: Color
) {
    Spacer(Modifier.height(12.dp))
    IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
    }
    Text(
        text = title,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
    Text(
        text = subtitle,
        fontSize = 14.sp,
        color = subTextColor,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun FlatSelectableRow(
    selected: Boolean,
    onClick: () -> Unit,
    textColor: Color,
    subTextColor: Color,
    dividerColor: Color,
    content: @Composable RowScope.() -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            thickness = 0.5.dp,
            color = dividerColor
        )
    }
}

@Composable
private fun FlatInfoRow(
    label: String,
    value: String,
    textColor: Color,
    subTextColor: Color,
    dividerColor: Color
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                fontSize = 15.sp,
                color = subTextColor,
                fontWeight = FontWeight.Medium
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            thickness = 0.5.dp,
            color = dividerColor
        )
    }
}
