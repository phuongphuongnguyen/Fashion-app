package com.example.fashionapp.ui.app.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.example.fashionapp.navigation.Screen

// ── Palette ──
private val PrimaryBlue   = Color(0xFF3669C9)
private val LightBlue     = Color(0xFFEEF2FF)
private val DarkSelectionBg = Color(0xFF1D2D50)
private val DangerRed     = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val settings = LocalAppSettings.current
    val scrollState = rememberScrollState()

    // sub-screen routing inside Settings
    var currentSubScreen by remember { mutableStateOf<SubScreen?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
            SubScreen.CURRENCY   -> CurrencyScreen(
                onBack = { currentSubScreen = null },
                isDark = isDark,
                bgColor = bgColor,
                cardColor = cardColor,
                textColor = textColor,
                subTextColor = subTextColor,
                dividerColor = dividerColor,
                topBarBgColor = topBarBgColor
            )
            SubScreen.SIZE       -> SizeSystemScreen(
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
            null -> {
                // ── Main settings list ──
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    settings.t(
                                        en = "Settings",
                                        vi = "Cài đặt",
                                        fr = "Paramètres",
                                        ja = "設定",
                                        ko = "설정",
                                        zh = "设置"
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = topBarBgColor,
                                titleContentColor = textColor,
                                navigationIconContentColor = textColor
                            )
                        )
                    },
                    containerColor = bgColor
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(scrollState)
                    ) {

                        Spacer(Modifier.height(12.dp))

                        // ── Appearance Section ──
                        SettingsSectionCard(
                            title = settings.t("Appearance", "Giao diện", "Apparence", "外観", "화면 설정", "外观"),
                            cardColor = cardColor,
                            subTextColor = subTextColor
                        ) {
                            SwitchSettingRow(
                                icon = if (settings.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                iconTint = if (settings.isDarkMode) Color(0xFF8B5CF6) else Color(0xFFF59E0B),
                                label = settings.t("Dark Mode", "Chế độ tối", "Mode sombre", "ダークモード", "다크 모드", "深色模式"),
                                checked = settings.isDarkMode,
                                onCheckedChange = { settings.updateDarkMode(it) },
                                textColor = textColor
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // ── Language & Region ──
                        SettingsSectionCard(
                            title = settings.t("Language & Region", "Ngôn ngữ & Vùng", "Langue & Région", "言語と地域", "언어 및 지역", "语言与地区"),
                            cardColor = cardColor,
                            subTextColor = subTextColor
                        ) {
                            ArrowSettingRow(
                                icon = Icons.Default.Language,
                                iconTint = PrimaryBlue,
                                label = settings.t("Language", "Ngôn ngữ", "Langue", "言語", "언어", "语言"),
                                value = "${settings.language.flag} ${settings.language.nativeName}",
                                onClick = { currentSubScreen = SubScreen.LANGUAGE },
                                textColor = textColor,
                                subTextColor = subTextColor
                            )
                            RowDivider(dividerColor)
                            ArrowSettingRow(
                                icon = Icons.Default.Public,
                                iconTint = Color(0xFF10B981),
                                label = settings.t("Country", "Quốc gia", "Pays", "国", "국가", "国家"),
                                value = settings.country,
                                onClick = { currentSubScreen = SubScreen.COUNTRY },
                                textColor = textColor,
                                subTextColor = subTextColor
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // ── Shop Preferences ──
                        SettingsSectionCard(
                            title = settings.t("Shop Preferences", "Tùy chọn mua sắm", "Préférences d'achat", "ショッピング設定", "쇼핑 설정", "购物偏好"),
                            cardColor = cardColor,
                            subTextColor = subTextColor
                        ) {
                            ArrowSettingRow(
                                icon = Icons.Default.AttachMoney,
                                iconTint = Color(0xFF059669),
                                label = settings.t("Currency", "Tiền tệ", "Devise", "通貨", "통화", "货币"),
                                value = "${settings.currency.symbol} ${settings.currency.code}",
                                onClick = { currentSubScreen = SubScreen.CURRENCY },
                                textColor = textColor,
                                subTextColor = subTextColor
                            )
                            RowDivider(dividerColor)
                            ArrowSettingRow(
                                icon = Icons.Default.Straighten,
                                iconTint = Color(0xFFEC4899),
                                label = settings.t("Size System", "Hệ size", "Système de taille", "サイズ表記", "사이즈 체계", "尺码系统"),
                                value = settings.sizeSystem.label,
                                onClick = { currentSubScreen = SubScreen.SIZE },
                                textColor = textColor,
                                subTextColor = subTextColor
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // ── Notifications ──
                        SettingsSectionCard(
                            title = settings.t("Notifications", "Thông báo", "Notifications", "通知", "알림", "通知"),
                            cardColor = cardColor,
                            subTextColor = subTextColor
                        ) {
                            ArrowSettingRow(
                                icon = Icons.Default.Notifications,
                                iconTint = Color(0xFFF59E0B),
                                label = settings.t("Notification Settings", "Cài đặt thông báo", "Réglages de notifications", "通知設定", "알림 설정", "通知 Cài đặt"),
                                value = if (settings.notificationsEnabled)
                                    settings.t("On", "Bật", "Activé", "オン", "켜짐", "开启")
                                else
                                    settings.t("Off", "Tắt", "Désactivé", "オフ", "꺼짐", "关闭"),
                                onClick = { currentSubScreen = SubScreen.NOTIFICATIONS },
                                textColor = textColor,
                                subTextColor = subTextColor
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // ── About ──
                        SettingsSectionCard(
                            title = settings.t("About", "Giới thiệu", "À propos", "アプリについて", "정보", "关于"),
                            cardColor = cardColor,
                            subTextColor = subTextColor
                        ) {
                            ArrowSettingRow(
                                icon = Icons.Default.Info,
                                iconTint = Color(0xFF6366F1),
                                label = settings.t("About Fashion App", "Về Fashion App", "À propos de Fashion App", "Fashion Appについて", "Fashion App 정보", "关于 Fashion App"),
                                onClick = { currentSubScreen = SubScreen.ABOUT },
                                textColor = textColor,
                                subTextColor = subTextColor
                            )
                            RowDivider(dividerColor)
                            ArrowSettingRow(
                                icon = Icons.Default.Description,
                                iconTint = Color(0xFF6366F1),
                                label = settings.t("Terms & Conditions", "Điều khoản & Điều kiện", "Conditions générales", "利用規約", "이용 약관", "条款与条件"),
                                onClick = { /* Show dialog */ },
                                textColor = textColor,
                                subTextColor = subTextColor
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // ── Danger Zone ──
                        SettingsSectionCard(
                            title = settings.t("Account", "Tài khoản", "Compte", "アカウント", "계정", "账户"),
                            cardColor = cardColor,
                            subTextColor = subTextColor
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDeleteConfirmDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = DangerRed,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = settings.t("Delete My Account", "Xóa tài khoản của tôi", "Supprimer mon compte", "アカウントを削除", "계정 삭제", "删除我的账户"),
                                    color = DangerRed,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        // Version info
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Fashion App", fontSize = 13.sp, color = textColor, fontWeight = FontWeight.Bold)
                            Text(
                                settings.t("Version 1.0 · May 2026", "Phiên bản 1.0 · Tháng 5, 2026", "Version 1.0 · Mai 2026", "バージョン 1.0 · 2026年5月", "버전 1.0 · 2026년 5월", "版本 1.0 · 2026年5月"),
                                fontSize = 12.sp, color = subTextColor
                            )
                        }

                        Spacer(Modifier.height(32.dp))
                    }

                    if (showDeleteConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirmDialog = false },
                            title = {
                                Text(
                                    text = settings.t(
                                        "Delete Account?",
                                        "Xóa tài khoản?",
                                        "Supprimer le compte?",
                                        "アカウントを削除しますか？",
                                        "계정을 삭제하시겠습니까?",
                                        "删除账户？"
                                    ),
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
                }
            }
        }
    }
}

// ── Sub-screen enum ──
private enum class SubScreen { LANGUAGE, CURRENCY, SIZE, COUNTRY, NOTIFICATIONS, ABOUT }

// ══════════════════════════════════════════
// ── Sub Screens ──
// ══════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageScreen(
    onBack: () -> Unit,
    isDark: Boolean,
    bgColor: Color,
    cardColor: Color,
    textColor: Color,
    subTextColor: Color,
    dividerColor: Color,
    topBarBgColor: Color
) {
    val settings = LocalAppSettings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settings.t("Language", "Ngôn ngữ", "Langue", "言語", "언어", "语言"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarBgColor,
                    titleContentColor = textColor,
                    navigationIconContentColor = textColor
                )
            )
        },
        containerColor = bgColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AppLanguage.values()) { lang ->
                val selected = settings.language == lang
                SelectableCard(
                    selected = selected,
                    onClick = { settings.updateLanguage(lang) },
                    isDark = isDark,
                    cardColor = cardColor
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(lang.flag, fontSize = 28.sp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(lang.nativeName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = textColor)
                            Text(lang.displayName, fontSize = 13.sp, color = subTextColor)
                        }
                        if (selected) {
                            Icon(Icons.Default.CheckCircle, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyScreen(
    onBack: () -> Unit,
    isDark: Boolean,
    bgColor: Color,
    cardColor: Color,
    textColor: Color,
    subTextColor: Color,
    dividerColor: Color,
    topBarBgColor: Color
) {
    val settings = LocalAppSettings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settings.t("Currency", "Tiền tệ", "Devise", "通貨", "통화", "货币"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarBgColor,
                    titleContentColor = textColor,
                    navigationIconContentColor = textColor
                )
            )
        },
        containerColor = bgColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AppCurrency.values()) { cur ->
                val selected = settings.currency == cur
                SelectableCard(
                    selected = selected,
                    onClick = { settings.updateCurrency(cur) },
                    isDark = isDark,
                    cardColor = cardColor
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E2E4A) else LightBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cur.symbol, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(cur.displayName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = textColor)
                            Text(cur.code, fontSize = 13.sp, color = subTextColor)
                        }
                        if (selected) {
                            Icon(Icons.Default.CheckCircle, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SizeSystemScreen(
    onBack: () -> Unit,
    isDark: Boolean,
    bgColor: Color,
    cardColor: Color,
    textColor: Color,
    subTextColor: Color,
    dividerColor: Color,
    topBarBgColor: Color
) {
    val settings = LocalAppSettings.current

    val descriptions = mapOf(
        SizeSystem.UK to settings.t("Standard UK sizing (e.g. 6, 8, 10)", "Bảng size chuẩn Anh (ví dụ: 6, 8, 10)", "Taille standard Royaume-Uni (ex. 6, 8, 10)", "英国標準サイズ (例 6, 8, 10)", "영국 표준 사이즈 (예: 6, 8, 10)", "英国标准尺码 (例如 6, 8, 10)"),
        SizeSystem.US to settings.t("Standard US sizing (e.g. 2, 4, 6)", "Bảng size chuẩn Mỹ (ví dụ: 2, 4, 6)", "Taille standard États-Unis (ex. 2, 4, 6)", "米国標準サイズ (例 2, 4, 6)", "미국 표준 사이즈 (예: 2, 4, 6)", "美国标准尺码 (例如 2, 4, 6)"),
        SizeSystem.EU to settings.t("European sizing (e.g. 36, 38, 40)", "Bảng size chuẩn châu Âu (ví dụ: 36, 38, 40)", "Taille européenne (ex. 36, 38, 40)", "欧州サイズ (例 36, 38, 40)", "유럽 사이즈 (예: 36, 38, 40)", "欧洲尺码 (例如 36, 38, 40)"),
        SizeSystem.ASIAN to settings.t("Asian sizing (e.g. S, M, L, XL)", "Bảng size chuẩn châu Á (ví dụ: S, M, L, XL)", "Taille asiatique (ex. S, M, L, XL)", "アジアサイズ (例 S, M, L, XL)", "아시아 사이즈 (예: S, M, L, XL)", "亚洲尺码 (例如 S, M, L, XL)")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settings.t("Size System", "Hệ size", "Système de taille", "サイズ表記", "사이즈 체계", "尺码系统"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarBgColor,
                    titleContentColor = textColor,
                    navigationIconContentColor = textColor
                )
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SizeSystem.values().forEach { size ->
                val selected = settings.sizeSystem == size
                SelectableCard(
                    selected = selected,
                    onClick = { settings.updateSizeSystem(size) },
                    isDark = isDark,
                    cardColor = cardColor
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) PrimaryBlue else (if (isDark) Color(0xFF1E2E4A) else LightBlue)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                size.label.uppercase(java.util.Locale.ROOT),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (selected) Color.White else PrimaryBlue
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${size.label} Sizes", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = textColor)
                            Text(descriptions[size] ?: "", fontSize = 12.sp, color = subTextColor)
                        }
                        if (selected) {
                            Icon(Icons.Default.CheckCircle, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryScreen(
    onBack: () -> Unit,
    isDark: Boolean,
    bgColor: Color,
    cardColor: Color,
    textColor: Color,
    subTextColor: Color,
    dividerColor: Color,
    topBarBgColor: Color
) {
    val settings = LocalAppSettings.current
    var search by remember { mutableStateOf("") }
    val filtered = countryList.filter { it.contains(search, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settings.t("Country", "Quốc gia", "Pays", "国", "국가", "国家"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarBgColor,
                    titleContentColor = textColor,
                    navigationIconContentColor = textColor
                )
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text(settings.t("Search country...", "Tìm quốc gia...", "Rechercher un pays...", "国を検索...", "국가 검색...", "搜索国家...")) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = subTextColor) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = dividerColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedContainerColor = cardColor,
                    unfocusedContainerColor = cardColor
                )
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered) { c ->
                    val selected = settings.country == c
                    SelectableCard(
                        selected = selected,
                        onClick = { settings.updateCountry(c) },
                        isDark = isDark,
                        cardColor = cardColor
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(c, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
                            if (selected) {
                                Icon(Icons.Default.CheckCircle, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationScreen(
    onBack: () -> Unit,
    isDark: Boolean,
    bgColor: Color,
    cardColor: Color,
    textColor: Color,
    subTextColor: Color,
    dividerColor: Color,
    topBarBgColor: Color
) {
    val settings = LocalAppSettings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settings.t("Notifications", "Thông báo", "Notifications", "通知", "알림", "通知"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarBgColor,
                    titleContentColor = textColor,
                    navigationIconContentColor = textColor
                )
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsSectionCard(
                title = settings.t("General", "Chung", "Général", "一般", "일반", "通用"),
                cardColor = cardColor,
                subTextColor = subTextColor
            ) {
                SwitchSettingRow(
                    icon = Icons.Default.Notifications,
                    iconTint = Color(0xFFF59E0B),
                    label = settings.t("Enable Notifications", "Bật thông báo", "Activer les notifications", "通知を有効化", "알림 허용", "启用通知"),
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { settings.updateNotifications(it) },
                    textColor = textColor
                )
            }

            AnimatedVisibility(visible = settings.notificationsEnabled) {
                SettingsSectionCard(
                    title = settings.t("Notification Types", "Loại thông báo", "Types de notification", "通知の種類", "알림 유형", "通知类型"),
                    cardColor = cardColor,
                    subTextColor = subTextColor
                ) {
                    SwitchSettingRow(
                        icon = Icons.Default.LocalShipping,
                        iconTint = PrimaryBlue,
                        label = settings.t("Order Updates", "Cập nhật đơn hàng", "Mises à jour de commande", "注文状況の更新", "주문 업데이트", "订单状态更新"),
                        checked = settings.orderUpdatesEnabled,
                        onCheckedChange = { settings.updateOrderUpdates(it) },
                        textColor = textColor
                    )
                    RowDivider(dividerColor)
                    SwitchSettingRow(
                        icon = Icons.Default.LocalOffer,
                        iconTint = Color(0xFFEC4899),
                        label = settings.t("Promotions & Deals", "Khuyến mãi & Ưu đãi", "Promotions & Offres", "セールとオファー", "프로모션 및 혜택", "促销与优惠"),
                        checked = settings.promotionsEnabled,
                        onCheckedChange = { settings.updatePromotions(it) },
                        textColor = textColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreen(
    onBack: () -> Unit,
    isDark: Boolean,
    bgColor: Color,
    cardColor: Color,
    textColor: Color,
    subTextColor: Color,
    dividerColor: Color,
    topBarBgColor: Color
) {
    val settings = LocalAppSettings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settings.t("About", "Giới thiệu", "À propos", "アプリについて", "정보", "关于"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarBgColor,
                    titleContentColor = textColor,
                    navigationIconContentColor = textColor
                )
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDark) Color(0xFF1E2E4A) else LightBlue),
                contentAlignment = Alignment.Center
            ) {
                Text("👗", fontSize = 52.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text("Fashion App", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text(
                settings.t("Version 1.0 · May 2026", "Phiên bản 1.0 · Tháng 5, 2026", "Version 1.0 · Mai 2026", "バージョン 1.0 · 2026年5月", "버전 1.0 · 2026년 5월", "版本 1.0 · 2026年5月"),
                fontSize = 13.sp, color = subTextColor
            )
            Spacer(Modifier.height(32.dp))

            SettingsSectionCard(
                title = settings.t("Information", "Thông tin", "Informations", "情報", "정보", "信息"),
                cardColor = cardColor,
                subTextColor = subTextColor
            ) {
                InfoRow(label = settings.t("Developer", "Nhà phát triển", "Développeur", "開発元", "개발자", "开发者"), value = "UET Fashion Team", textColor = textColor, subTextColor = subTextColor)
                RowDivider(dividerColor)
                InfoRow(label = settings.t("University", "Trường", "Université", "大学", "대학교", "学校"), value = "UET - VNU Hanoi", textColor = textColor, subTextColor = subTextColor)
                RowDivider(dividerColor)
                InfoRow(label = settings.t("Build", "Phiên bản build", "Build", "ビルド", "빌드", "构建版本"), value = "2026.05.07", textColor = textColor, subTextColor = subTextColor)
                RowDivider(dividerColor)
                InfoRow(label = settings.t("Platform", "Nền tảng", "Plateforme", "プラットフォーム", "플랫폼", "平台"), value = "Android (Compose)", textColor = textColor, subTextColor = subTextColor)
            }

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
                color = subTextColor
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
