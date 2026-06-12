package com.example.fashionapp.ui.app.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.navigation.openProfileOrShop
import com.example.fashionapp.ui.app.saved.SavedViewModel
import com.example.fashionapp.ui.components.CommentBottomSheet
import com.example.fashionapp.ui.components.FeedPostItem
import com.example.fashionapp.ui.components.FeedPostSkeleton
import com.example.fashionapp.ui.components.HomeTopBar
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(),
    savedViewModel: SavedViewModel = viewModel()
) {
    val settings = LocalAppSettings.current
    val uiState by viewModel.uiState.collectAsState()
    val savedUiState by savedViewModel.uiState.collectAsState()

    var showComments by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            HomeTopBar(
                scrollBehavior = scrollBehavior,
                onSearchClick = {
                    navController.navigate(Screen.Search.createRoute())
                },
                onMessClick = {
                    navController.navigate(Screen.Messages.route)
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->

        if (showComments && selectedPostId != null) {
            val post = uiState.posts.find { it.id == selectedPostId }
            if (post != null) {
                CommentBottomSheet(
                    post = post,
                    sheetState = sheetState,
                    currentUserAvatarUrl = uiState.user?.avatarUrl.orEmpty(),
                    onDismiss = { showComments = false },
                    onSendComment = { text ->
                        viewModel.addComment(post.id, text)
                    }
                )
            }
        }

        when {
            uiState.isLoading -> {
                LazyColumn(
                    modifier = Modifier.padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(3) {
                        FeedPostSkeleton()
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }

            uiState.posts.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(settings.t("No posts yet", "Chưa có bài đăng nào"), color = AppTheme.colors.textSecondary)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.posts, key = { it.id }) { post ->
                        FeedPostItem(
                            post = post,
                            isLiked = uiState.likedPosts[post.id] ?: false,
                            isSaved = savedUiState.savedPostIds.contains(post.id),
                            isVerified = post.authorName == "mina",
                            isLikePending = post.id in uiState.pendingLikePostIds,
                            onLikeClick = { viewModel.toggleLike(post.id) },
                            onSaveClick = { savedViewModel.toggleSave(post.id) },
                            onCommentClick = {
                                selectedPostId = post.id
                                showComments = true
                            },
                            onHeaderClick = {
                                if (post.authorId.isNotBlank()) {
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
            }
        }
    }
}
