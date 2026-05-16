package com.example.fashionapp.ui.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fashionapp.data.MockData
import com.example.fashionapp.model.Post
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.components.CommentBottomSheet
import com.example.fashionapp.ui.components.formatLikeCount
import androidx.compose.material3.rememberModalBottomSheetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val displayPosts = uiState.posts.ifEmpty { MockData.feedPosts }

    var showComments by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(containerColor = Color.White) { innerPadding ->
        if (showComments && selectedPostId != null) {
            displayPosts.find { it.id == selectedPostId }?.let { post ->
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
            contentPadding = PaddingValues(bottom = 10.dp)
        ) {
            item {
                HomeTopBar(navController = navController)
            }

            items(displayPosts, key = { it.id }) { post ->
                HomeFeedCard(
                    post = post,
                    isLiked = uiState.likedPosts[post.id] ?: false,
                    onLikeClick = { viewModel.toggleLike(post.id) },
                    onCommentClick = {
                        selectedPostId = post.id
                        showComments = true
                    }
                )
            }
        }
    }
}

@Composable
fun HomeTopBar(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "FashionApp",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color.Black
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { navController.navigate(Screen.Shop.route) }) {
            Icon(Icons.Outlined.Search, contentDescription = "Search", tint = Color.Black)
        }
        IconButton(onClick = { }) {
            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Messages", tint = Color.Black)
        }
    }
}

@Composable
fun StoriesRow(posts: List<Post>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(posts) { post ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = post.authorAvt,
                    contentDescription = post.authorName,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFFFF5F5F), CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = post.authorName.ifBlank { "mina" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun HomeFeedCard(
    post: Post,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Author row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = post.authorAvt,
                contentDescription = post.authorName,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color(0xFFFF5F5F), CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(8.dp))
            Text(
                post.authorName.ifBlank { "mina" },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(22.dp))
        }

        // Post image – full width, square
        AsyncImage(
            model = post.imageUrls.firstOrNull(),
            contentDescription = post.caption,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFFF2F2F2)),
            contentScale = ContentScale.Crop
        )

        // Action row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onLikeClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color(0xFFE94242) else Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onCommentClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment", tint = Color.Black, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Send, contentDescription = "Share", tint = Color.Black, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.weight(1f))
            // Dots indicator
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(end = 4.dp)) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == 0) 6.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (i == 0) Color(0xFF1769FF) else Color(0xFFCCCCCC))
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Save", tint = Color.Black, modifier = Modifier.size(22.dp))
            }
        }

        // Likes count
        Text(
            text = "${formatLikeCount(post.likeCount)} Likes",
            modifier = Modifier.padding(horizontal = 16.dp),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
        // Caption
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append(post.authorName.ifBlank { "mina" })
                }
                append(" ")
                append(post.caption.ifBlank { "My outfit is perfect!" })
                append(" ")
                withStyle(SpanStyle(color = Color(0xFF7C7C7C))) { append("June 7, 2021 \u2022") }
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp),
            fontSize = 12.sp
        )
        Spacer(Modifier.height(4.dp))
    }
}
