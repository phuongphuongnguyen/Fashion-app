package com.example.fashionapp.ui.app.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.fashionapp.R
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.components.CommentBottomSheet
import com.example.fashionapp.ui.components.FeedPostItem
//import com.example.fashionapp.ui.components.MainTopBar
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.app.saved.SavedViewModel
import com.example.fashionapp.ui.components.FashionTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(),
    savedViewModel: SavedViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = LocalAppSettings.current
    val isDark = settings.isDarkMode
    val savedUiState by savedViewModel.uiState.collectAsState()

    // Dynamic styling
    val bgColor = if (isDark) Color(0xFF121212) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1A1A2E)
    val dividerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE)

    var showComments by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = { 
            FashionTopBar(
                title = settings.t(
                    en = "Feed",
                    vi = "Bảng tin",
                    fr = "Flux",
                    ja = "フィード",
                    ko = "피드",
                    zh = "动态"
                ),
                isDark = isDark,
                bgColor = if (isDark) Color(0xFF1E1E1E) else Color.White,
                textColor = textColor,
                actions = {
                    IconButton(onClick = { /* TODO: Implement Search */ }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.Black
                        )
                    }

                    IconButton(onClick = { navController.navigate(Screen.Chatbot.route) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chatbot),
                            contentDescription = "Chatbot",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        },
        containerColor = bgColor
    ) { innerPadding ->
        
        if (showComments && selectedPostId != null) {
            val post = uiState.posts.find { it.id == selectedPostId }
            if (post != null) {
                CommentBottomSheet(
                    post = post,
                    sheetState = sheetState,
                    onDismiss = { showComments = false },
                    onSendComment = { text ->
                        viewModel.addComment(post.id, text)
                    }
                )
            }
        }

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF3669C9))
                }
            }

            uiState.posts.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text(
                        text = settings.t(
                            en = "No posts available",
                            vi = "Chưa có bài đăng nào",
                            fr = "Aucune publication",
                            ja = "投稿はありません",
                            ko = "게시물이 없습니다",
                            zh = "暂无动态"
                        ),
                        color = if (isDark) Color(0xFF888888) else Color.Gray
                    )
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
                            isLiked = (uiState.likedPosts[post.id] ?: false),
                            isSaved = savedUiState.savedPostIds.contains(post.id),
                            isVerified = (post.authorName == "mina"),
                            onLikeClick = { viewModel.toggleLike(post.id) },
                            onSaveClick = { savedViewModel.toggleSave(post.id) },
                            onCommentClick = {
                                selectedPostId = post.id
                                showComments = true
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                    }
                }
            }
        }
    }
}
