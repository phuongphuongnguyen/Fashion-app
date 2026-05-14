package com.example.fashionapp.ui.app.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.fashionapp.ui.components.CommentBottomSheet
import com.example.fashionapp.ui.components.FeedPostItem
import com.example.fashionapp.ui.components.MainTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var showComments by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = { 
            MainTopBar(
                title = "Feed",
                onNotiClick = { },
                onMessClick = { },
                onCartClick = { }
            )
        },
        containerColor = Color.White
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
                    CircularProgressIndicator()
                }
            }

            uiState.posts.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("Chưa có bài đăng nào", color = Color.Gray)
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
                            onLikeClick = { viewModel.toggleLike(post.id) },
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
    }
}
