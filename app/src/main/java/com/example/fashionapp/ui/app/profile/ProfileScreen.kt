package com.example.fashionapp.ui.app.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.fashionapp.R
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.app.home.HomeViewModel
import com.example.fashionapp.ui.app.saved.SavedViewModel
import com.example.fashionapp.ui.components.CommentBottomSheet
import com.example.fashionapp.ui.components.FashionTopBar
import com.example.fashionapp.ui.components.FeedPostItem
import com.example.fashionapp.ui.components.ProfileTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    userId: String? = null,
    profileViewModel: ProfileViewModel = viewModel(
        key = "profile_${userId.orEmpty()}",
        factory = ProfileViewModelFactory(userId)
    ),
    homeViewModel: HomeViewModel = viewModel(),
    savedViewModel: SavedViewModel = viewModel()
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val isOwnProfile = uiState.isOwnProfile
    val homeState by homeViewModel.uiState.collectAsState()
    val savedUiState by savedViewModel.uiState.collectAsState()
    val profilePosts = uiState.posts

    var selectedPostId by remember { mutableStateOf<String?>(null) }
    var showComments by remember { mutableStateOf(false) }
    var localLikedPosts by remember { mutableStateOf(setOf<String>()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (isOwnProfile) {
                ProfileTopBar(
                    scrollBehavior = scrollBehavior,
                    onSettingsClick = { navController.navigate(Screen.Settings.route) }
                )
            } else {
                FashionTopBar(
                    title = uiState.user?.username?.takeIf { it.isNotBlank() }
                        ?: uiState.user?.name?.takeIf { it.isNotBlank() }
                        ?: "Profile",
                    onBackClick = { navController.popBackStack() }
                )
            }
        },
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->

        if (showComments && selectedPostId != null) {
            profilePosts.find { it.id == selectedPostId }?.let { post ->
                CommentBottomSheet(
                    post = post,
                    sheetState = sheetState,
                    currentUserAvatarUrl = uiState.user?.avatarUrl.orEmpty(),
                    onDismiss = { showComments = false },
                    onSendComment = { text -> homeViewModel.addComment(post.id, text) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            item {
                ProfileHeader(
                    user = uiState.user,
                    postsCount = profilePosts.size,
                    isOwnProfile = isOwnProfile,
                    isFollowing = uiState.isFollowing,
                    onFollowClick = { profileViewModel.toggleFollow() },
                    onAddPost = { navController.navigate(Screen.CreatePost.createRoute()) }
                )
            }

            items(profilePosts, key = { it.id }) { post ->
                FeedPostItem(
                    post = post,
                    isLiked = (homeState.likedPosts[post.id] ?: false) || localLikedPosts.contains(post.id),
                    isSaved = savedUiState.savedPostIds.contains(post.id),
                    isVerified = post.authorName == "mina",
                    onLikeClick = {
                        if (homeState.posts.any { it.id == post.id }) {
                            homeViewModel.toggleLike(post.id)
                        } else {
                            localLikedPosts =
                                if (localLikedPosts.contains(post.id)) localLikedPosts - post.id
                                else localLikedPosts + post.id
                        }
                    },
                    onSaveClick = { savedViewModel.toggleSave(post.id) },
                    onCommentClick = {
                        selectedPostId = post.id
                        showComments = true
                    },
                    onHeaderClick = {
                        if (post.authorId.isNotBlank()) {
                            navController.navigate(Screen.ShopDetail.createRoute(post.authorId))
                        }
                    }
                )
                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))
            }
        }
    }
}

// ── Profile Header ────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeader(
    user: com.example.fashionapp.model.User?,
    postsCount: Int,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onAddPost: () -> Unit
) {
    val name = user?.name?.takeIf { it.isNotBlank() } ?: "User"
    val avatar = user?.avatarUrl.orEmpty()
    val context = LocalContext.current
    val followers = user?.followersCount ?: 0
    val following = user?.followingCount ?: 0

    val avatarRequest = remember(context, avatar) {
        ImageRequest.Builder(context)
            .data(avatar.takeIf { it.isNotBlank() })
            .crossfade(true)
            .build()
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        // ── Hàng thông tin (bố cục giống ShopScreen): avatar | name + stats | Follow ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = avatarRequest,
                contentDescription = name,
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFFFF5F5F), CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFECECEC)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_profile),
                fallback = painterResource(R.drawable.ic_profile)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                // displayName + nút Follow cùng một hàng
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!isOwnProfile) {
                        Spacer(Modifier.width(8.dp))
                        if (isFollowing) {
                            // Đang theo dõi: chỉ chữ xanh, không button (vẫn bấm được để bỏ theo dõi)
                            Text(
                                "Following",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1769FF),
                                modifier = Modifier.clickable { onFollowClick() }
                            )
                        } else {
                            Button(
                                onClick = onFollowClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1769FF)),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(postsCount.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(" Posts", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.width(12.dp))
                    Text(followers.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(" Followers", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.width(12.dp))
                    Text(following.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(" Following", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Hàng "Posts" + nút thêm bài (chỉ trang của mình) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Posts",
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(2.dp)
                        .background(Color.Black)
                )
            }
            if (isOwnProfile) {
                Icon(
                    imageVector = Icons.Outlined.AddCircleOutline,
                    contentDescription = "Add Post",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onAddPost() }
                )
            }
        }
    }
}

