package com.example.fashionapp.ui.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fashionapp.R
import com.example.fashionapp.model.Post
import com.example.fashionapp.model.ProductTag
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { FeedTopBar() },
        containerColor = Color.White
    ) { innerPadding ->

        when {
            uiState.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            uiState.posts.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { Text("Chưa có bài đăng nào", color = Color.Gray) }
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
                            onLikeClick = { viewModel.toggleLike(post.id) }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))
                    }
                }
            }
        }
    }
}

// ── Top Bar ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedTopBar() {
    TopAppBar(
        title = {
            Text("Feed", fontWeight = FontWeight.Bold, fontSize = 24.sp)
        },
        actions = {
            Row(
                modifier = Modifier.padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((-6).dp) // Giảm khoảng cách giữa các IconButton
            ) {
                IconButton(onClick = { /* notification */ }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_noti),
                        contentDescription = "Notifications",
                        modifier = Modifier.size(34.dp),
                        tint = Color.Unspecified
                    )
                }
                IconButton(onClick = { /* message */ }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mess),
                        contentDescription = "Messages",
                        modifier = Modifier.size(34.dp),
                        tint = Color.Unspecified
                    )
                }
                IconButton(onClick = { /* cart */ }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_cart),
                        contentDescription = "Cart",
                        modifier = Modifier.size(34.dp),
                        tint = Color.Unspecified
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

// ── Feed Post Item ────────────────────────────────────────

@Composable
fun FeedPostItem(
    post: Post,
    isLiked: Boolean,
    onLikeClick: () -> Unit
) {
    Column {
        // Header: avatar + username + menu
        PostHeader(
            avatarUrl = post.avatarUrl,
            username = post.username
        )

        // Ảnh outfit chính
        AsyncImage(
            model = post.imageUrl,
            contentDescription = "Post image",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFFEEEEEE)), // Màu nền xám nhạt khi đang load
            contentScale = ContentScale.Crop,
            // Thêm crossfade để hiệu ứng hiện ảnh mượt hơn
            // Nếu link lỗi, sẽ hiện icon mặc định
            error = painterResource(R.drawable.ic_launcher_foreground)
        )

        // Product tags (thumbnail nhỏ)
        if (post.productTags.isNotEmpty()) {
            ProductTagsRow(tags = post.productTags)
        }

        // Action bar: like, comment, share + bookmark
        ActionRow(
            isLiked = isLiked,
            onLikeClick = onLikeClick
        )

        // Số lượt like
        Text(
            text = "${formatLikeCount(post.likeCount)} Likes",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(4.dp))

        // Caption + ngày
        CaptionRow(
            username = post.username,
            caption = post.caption,
            timestamp = post.createdAt?.toDate()
        )

        Spacer(Modifier.height(8.dp))
    }
}

// ── Post Header ───────────────────────────────────────────

@Composable
private fun PostHeader(avatarUrl: String, username: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar với ring màu gradient giả lập
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF6B6B))  // ring placeholder
                .padding(2.dp)
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                // placeholder khi chưa load xong
                error = painterResource(R.drawable.ic_profile)
            )
        }

        Spacer(Modifier.width(10.dp))

        Text(
            text = username,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = { /* show menu */ }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Black)
        }
    }
}

// ── Product Tags ──────────────────────────────────────────

@Composable
private fun ProductTagsRow(tags: List<ProductTag>) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.take(4).forEach { tag ->  // tối đa 4 thumbnail
            AsyncImage(
                model = tag.imageUrl,
                contentDescription = "Product",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF0F0F0)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_launcher_foreground),
                error = painterResource(R.drawable.ic_launcher_foreground)
            )
        }
    }
}

// ── Action Row ────────────────────────────────────────────

@Composable
private fun ActionRow(
    isLiked: Boolean,
    onLikeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Like
        IconButton(onClick = onLikeClick) {
            Icon(
                painter = painterResource(
                    if (isLiked) R.drawable.ic_like_full else R.drawable.ic_like
                ),
                contentDescription = "Like",
                tint = if (isLiked) Color(0xFFF04957) else Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }

        // Comment
        IconButton(onClick = { }) {
            Icon(
                painter = painterResource(R.drawable.ic_comment),
                contentDescription = "Comment",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }

        // Share
        IconButton(onClick = { }) {
            Icon(
                painter = painterResource(R.drawable.ic_share),
                contentDescription = "Share",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }

        // Dot indicator (hai chấm như ảnh)
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3669C9))
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Bookmark
        var bookmarked by remember { mutableStateOf(false) }
        IconButton(onClick = { bookmarked = !bookmarked }) {
            Icon(
                painter = painterResource(
                    if (bookmarked) R.drawable.ic_bookmark_full else R.drawable.ic_bookmark
                ),
                contentDescription = "Bookmark",
                tint = Color.Black,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ── Caption Row ───────────────────────────────────────────

@Composable
private fun CaptionRow(
    username: String,
    caption: String,
    timestamp: Date?
) {
    val dateStr = timestamp?.let {
        SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).format(it)
    } ?: ""

    Row(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$username ",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        Text(
            text = caption,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }

    if (dateStr.isNotEmpty()) {
        Spacer(Modifier.height(2.dp))
        Text(
            text = "$dateStr •",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

// ── Helpers ───────────────────────────────────────────────

private fun formatLikeCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%,.0f", count.toDouble())
        else -> count.toString()
    }
}