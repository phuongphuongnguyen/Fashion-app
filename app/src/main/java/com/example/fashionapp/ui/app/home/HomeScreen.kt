package com.example.fashionapp.ui.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import com.example.fashionapp.R
import com.example.fashionapp.data.mock.MockDataProvider
import com.example.fashionapp.data.model.Post
import com.example.fashionapp.data.model.Story
import com.example.fashionapp.data.model.User
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Feed",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        )
                    )
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        IconButton(onClick = { /* TODO */ }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_cart),
                                contentDescription = "Cart",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        IconButton(onClick = { /* TODO */ }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_messages),
                                contentDescription = "Messages",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        IconButton(onClick = { /* TODO */ }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_notify),
                                contentDescription = "Notifications",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
                .padding(innerPadding)
        ) {
            // Danh sách Feed Bài đăng
            items(MockDataProvider.feedPosts) { post ->
                val author = MockDataProvider.getUserById(post.authorId)
                author?.let {
                    PostItem(post = post, author = it)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // Khoảng trống dưới cùng
            item { 
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}





@Composable
fun PostItem(post: Post, author: User) {
    var isLiked by remember { mutableStateOf(post.isLiked) }
    var likeCount by remember { mutableStateOf(post.likeCount) }
    var isSaved by remember { mutableStateOf(post.isSaved) }
    var showComments by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = author.avatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = author.username,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (author.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = Color(0xFF3897F0),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    if (author.location.isNotEmpty()) {
                        Text(
                            text = author.location,
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            }

            // Image Carousel
            val pagerState = rememberPagerState(pageCount = { post.images.size })
            Box(contentAlignment = Alignment.BottomCenter) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.85f)
                ) { page ->
                    AsyncImage(
                        model = post.images[page],
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Indicators
                if (post.images.size > 1) {
                    Row(
                        Modifier
                            .height(24.dp)
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(post.images.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(6.dp)
                            )
                        }
                    }
                }
            }

            // Tagged Products Row (Small circles below image)
            if (post.taggedProducts.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(post.taggedProducts) { taggedProduct ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.5.dp, Color(0xFFEEEEEE), CircleShape)
                                .padding(3.dp)
                                .clickable { /* TODO: Navigate to taggedProduct.productId */ }
                        ) {
                            AsyncImage(
                                model = taggedProduct.thumbnailUrl,
                                contentDescription = taggedProduct.label,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    isLiked = !isLiked
                    if (isLiked) likeCount++ else likeCount--
                }) {
                    Icon(
                        painter = painterResource(
                            id = if (isLiked)
                                R.drawable.ic_like_fill
                            else
                                R.drawable.ic_like
                        ),
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = { showComments = !showComments }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_comment),
                        contentDescription = "Comment",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = { /* TODO */ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_share),
                        contentDescription = "Share",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { isSaved = !isSaved }) {
                    Icon(
                        painter = painterResource(
                            id = if (isSaved)
                                R.drawable.ic_bookmark_fill
                            else
                                R.drawable.ic_bookmark
                        ),
                        contentDescription = "Save",
                        tint = if (isSaved) Color.Black else Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Likes & Caption
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = "${String.format(Locale.getDefault(), "%,d", likeCount).replace(',', '.')} Likes",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(4.dp))

                val annotatedCaption = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(author.username)
                    }
                    append(" ")
                    append(post.caption)
                }
                
                Text(
                    text = annotatedCaption,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3
                )

                if (post.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = post.tags.joinToString(" ") { "#$it" },
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF00376B)),
                        modifier = Modifier.clickable { /* TODO */ }
                    )
                }

                if (post.commentCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (showComments) "Hide comments" else "View all ${post.commentCount} comments",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                        modifier = Modifier.clickable { showComments = !showComments }
                    )
                }

                if (showComments) {
                    val comments = MockDataProvider.getCommentsForPost(post.id)
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        comments.forEach { comment ->
                            val commentAuthor = MockDataProvider.getUserById(comment.authorId)
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(commentAuthor?.username ?: "user")
                                    }
                                    append(" ")
                                    append(comment.content)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = post.createdAt,
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.sp)
                )
            }
        }
    }
}
