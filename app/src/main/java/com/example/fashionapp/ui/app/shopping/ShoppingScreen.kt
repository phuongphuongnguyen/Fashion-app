package com.example.fashionapp.ui.app.shopping

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fashionapp.data.MockData
import com.example.fashionapp.model.Post
import com.example.fashionapp.model.Product
import com.example.fashionapp.ui.app.home.HomeViewModel
import com.example.fashionapp.ui.components.CommentBottomSheet
import com.example.fashionapp.ui.components.FeedPostItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sourcePosts = uiState.posts.ifEmpty { MockData.feedPosts }
    val shopPosts = remember(sourcePosts) { sourcePosts.toShopPosts() }

    var selectedTab by remember { mutableStateOf("Posts") }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    var showComments by remember { mutableStateOf(false) }
    var localLikedPosts by remember { mutableStateOf(setOf<String>()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        if (showComments && selectedPostId != null) {
            shopPosts.find { it.id == selectedPostId }?.let { post ->
                CommentBottomSheet(
                    post = post,
                    sheetState = sheetState,
                    onDismiss = { showComments = false },
                    onSendComment = { text -> viewModel.addComment(post.id, text) }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ShopHeader(
                selectedTab = selectedTab,
                postCount = shopPosts.size,
                onTabSelected = { selectedTab = it }
            )

            if (selectedTab == "Posts") {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(shopPosts, key = { it.id }) { post ->
                        FeedPostItem(
                            post = post,
                            isLiked = (uiState.likedPosts[post.id] ?: false) ||
                                localLikedPosts.contains(post.id),
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
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(MockData.products, key = { it.id }) { product ->
                        ProductTile(product = product)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopHeader(
    selectedTab: String,
    postCount: Int,
    onTabSelected: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .border(1.dp, Color(0xFFE0E0E0), CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text("LSOUL", fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Lsoul", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(postCount.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(" Posts", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(" 4.5", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(12.dp))
                    Text("800 ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Followers", fontSize = 12.sp, color = Color.Gray)
                }
            }
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1769FF)),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Follow", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            listOf("Posts", "Products").forEach { tab ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onTabSelected(tab) }
                ) {
                    Text(
                        text = tab,
                        color = if (selectedTab == tab) Color.Black else Color(0xFF9A9A9A),
                        fontSize = 16.sp,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(2.dp)
                            .background(if (selectedTab == tab) Color.Black else Color.Transparent)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProductTile(product: Product) {
    Column {
        AsyncImage(
            model = product.imageUrl,
            contentDescription = product.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.78f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF1F1F1)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(6.dp))
        Text(product.name, maxLines = 1, fontSize = 12.sp)
        Text("$${" %.2f".format(product.price).trim()}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

private fun List<Post>.toShopPosts(): List<Post> {
    val fallbackProducts = MockData.products
    return if (isNotEmpty()) {
        mapIndexed { index, post ->
            val fallbackProduct = fallbackProducts[index % fallbackProducts.size]
            post.copy(
                authorName = "Lsoul",
                authorAvt = "",
                caption = post.caption.ifBlank { fallbackProduct.name },
                imageUrls = post.imageUrls.ifEmpty { listOf(fallbackProduct.imageUrl) },
                likeCount = if (post.likeCount > 0) post.likeCount else 287_698L
            )
        }
    } else {
        fallbackProducts.take(2).mapIndexed { index, product ->
            Post(
                id = "shop-product-$index",
                authorId = "shop-lsoul",
                authorName = "Lsoul",
                caption = product.name,
                imageUrls = listOf(product.imageUrl),
                likeCount = 287_698L,
                commentCount = 24L
            )
        }
    }
}
