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
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.theme.AppTheme

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
    val settings = LocalAppSettings.current
    val uiState by profileViewModel.uiState.collectAsState()
    val isOwnProfile = uiState.isOwnProfile
    val homeState by homeViewModel.uiState.collectAsState()
    val savedUiState by savedViewModel.uiState.collectAsState()
    val profilePosts = uiState.posts

    var selectedPostId by remember { mutableStateOf<String?>(null) }
    var showComments by remember { mutableStateOf(false) }
    var showBioDialog by remember { mutableStateOf(false) }
    var bioDraft by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

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
                        ?: settings.t("Profile", "Trang cá nhân"),
                    onBackClick = { navController.popBackStack() }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LaunchedEffect(uiState.bioError) {
            uiState.bioError?.let { message ->
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                profileViewModel.consumeBioError()
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
                    OutlinedTextField(
                        value = bioDraft,
                        onValueChange = { bioDraft = it.take(160) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        placeholder = { Text(settings.t("Tell people about yourself", "Giới thiệu bản thân"), color = AppTheme.colors.textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
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
                            profileViewModel.updateBio(bioDraft)
                            showBioDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                    ) {
                        Text(settings.t("Save Changes", "Lưu thay đổi"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }


        if (showComments && selectedPostId != null) {
            profilePosts.find { it.id == selectedPostId }?.let { post ->
                CommentBottomSheet(
                    post = post,
                    sheetState = sheetState,
                    currentUserAvatarUrl = homeState.user?.avatarUrl.orEmpty(),
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
                    onEditBio = {
                        bioDraft = uiState.user?.bio.orEmpty()
                        showBioDialog = true
                    },
                    onAddPost = { navController.navigate(Screen.CreatePost.createRoute()) }
                )
            }

            items(profilePosts, key = { it.id }) { post ->
                FeedPostItem(
                    post = post,
                    isLiked = homeState.likedPosts[post.id] ?: false,
                    isSaved = savedUiState.savedPostIds.contains(post.id),
                    isVerified = post.authorName == "mina",
                    isLikePending = post.id in homeState.pendingLikePostIds,
                    onLikeClick = { homeViewModel.toggleLike(post.id) },
                    onSaveClick = { savedViewModel.toggleSave(post.id) },
                    onCommentClick = {
                        selectedPostId = post.id
                        showComments = true
                    },
                    onHeaderClick = {
                        if (post.authorId.isNotBlank()) {
                            navController.navigate(Screen.ShopDetail.createRoute(post.authorId))
                        }
                    },
                    onProductClick = { productId ->
                        navController.navigate(Screen.ProductDetail.createRoute(productId))
                    }
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
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
    onEditBio: () -> Unit,
    onAddPost: () -> Unit
) {
    val settings = LocalAppSettings.current
    val name = user?.name?.takeIf { it.isNotBlank() } ?: settings.t("User", "Người dùng")
    val avatar = user?.avatarUrl.orEmpty()
    val bio = user?.bio.orEmpty()
    val context = LocalContext.current
    val followers = user?.followersCount ?: 0
    val following = user?.followingCount ?: 0

    val avatarRequest = remember(context, avatar) {
        ImageRequest.Builder(context)
            .data(avatar.takeIf { it.isNotBlank() })
            .crossfade(true)
            .build()
    }

    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
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
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_profile),
                fallback = painterResource(R.drawable.ic_profile)
            )
            Spacer(Modifier.width(14.dp))
            // ── name + stats (giống ShopScreen) ──
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(postsCount.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                    Text(settings.t(" Posts", " Bài viết"), fontSize = 12.sp, color = AppTheme.colors.textSecondary, softWrap = false, maxLines = 1)
                    Spacer(Modifier.width(12.dp))
                    Text(followers.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                    Text(settings.t(" Followers", " Người theo dõi"), fontSize = 12.sp, color = AppTheme.colors.textSecondary, softWrap = false, maxLines = 1)
                    Spacer(Modifier.width(12.dp))
                    Text(following.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                    Text(settings.t(" Following", " Đang theo dõi"), fontSize = 12.sp, color = AppTheme.colors.textSecondary, softWrap = false, maxLines = 1)
                }
            }
            // ── Follow button (chỉ trang người khác) ──
            if (!isOwnProfile) {
                if (isFollowing) {
                    Button(
                        onClick = onFollowClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE5EDFF),
                            contentColor = Color(0xFF1769FF)
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(settings.t("Following", "Đang theo dõi"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = onFollowClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1769FF)),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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

        Spacer(Modifier.height(16.dp))

        // ── Hàng "Posts" + nút thêm bài (chỉ trang của mình) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = settings.t("Posts", "Bài viết"),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.onBackground)
                )
            }
            if (isOwnProfile) {
                Icon(
                    imageVector = Icons.Outlined.AddCircleOutline,
                    contentDescription = settings.t("Add Post", "Đăng bài"),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onAddPost() }
                )
            }
        }
    }
}

