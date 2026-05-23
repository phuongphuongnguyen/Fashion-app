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
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fashionapp.R
import com.example.fashionapp.data.MockData
import com.example.fashionapp.model.Post
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.app.home.HomeViewModel
import com.example.fashionapp.ui.components.CommentBottomSheet
import com.example.fashionapp.ui.components.FeedPostItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val profilePosts = uiState.posts.ifEmpty { MockData.feedPosts }
    var selectedTab by remember { mutableStateOf("Posts") }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    var showComments by remember { mutableStateOf(false) }
    var localLikedPosts by remember { mutableStateOf(setOf<String>()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Chatbot icon
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.Chatbot.route)
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chatbot),
                            contentDescription = "Chatbot",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    // Settings icon
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.Settings.route)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
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
                    onSendComment = { text -> viewModel.addComment(post.id, text) }
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
                    posts = profilePosts,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            if (selectedTab == "Posts") {
                items(profilePosts, key = { it.id }) { post ->
                    FeedPostItem(
                        post = post,
                        isLiked = (uiState.likedPosts[post.id] ?: false) || localLikedPosts.contains(post.id),
                        onLikeClick = {
                            if (uiState.posts.any { it.id == post.id }) {
                                viewModel.toggleLike(post.id)
                            } else {
                                localLikedPosts =
                                    if (localLikedPosts.contains(post.id)) localLikedPosts - post.id
                                    else localLikedPosts + post.id
                            }
                        },
                        onCommentClick = {
                            selectedPostId = post.id
                            showComments = true
                        }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))
                }
            } else {
                items(MockData.products.chunked(2)) { rowProducts ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowProducts.forEach { product ->
                            Column(modifier = Modifier.weight(1f)) {
                                AsyncImage(
                                    model = product.imageUrl.ifBlank { null },
                                    contentDescription = product.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.78f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFF4F4F4)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(product.name, fontSize = 12.sp, maxLines = 2)
                                Text(
                                    "$${"%.2f".format(product.price)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        if (rowProducts.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    posts: List<Post>,
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val profilePost = posts.firstOrNull()
    val name = profilePost?.authorName?.takeIf { it.isNotBlank() } ?: "Romina"
    val avatar = profilePost?.authorAvt.orEmpty()

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = avatar.ifBlank { null },
                contentDescription = name,
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
            Spacer(Modifier.weight(1f))
            ProfileMetric(posts.size.toString(), "Posts")
            ProfileMetric("834", "Followers")
            ProfileMetric("162", "Following")
        }

        Spacer(Modifier.height(10.dp))
        Text(name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            listOf("Posts", "Products").forEach { tab ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onTabSelected(tab) }
                ) {
                    Text(
                        text = tab,
                        color = if (selectedTab == tab) Color.Black else Color(0xFF9A9A9A),
                        fontSize = 15.sp,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(2.dp)
                            .background(if (selectedTab == tab) Color.Black else Color.Transparent)
                    )
                }
            }
        }
    }
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
