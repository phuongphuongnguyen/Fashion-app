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
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import com.example.fashionapp.ui.components.FashionTopBar
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
import com.example.fashionapp.data.ShopProfile
import com.example.fashionapp.model.Post
import com.example.fashionapp.model.Product
import com.example.fashionapp.ui.app.home.HomeViewModel
import com.example.fashionapp.ui.app.shopping.ShoppingViewModel
import com.example.fashionapp.ui.app.saved.SavedViewModel
import com.example.fashionapp.ui.components.CommentBottomSheet
import com.example.fashionapp.ui.components.FeedPostItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel(),
    shoppingViewModel: ShoppingViewModel = viewModel(),
    savedViewModel: SavedViewModel = viewModel()
) {
    val homeState by homeViewModel.uiState.collectAsState()
    val shoppingState by shoppingViewModel.uiState.collectAsState()
    val savedUiState by savedViewModel.uiState.collectAsState()
    val shop = shoppingState.shop
    val sourcePosts = homeState.posts
    val shopPosts = remember(sourcePosts, shop?.ownerUserId, shop?.name, shop?.logoUrl) {
        sourcePosts.filter { post ->
            shop?.ownerUserId?.isNotBlank() == true && post.authorId == shop.ownerUserId
        }.map { post ->
            post.copy(
                authorName = shop?.name?.takeIf { it.isNotBlank() } ?: post.authorName,
                authorAvt = shop?.logoUrl?.takeIf { it.isNotBlank() } ?: post.authorAvt
            )
        }
    }
    val shopProducts = remember(shoppingState.products, shop?.id) {
        shoppingState.products.filter { product ->
            shop?.id.isNullOrBlank() || product.shopId == shop?.id
        }
    }

    var selectedTab by remember { mutableStateOf("Posts") }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    var showComments by remember { mutableStateOf(false) }
    var localLikedPosts by remember { mutableStateOf(setOf<String>()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            FashionTopBar(
                title = "",
                onBackClick = { navController.popBackStack() }
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
                    onSendComment = { text -> homeViewModel.addComment(post.id, text) }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ShopHeader(
                shop = shop,
                selectedTab = selectedTab,
                postCount = shopPosts.size,
                productCount = shopProducts.size,
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
                            isLiked = (homeState.likedPosts[post.id] ?: false) ||
                                localLikedPosts.contains(post.id),
                            isSaved = savedUiState.savedPostIds.contains(post.id),
                            onLikeClick = {
                                if (homeState.posts.any { it.id == post.id }) {
                                    homeViewModel.toggleLike(post.id)
                                } else {
                                    localLikedPosts =
                                        if (localLikedPosts.contains(post.id)) localLikedPosts - post.id
                                        else localLikedPosts + post.id
                                }
                            },
                            onSaveClick = { savedViewModel.toggleSave(post.id) },
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
                    items(shopProducts, key = { it.id }) { product ->
                        ProductTile(product = product)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopHeader(
    shop: ShopProfile?,
    selectedTab: String,
    postCount: Int,
    productCount: Int,
    onTabSelected: (String) -> Unit
) {
    val shopName = shop?.name?.takeIf { it.isNotBlank() } ?: "Shop"
    val logoUrl = shop?.logoUrl.orEmpty()
    val rating = shop?.rating ?: 0f
    val followers = shop?.followerCount ?: 0

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = logoUrl.ifBlank { null },
                contentDescription = shopName,
                modifier = Modifier
                    .size(68.dp)
                    .border(1.dp, Color(0xFFE0E0E0), CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        shopName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1769FF)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Follow", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricInline(postCount.toString(), "Posts")
                    MetricInline(productCount.toString(), "Products")
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(" ${"%.1f".format(rating)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    MetricInline(formatCount(followers), "Followers")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
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
            Icon(
                imageVector = Icons.Outlined.AddCircleOutline,
                contentDescription = "Add",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MetricInline(value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.width(3.dp))
        Text(label, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
    }
}

private fun formatCount(value: Int): String {
    return if (value >= 1000) {
        "${"%.1f".format(value / 1000.0)}K"
    } else {
        value.toString()
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
        Text(product.name, maxLines = 2, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text("$${"%.2f".format(product.price)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
