package com.example.fashionapp.ui.app.profile

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.fashionapp.R
import com.example.fashionapp.model.Post
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.app.home.HomeViewModel
import com.example.fashionapp.ui.app.profile.ProfileViewModel
import com.example.fashionapp.ui.app.saved.SavedViewModel
import com.example.fashionapp.ui.components.CommentBottomSheet
import com.example.fashionapp.ui.components.FeedPostItem
import com.example.fashionapp.ui.components.FashionTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    savedViewModel: SavedViewModel = viewModel()
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val homeState by homeViewModel.uiState.collectAsState()
    val savedUiState by savedViewModel.uiState.collectAsState()
    val profilePosts = uiState.posts
    var selectedTab by remember { mutableStateOf("Posts") }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    var showComments by remember { mutableStateOf(false) }
    var localLikedPosts by remember { mutableStateOf(setOf<String>()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            FashionTopBar(
                title = "",
                actions = {
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.Settings.route)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF0057FF)
                        )
                    }
                }
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        if (showComments && selectedPostId != null) {
            profilePosts.find { it.id == selectedPostId }?.let { post ->
                CommentBottomSheet(
                    post = post,
                    sheetState = sheetState,
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
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            items(profilePosts, key = { it.id }) { post ->
                FeedPostItem(
                    post = post,
                    isLiked = (homeState.likedPosts[post.id] ?: false) || localLikedPosts.contains(post.id),
                    isSaved = savedUiState.savedPostIds.contains(post.id),
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
                    }
                )
                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    user: com.example.fashionapp.model.User?,
    postsCount: Int,
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val username = user?.username?.takeIf { it.isNotBlank() } ?: "User"
    val avatar = user?.avatarUrl.orEmpty()
    val context = LocalContext.current
    val avatarModel = remember(avatar) {
        avatar.takeIf { it.isNotBlank() }?.withCacheBust()
    }
    val avatarRequest = remember(avatarModel, context) {
        ImageRequest.Builder(context)
            .data(avatarModel)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .networkCachePolicy(CachePolicy.DISABLED)
            .build()
    }
    val followers = user?.followersCount ?: 0
    val following = user?.followingCount ?: 0

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = avatarRequest,
                contentDescription = username,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFFFF5F5F), CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFECECEC)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_profile)
            )
            Spacer(Modifier.weight(2f))
            ProfileMetric(postsCount.toString(), "Posts")
            ProfileMetric(followers.toString(), "Followers")
            ProfileMetric(following.toString(), "Following")
        }

        Spacer(Modifier.height(10.dp))
        Text(username, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = Icons.Outlined.AddCircleOutline,
                    contentDescription = "Add Post",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun String.withCacheBust(): String {
    val separator = if (contains("?")) "&" else "?"
    return "$this${separator}profileAvatarBust=${System.currentTimeMillis()}"
}

@Composable
private fun ProfileMetric(value: String, label: String) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}
