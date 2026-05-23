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
private val SectionBg     = Color(0xFFF7F8FA)
private val DividerColor  = Color(0xFFEEEEEE)
private val DangerRed     = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val settings = LocalAppSettings.current
    val scrollState = rememberScrollState()

    // sub-screen routing inside Settings
    var currentSubScreen by remember { mutableStateOf<SubScreen?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
            SubScreen.LANGUAGE   -> LanguageScreen(onBack = { currentSubScreen = null })
            SubScreen.CURRENCY   -> CurrencyScreen(onBack = { currentSubScreen = null })
            SubScreen.SIZE       -> SizeSystemScreen(onBack = { currentSubScreen = null })
            SubScreen.COUNTRY    -> CountryScreen(onBack = { currentSubScreen = null })
            SubScreen.NOTIFICATIONS -> NotificationScreen(onBack = { currentSubScreen = null })
            SubScreen.ABOUT      -> AboutScreen(onBack = { currentSubScreen = null })
            null -> {
                // ── Main settings list ──
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    settings.t("Settings", "Cài đặt"),
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
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                        )
                    },
                    containerColor = SectionBg
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(scrollState)
                    ) {

                        // ── Appearance Section ──
                        SettingsSectionCard(title = settings.t("Appearance", "Giao diện")) {
                            // Dark Mode toggle
                            SwitchSettingRow(
                                icon = if (settings.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                iconTint = if (settings.isDarkMode) Color(0xFF7C3AED) else Color(0xFFF59E0B),
                                label = settings.t("Dark Mode", "Chế độ tối"),
                                checked = settings.isDarkMode,
                                onCheckedChange = { settings.updateDarkMode(it) }
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // ── Language & Region ──
                        SettingsSectionCard(title = settings.t("Language & Region", "Ngôn ngữ & Vùng")) {
                            ArrowSettingRow(
                                icon = Icons.Default.Language,
                                iconTint = PrimaryBlue,
                                label = settings.t("Language", "Ngôn ngữ"),
                                value = "${settings.language.flag} ${settings.language.nativeName}",
                                onClick = { currentSubScreen = SubScreen.LANGUAGE }
                            )
                            RowDivider()
                            ArrowSettingRow(
                                icon = Icons.Default.Public,
                                iconTint = Color(0xFF10B981),
                                label = settings.t("Country", "Quốc gia"),
                                value = settings.country,
                                onClick = { currentSubScreen = SubScreen.COUNTRY }
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // ── Shop Preferences ──
                        SettingsSectionCard(title = settings.t("Shop Preferences", "Tùy chọn mua sắm")) {
                            ArrowSettingRow(
                                icon = Icons.Default.AttachMoney,
                                iconTint = Color(0xFF059669),
                                label = settings.t("Currency", "Tiền tệ"),
                                value = "${settings.currency.symbol} ${settings.currency.code}",
                                onClick = { currentSubScreen = SubScreen.CURRENCY }
                            )
                            RowDivider()
                            ArrowSettingRow(
                                icon = Icons.Default.Straighten,
                                iconTint = Color(0xFFEC4899),
                                label = settings.t("Size System", "Hệ size"),
                                value = settings.sizeSystem.label,
                                onClick = { currentSubScreen = SubScreen.SIZE }
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // ── Notifications ──
                        SettingsSectionCard(title = settings.t("Notifications", "Thông báo")) {
                            ArrowSettingRow(
                                icon = Icons.Default.Notifications,
                                iconTint = Color(0xFFF59E0B),
                                label = settings.t("Notification Settings", "Cài đặt thông báo"),
                                value = if (settings.notificationsEnabled)
                                    settings.t("On", "Bật")
                                else
                                    settings.t("Off", "Tắt"),
                                onClick = { currentSubScreen = SubScreen.NOTIFICATIONS }
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // ── About ──
                        SettingsSectionCard(title = settings.t("About", "Giới thiệu")) {
                            ArrowSettingRow(
                                icon = Icons.Default.Info,
                                iconTint = Color(0xFF6366F1),
                                label = settings.t("About Fashion App", "Về Fashion App"),
                                onClick = { currentSubScreen = SubScreen.ABOUT }
                            )
                            RowDivider()
                            ArrowSettingRow(
                                icon = Icons.Default.Description,
                                iconTint = Color(0xFF6366F1),
                                label = settings.t("Terms & Conditions", "Điều khoản & Điều kiện"),
                                onClick = { /* Show dialog */ }
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // ── Danger Zone ──
                        SettingsSectionCard(title = settings.t("Account", "Tài khoản")) {
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
                                    text = settings.t("Delete My Account", "Xóa tài khoản"),
                                    color = DangerRed,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Version info
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Fashion App", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(
                                settings.t("Version 1.0 · May 2026", "Phiên bản 1.0 · Tháng 5, 2026"),
                                fontSize = 12.sp, color = Color.LightGray
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
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Text(
                                    text = settings.t(
                                        "Are you sure you want to delete your account? This action is permanent and cannot be undone.",
                                        "Bạn có chắc chắn muốn xóa tài khoản không? Hành động này là vĩnh viễn và không thể hoàn tác.",
                                        "Êtes-vous sûr de vouloir supprimer votre compte? Cette action est irréversible.",
                                        "アカウントを削除してもよろしいですか？この操作 là vĩnh viễn và không thể hoàn tác.",
                                        "계정을 삭제하시겠습니까? 이 작업은 영구적이며 취소할 수 없습니다.",
                                        "您确定要删除账户吗？此操作是永久性的，且无法撤销。"
                                    )
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
                                    Text(text = settings.t("Cancel", "Hủy", "Annuler", "キャンセル", "취소", "取消"))
                                }
                            }
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
private fun LanguageScreen(onBack: () -> Unit) {
    val settings = LocalAppSettings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settings.t("Language", "Ngôn ngữ"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = SectionBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AppLanguage.values()) { lang ->
                SelectableCard(
                    selected = settings.language == lang,
                    onClick = { settings.updateLanguage(lang) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(lang.flag, fontSize = 28.sp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(lang.nativeName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(lang.displayName, fontSize = 13.sp, color = Color.Gray)
                        }
                        if (settings.language == lang) {
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
private fun CurrencyScreen(onBack: () -> Unit) {
    val settings = LocalAppSettings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settings.t("Currency", "Tiền tệ"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = SectionBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AppCurrency.values()) { cur ->
                SelectableCard(
                    selected = settings.currency == cur,
                    onClick = { settings.updateCurrency(cur) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(LightBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cur.symbol, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(cur.displayName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(cur.code, fontSize = 13.sp, color = Color.Gray)
                        }
                        if (settings.currency == cur) {
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
private fun SizeSystemScreen(onBack: () -> Unit) {
    val settings = LocalAppSettings.current

    val descriptions = mapOf(
        SizeSystem.UK to "Standard UK sizing (e.g. 6, 8, 10)",
        SizeSystem.US to "Standard US sizing (e.g. 2, 4, 6)",
        SizeSystem.EU to "European sizing (e.g. 36, 38, 40)",
        SizeSystem.ASIAN to "Asian sizing (e.g. S, M, L, XL)"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settings.t("Size System", "Hệ cỡ số"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = SectionBg
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SizeSystem.values().forEach { size ->
                SelectableCard(
                    selected = settings.sizeSystem == size,
                    onClick = { settings.updateSizeSystem(size) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (settings.sizeSystem == size) PrimaryBlue else LightBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                size.label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (settings.sizeSystem == size) Color.White else PrimaryBlue
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${size.label} Sizes", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(descriptions[size] ?: "", fontSize = 12.sp, color = Color.Gray)
                        }
                        if (settings.sizeSystem == size) {
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
private fun CountryScreen(onBack: () -> Unit) {
    val settings = LocalAppSettings.current
    var search by remember { mutableStateOf("") }
    val filtered = countryList.filter { it.contains(search, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settings.t("Country", "Quốc gia"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = SectionBg
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text(settings.t("Search country...", "Tìm quốc gia...")) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = DividerColor
                )
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered) { c ->
                    SelectableCard(
                        selected = settings.country == c,
                        onClick = { settings.updateCountry(c) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(c, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            if (settings.country == c) {
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
private fun NotificationScreen(onBack: () -> Unit) {
    val settings = LocalAppSettings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settings.t("Notifications", "Thông báo"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = SectionBg
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsSectionCard(title = settings.t("General", "Chung")) {
                SwitchSettingRow(
                    icon = Icons.Default.Notifications,
                    iconTint = Color(0xFFF59E0B),
                    label = settings.t("Enable Notifications", "Bật thông báo"),
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { settings.updateNotifications(it) }
                )
            }

            AnimatedVisibility(visible = settings.notificationsEnabled) {
                SettingsSectionCard(title = settings.t("Notification Types", "Loại thông báo")) {
                    SwitchSettingRow(
                        icon = Icons.Default.LocalShipping,
                        iconTint = PrimaryBlue,
                        label = settings.t("Order Updates", "Cập nhật đơn hàng"),
                        checked = settings.orderUpdatesEnabled,
                        onCheckedChange = { settings.updateOrderUpdates(it) }
                    )
                    RowDivider()
                    SwitchSettingRow(
                        icon = Icons.Default.LocalOffer,
                        iconTint = Color(0xFFEC4899),
                        label = settings.t("Promotions & Deals", "Khuyến mãi & Ưu đãi"),
                        checked = settings.promotionsEnabled,
                        onCheckedChange = { settings.updatePromotions(it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreen(onBack: () -> Unit) {
    val settings = LocalAppSettings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settings.t("About", "Giới thiệu"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = SectionBg
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
                    .background(LightBlue),
                contentAlignment = Alignment.Center
            ) {
                Text("👗", fontSize = 52.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text("Fashion App", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                settings.t("Version 1.0 · May 2026", "Phiên bản 1.0 · Tháng 5, 2026"),
                fontSize = 13.sp, color = Color.Gray
            )
            Spacer(Modifier.height(32.dp))

            SettingsSectionCard(title = settings.t("Information", "Thông tin")) {
                InfoRow(label = settings.t("Developer", "Nhà phát triển"), value = "UET Fashion Team")
                RowDivider()
                InfoRow(label = settings.t("University", "Trường"), value = "UET - VNU Hanoi")
                RowDivider()
                InfoRow(label = settings.t("Build", "Phiên bản build"), value = "2026.05.07")
                RowDivider()
                InfoRow(label = settings.t("Platform", "Nền tảng"), value = "Android (Compose)")
            }

            Spacer(Modifier.height(24.dp))
            Text(
                settings.t(
                    "© 2026 Fashion App. All rights reserved.",
                    "© 2026 Fashion App. Tất cả quyền được bảo lưu."
                ),
                fontSize = 12.sp,
                color = Color.LightGray
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
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
    onClick: () -> Unit
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
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
        if (value != null) {
            Text(value, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(end = 4.dp))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.LightGray,
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
    onCheckedChange: (Boolean) -> Unit
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
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
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
    content: @Composable () -> Unit
) {
    val borderColor = if (selected) PrimaryBlue else Color.Transparent
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) LightBlue else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 1.dp)
    ) {
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = DividerColor
    )
}
