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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fashionapp.R
import com.example.fashionapp.model.Product
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.app.home.HomeViewModel
import com.example.fashionapp.ui.app.saved.SavedViewModel
import com.example.fashionapp.ui.components.CommentBottomSheet
import com.example.fashionapp.ui.components.FashionTopBar
import com.example.fashionapp.ui.components.FeedPostItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    shopId: String,
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel(),
    shopViewModel: ShopViewModel = viewModel(),
    savedViewModel: SavedViewModel = viewModel()
) {
    val homeState by homeViewModel.uiState.collectAsState()
    val shopState by shopViewModel.uiState.collectAsState()
    val savedUiState by savedViewModel.uiState.collectAsState()

    LaunchedEffect(shopId) {
        shopViewModel.loadShopUser(shopId)
    }

    val shopPosts = shopState.posts

    // id thật của chủ tài khoản (resolve trong ViewModel) — dùng cho follow / đăng bài
    val ownerId = shopState.shopUser?.id ?: shopId

    val shopProducts = remember(shopState.products, shopState.productShopId) {
        val sid = shopState.productShopId.ifBlank { shopId }
        shopState.products.filter { it.shopId == sid }
    }

    var selectedTab by remember { mutableStateOf("Posts") }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    var showComments by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            FashionTopBar(
                title = "",
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
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
            val shopName = shopState.shopUser?.name
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
                selectedTab = selectedTab,
                postCount = shopPosts.size,
                followerCount = shopState.shopUser?.followersCount ?: 0,
                followingCount = shopState.shopUser?.followingCount ?: 0,
                rating = shopState.shopRating,
                isOwnProfile = shopState.isOwnProfile,
                isFollowing = shopState.isFollowing,
                isShop = isShop,
                onFollowClick = { shopViewModel.toggleFollow(ownerId) },
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
                                    navController.navigate(Screen.ShopDetail.createRoute(post.authorId))
                                }
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))
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
    selectedTab: String,
    postCount: Int,
    followerCount: Int,
    followingCount: Int,
    rating: Float,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    isShop: Boolean,
    onFollowClick: () -> Unit,
    onTabSelected: (String) -> Unit,
    onAddPost: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .border(1.dp, Color(0xFFE0E0E0), CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotBlank()) {
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
                Text(shopName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(postCount.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(" Posts", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.width(12.dp))
                    Text(followerCount.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(" Followers", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        " ${"%.1f".format(rating)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (!isOwnProfile) {
                if (isFollowing) {
                    // Đang theo dõi: chỉ chữ xanh, không button (vẫn bấm được để bỏ theo dõi)
                    Button(
                        onClick = onFollowClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE5EDFF),
                            contentColor = Color(0xFF1769FF)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Following", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = onFollowClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1769FF)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            "Follow",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
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
                            text = tab,
                            color = if (selectedTab == tab) Color.Black else Color(0xFF9A9A9A),
                            fontSize = 16.sp,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(2.dp)
                                .background(if (selectedTab == tab) Color.Black else Color.Transparent)
                        )
                    }
                }
            }
            if (isOwnProfile) {
                Icon(
                    imageVector = Icons.Outlined.AddCircleOutline,
                    contentDescription = "Add",
                    tint = Color.Black,
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
                .background(Color(0xFFF1F1F1)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(6.dp))
        Text(product.name, maxLines = 2, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text("₫${"%.0f".format(product.price)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
