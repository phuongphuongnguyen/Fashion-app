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
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.app.home.HomeViewModel
import com.example.fashionapp.ui.components.CommentBottomSheet
import com.example.fashionapp.ui.components.FeedPostItem
import com.example.fashionapp.ui.app.settings.LocalAppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    navController: NavController,
    postId: String,
    homeViewModel: HomeViewModel = viewModel(),
    savedViewModel: SavedViewModel = viewModel()
) {
    val settings = LocalAppSettings.current
    val homeState by homeViewModel.uiState.collectAsState()
    val savedUiState by savedViewModel.uiState.collectAsState()
    val post = homeState.posts.find { it.id == postId } ?: savedUiState.savedPosts.find { it.id == postId }

    var showComments by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settings.t("Post", "Bài viết"), fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = settings.t("Back", "Quay lại"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        if (showComments && post != null) {
            CommentBottomSheet(
                post = post,
                sheetState = sheetState,
                currentUserAvatarUrl = homeState.user?.avatarUrl.orEmpty(),
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
                    text = settings.t("Post not found", "Không tìm thấy bài viết"),
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            } else {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    FeedPostItem(
                        post = post,
                        isLiked = homeState.likedPosts[post.id] ?: false,
                        isSaved = savedUiState.savedPostIds.contains(post.id),
                        isVerified = (post.authorName == "mina"),
                        isLikePending = post.id in homeState.pendingLikePostIds,
                        onLikeClick = { homeViewModel.toggleLike(post.id) },
                        onSaveClick = { savedViewModel.toggleSave(post.id) },
                        onCommentClick = { showComments = true },
                        onProductClick = { productId ->
                            navController.navigate(Screen.ProductDetail.createRoute(productId))
                        }
                    )
                }
            }
        }
    }
}
