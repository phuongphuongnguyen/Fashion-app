package com.example.fashionapp.ui.app.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fashionapp.R
import com.example.fashionapp.model.Product
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.navigation.openProfileOrShop
import com.example.fashionapp.ui.app.home.HomeViewModel
import com.example.fashionapp.ui.app.saved.SavedViewModel
import com.example.fashionapp.ui.components.CommentBottomSheet
import com.example.fashionapp.ui.components.FashionTopBar
import com.example.fashionapp.ui.components.FeedPostItem
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    shopId: String,
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel(),
    shopViewModel: ShopViewModel = viewModel(key = "shop_$shopId"),
    savedViewModel: SavedViewModel = viewModel()
) {
    val homeState by homeViewModel.uiState.collectAsState()
    val shopState by shopViewModel.uiState.collectAsState()
    val savedUiState by savedViewModel.uiState.collectAsState()
    val settings = LocalAppSettings.current

    LaunchedEffect(shopId) {
        shopViewModel.loadShopUser(shopId)
    }

    val shopPosts = shopState.posts

    // id thật của chủ tài khoản (resolve trong ViewModel) — dùng cho follow / đăng bài
    val ownerId = shopState.shopUser?.id ?: shopId

    // Products chỉ theo đúng productShopId đã resolve (rỗng = không phải shop → không có sản phẩm).
    val shopProducts = remember(shopState.products, shopState.productShopId) {
        val sid = shopState.productShopId
        if (sid.isBlank()) emptyList()
        else shopState.products.filter { it.shopId == sid }
    }

    var selectedTab by remember { mutableStateOf("Posts") }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    var showComments by remember { mutableStateOf(false) }
    var showBioDialog by remember { mutableStateOf(false) }
    var bioDraft by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    Scaffold(
        topBar = {
            FashionTopBar(
                title = if (shopState.isOwnProfile) settings.t("Profile", "Trang cá nhân") else "",
                onBackClick = if (shopState.isOwnProfile) null else { { navController.popBackStack() } },
                actions = {
                    if (shopState.isOwnProfile) {
                        com.example.fashionapp.ui.components.ActionIconButton(
                            iconRes = if (settings.isDarkMode) R.drawable.ic_setting_dark else R.drawable.ic_setting,
                            contentDescription = settings.t("Settings", "Cài đặt"),
                            onClick = { navController.navigate(Screen.Settings.route) }
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LaunchedEffect(shopState.bioError) {
            shopState.bioError?.let { message ->
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                shopViewModel.consumeBioError()
            }
        }

        if (showBioDialog) {
            val bioSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showBioDialog = false },
                sheetState = bioSheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = settings.t("Edit Bio", "Sửa tiểu sử"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = bioDraft,
                        onValueChange = { bioDraft = it.take(160) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        placeholder = { Text(settings.t("Tell people about yourself", "Giới thiệu bản thân"), color = AppTheme.colors.textSecondary) },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        text = "${bioDraft.length}/160",
                        fontSize = 13.sp,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            shopViewModel.updateBio(bioDraft)
                            showBioDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(settings.t("Save Changes", "Lưu thay đổi"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }

        if (showComments && selectedPostId != null) {
            shopPosts.find { it.id == selectedPostId }?.let { post ->
                CommentBottomSheet(
                    post = post,
                    sheetState = sheetState,
                    currentUserAvatarUrl = homeState.user?.avatarUrl.orEmpty(),
                    onDismiss = { showComments = false },
                    onSendComment = { text -> homeViewModel.addComment(post.id, text) }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val shopName = shopState.shopUser?.username?.takeIf { it.isNotBlank() }
                ?: shopState.shopUser?.name
                ?: shopPosts.firstOrNull()?.authorName
                ?: "Shop"

            // Ưu tiên logo shop chuyên dụng, sau đó đến avatar của user quản lý shop, cuối cùng mới lấy từ bài đăng
            val shopAvatarUrl = shopState.shopLogoUrl.ifBlank {
                shopState.shopUser?.avatarUrl.orEmpty().ifBlank {
                    shopPosts.firstOrNull()?.authorAvt.orEmpty()
                }
            }
            
            // Chỉ tài khoản role = "shop" mới có tab Products; user thường chỉ có Posts.
            val isShop = shopState.shopUser?.role?.equals("shop", ignoreCase = true) == true

            ShopHeader(
                shopName = shopName,
                avatarUrl = shopAvatarUrl,
                bio = shopState.shopUser?.bio.orEmpty(),
                selectedTab = selectedTab,
                postCount = shopPosts.size,
                followerCount = shopState.shopUser?.followersCount ?: 0,
                followingCount = shopState.shopUser?.followingCount ?: 0,
                rating = shopState.shopRating,
                isOwnProfile = shopState.isOwnProfile,
                isFollowing = shopState.isFollowing,
                isShop = isShop,
                onFollowClick = { shopViewModel.toggleFollow(ownerId) },
                onEditBio = {
                    bioDraft = shopState.shopUser?.bio.orEmpty()
                    showBioDialog = true
                },
                onTabSelected = { selectedTab = it },
                onAddPost = { navController.navigate(Screen.CreatePost.createRoute(ownerId)) }
            )

            if (selectedTab == "Posts" || !isShop) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(shopPosts, key = { it.id }) { post ->
                        FeedPostItem(
                            post = post,
                            isLiked = homeState.likedPosts[post.id] ?: false,
                            isSaved = savedUiState.savedPostIds.contains(post.id),
                            isLikePending = post.id in homeState.pendingLikePostIds,
                            onLikeClick = { homeViewModel.toggleLike(post.id) },
                            onSaveClick = { savedViewModel.toggleSave(post.id) },
                            onCommentClick = {
                                selectedPostId = post.id
                                showComments = true
                            },
                            onHeaderClick = {
                                if (post.authorId != shopId) {
                                    navController.openProfileOrShop(post.authorId)
                                }
                            },
                            onProductClick = { productId ->
                                navController.navigate(Screen.ProductDetail.createRoute(productId))
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(shopProducts, key = { it.id }) { product ->
                        ProductTile(product = product)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopHeader(
    shopName: String,
    avatarUrl: String,
    bio: String,
    selectedTab: String,
    postCount: Int,
    followerCount: Int,
    followingCount: Int,
    rating: Float,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    isShop: Boolean,
    onFollowClick: () -> Unit,
    onEditBio: () -> Unit,
    onTabSelected: (String) -> Unit,
    onAddPost: () -> Unit
) {
    val settings = LocalAppSettings.current
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar: user thường → vòng ring hồng; shop → viền xám (giữ UI như cũ của từng loại)
            val avatarModifier = if (isShop) {
                Modifier
                    .size(68.dp)
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            } else {
                Modifier
                    .size(68.dp)
                    .border(2.dp, Color(0xFFFF5F5F), CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            }
            Box(
                modifier = avatarModifier,
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotBlank() || !isShop) {
                    AsyncImage(
                        model = avatarUrl.ifBlank { null },
                        contentDescription = shopName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(R.drawable.ic_profile),
                        fallback = androidx.compose.ui.res.painterResource(R.drawable.ic_profile)
                    )
                } else {
                    Text(shopName.uppercase(), fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                // displayName + nút Follow cùng một hàng (giống user thường)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        shopName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!isOwnProfile) {
                        Spacer(Modifier.width(8.dp))
                        if (isFollowing) {
                            // Đang theo dõi: chỉ chữ xanh (vẫn bấm được để bỏ theo dõi)
                            Button(
                                onClick = onFollowClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(settings.t("Following", "Đang theo dõi"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Button(
                                onClick = onFollowClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    settings.t("Follow", "Theo dõi"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(postCount.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(settings.t(" Posts", " Bài viết"), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                    Spacer(Modifier.width(12.dp))
                    Text(followerCount.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(settings.t(" Followers", " Người theo dõi"), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = AppTheme.colors.star,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        " ${"%.1f".format(rating)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (bio.isNotBlank() || isOwnProfile) {
            Spacer(Modifier.height(12.dp))
            if (bio.isNotBlank()) {
                Text(
                    text = bio,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 20.sp
                )
            }
            if (isOwnProfile) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onEditBio,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = settings.t("Edit Profile", "Chỉnh sửa trang cá nhân"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                val tabs = if (isShop) listOf("Posts", "Products") else listOf("Posts")
                tabs.forEach { tab ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onTabSelected(tab) }
                    ) {
                        Text(
                            text = if (tab == "Posts") settings.t("Posts", "Bài viết") else settings.t("Products", "Sản phẩm"),
                            color = if (selectedTab == tab) MaterialTheme.colorScheme.onBackground else AppTheme.colors.textSecondary,
                            fontSize = 16.sp,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(2.dp)
                                .background(if (selectedTab == tab) MaterialTheme.colorScheme.onBackground else Color.Transparent)
                        )
                    }
                }
            }
            if (isOwnProfile) {
                Icon(
                    imageVector = Icons.Outlined.AddCircleOutline,
                    contentDescription = settings.t("Add", "Đăng bài"),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onAddPost() }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProductTile(product: Product) {
    Column {
        AsyncImage(
            model = product.imageUrl,
            contentDescription = product.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.78f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(6.dp))
        Text(product.name, maxLines = 2, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text("₫${"%.0f".format(product.price)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
