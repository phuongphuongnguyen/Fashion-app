package com.example.fashionapp.ui.app.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.fashionapp.ui.app.home.HomeViewModel
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.components.CommentBottomSheet
import com.example.fashionapp.ui.components.FeedPostItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    navController: NavController,
    postId: String,
    homeViewModel: HomeViewModel = viewModel(),
    savedViewModel: SavedViewModel = viewModel()
) {
    val settings = LocalAppSettings.current
    val isDark = settings.isDarkMode
    val bgColor = if (isDark) Color(0xFF121212) else Color.White
    val topBarBg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1A1A2E)
    val iconColor = if (isDark) Color.White else Color.Black

    val homeState by homeViewModel.uiState.collectAsState()
    val savedUiState by savedViewModel.uiState.collectAsState()
    val post = homeState.posts.find { it.id == postId } ?: savedUiState.savedPosts.find { it.id == postId }

    var showComments by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        settings.t(en = "Post", vi = "Bài viết", fr = "Publication", ja = "投稿", ko = "게시물", zh = "帖子"),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = textColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = iconColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarBg)
            )
        },
        containerColor = bgColor
    ) { innerPadding ->
        if (showComments && post != null) {
            CommentBottomSheet(
                post = post,
                sheetState = sheetState,
                onDismiss = { showComments = false },
                onSendComment = { text ->
                    homeViewModel.addComment(post.id, text)
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (homeState.isLoading || savedUiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (post == null) {
                Text(
                    text = "Post not found for ID: $postId",
                    modifier = Modifier.align(Alignment.Center),
                    color = if (isDark) Color(0xFF888888) else Color.Gray
                )
            } else {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    FeedPostItem(
                        post = post,
                        isLiked = homeState.likedPosts[post.id] ?: false,
                        isSaved = savedUiState.savedPostIds.contains(post.id),
                        isVerified = (post.authorName == "mina"),
                        onLikeClick = { homeViewModel.toggleLike(post.id) },
                        onSaveClick = { savedViewModel.toggleSave(post.id) },
                        onCommentClick = { showComments = true }
                    )
                }
            }
        }
    }
}
