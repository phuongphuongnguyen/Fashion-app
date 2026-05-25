package com.example.fashionapp.ui.app.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.example.fashionapp.model.Category
import com.example.fashionapp.model.Product
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.components.ShoppingTopBar

// ── Palette ───────────────────────────────────────────────────────────────────
private val PrimaryBlue  = Color(0xFF3669C9)
private val BgGray       = Color(0xFFF7F8FA)
private val CardBg       = Color(0xFFEEEEEE)
private val TextDark     = Color(0xFF1A1A1A)
private val TextGray     = Color(0xFF888888)
private val FlashStart   = Color(0xFFFF9500)
private val FlashEnd     = Color(0xFFFFCC00)

// ── Fallback categories khi Firestore trả về rỗng ─────────────────────────────
private val fallbackCategories = listOf(
    Category("c1", "Clothing"),
    Category("c2", "Shoes"),
    Category("c3", "Bags"),
    Category("c4", "Lingerie"),
    Category("c5", "Watch"),
    Category("c6", "Hoodies"),
)

// ─────────────────────────────────────────────────────────────────────────────
//  SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    navController: NavController,
    viewModel: ShoppingViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar    = { 
            ShoppingTopBar(
                scrollBehavior = scrollBehavior,
                onSearchClick = {
                    navController.navigate(Screen.Search.createRoute())
                },
                onCartClick = {
                    navController.navigate(Screen.Cart.route)
                }
            )
        },
        containerColor = BgGray,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = PrimaryBlue) }
            return@Scaffold
        }

        LazyColumn(
            modifier        = Modifier.padding(innerPadding),
            contentPadding  = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Flash Sale Banner ──────────────────────────────────────────
            item { FlashSaleBanner() }

            // ── Categories ────────────────────────────────────────────────
            item {
                val cats = state.categories.ifEmpty { fallbackCategories }
                SectionHeader(
                    title = "Categories",
                    onSeeAll = {}
                )
                Spacer(Modifier.height(12.dp))
                CategoriesGrid(
                    categories = cats,
                    onCategoryClick = { cat ->
                        navController.navigate(Screen.Search.createRoute(categoryId = cat.id))
                    }
                )
            }

            // ── New Items ─────────────────────────────────────────────────
            item {
                SectionHeader(title = "New Items", onSeeAll = {})
                Spacer(Modifier.height(12.dp))
                ProductRow(
                    products = state.newItems,
                    onProductClick = { p ->
                        navController.navigate(Screen.ProductDetail.createRoute(p.id))
                    }
                )
            }

            // ── Most Popular ──────────────────────────────────────────────
            item {
                SectionHeader(
                    title = "Most Popular",
                    onSeeAll = {}
                )
                Spacer(Modifier.height(12.dp))
                ProductRow(
                    products = state.mostPopular,
                    showBadge = true,
                    onProductClick = { p ->
                        navController.navigate(Screen.ProductDetail.createRoute(p.id))
                    }
                )
            }

            // ── Just For You ──────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text       = "Just For You",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        color      = TextDark
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("⭐", fontSize = 15.sp)
                }
                Spacer(Modifier.height(12.dp))
                ForYouGrid(
                    products = state.forYou,
                    onProductClick = { p ->
                        navController.navigate(Screen.ProductDetail.createRoute(p.id))
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  FLASH SALE BANNER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FlashSaleBanner() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(FlashStart, FlashEnd)))
    ) {
        // Nội dung bên trái
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
        ) {
            Text(
                "Flash Sale",
                color      = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 22.sp
            )
            Text(
                "Up to 50%",
                color    = Color.White.copy(alpha = 0.92f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.28f)
            ) {
                Text(
                    "Happening Now",
                    color    = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SECTION HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.clickable { onSeeAll() }
        ) {
            Text("See All", color = PrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(6.dp))
            Box(
                modifier            = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment    = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CATEGORIES GRID (2 cột)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoriesGrid(
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit = {}
) {
    Column(
        modifier            = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        categories.chunked(2).forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { cat ->
                    CategoryCard(
                        category = cat,
                        modifier = Modifier.weight(1f),
                        onClick  = { onCategoryClick(cat) }
                    )
                }
                if (row.size < 2) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // Grid 2x2 Preview Images
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        PreviewImage(category.previewImages.getOrNull(0), Modifier.weight(1f))
                        Spacer(Modifier.width(2.dp))
                        PreviewImage(category.previewImages.getOrNull(1), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(modifier = Modifier.weight(1f)) {
                        PreviewImage(category.previewImages.getOrNull(2), Modifier.weight(1f))
                        Spacer(Modifier.width(2.dp))
                        PreviewImage(category.previewImages.getOrNull(3), Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text       = category.name,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
                color      = Color.Black,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun PreviewImage(url: String?, modifier: Modifier) {
    if (url.isNullOrBlank()) {
        Box(modifier = modifier.fillMaxSize().background(Color(0xFFE0E0E0)))
    } else {
        AsyncImage(
            model           = url,
            contentDescription = null,
            modifier        = modifier.fillMaxSize(),
            contentScale    = ContentScale.Crop
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HORIZONTAL PRODUCT ROW  (New Items / Most Popular)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProductRow(
    products: List<Product>,
    showBadge: Boolean = false,
    onProductClick: (Product) -> Unit = {}
) {
    if (products.isEmpty()) {
        // Skeleton placeholder khi chưa có data
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(4) { SkeletonProductCard() }
        }
        return
    }

    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products, key = { it.id }) { product ->
            ProductCardVertical(
                product = product,
                showBadge = showBadge,
                onClick = { onProductClick(product) }
            )
        }
    }
}

@Composable
private fun ProductCardVertical(
    product: Product,
    showBadge: Boolean,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .width(145.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
            ) {
                AsyncImage(
                    model              = product.imageUrl.ifBlank { null },
                    contentDescription = product.name,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop,
                    error              = painterResource(R.drawable.ic_launcher_foreground)
                )
                // Badge: sold count + label
                if (showBadge) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("❤", fontSize = 9.sp)
                        Text(
                            text     = "${product.soldCount}",
                            color    = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (product.isSale) {
                            Text(
                                text     = "• Sale",
                                color    = Color(0xFFFFCC00),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text     = product.name,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color    = TextGray,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = "₫${formatPrice(product.price)}",
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp,
                color      = TextDark
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  JUST FOR YOU GRID  (2 cột)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ForYouGrid(
    products: List<Product>,
    onProductClick: (Product) -> Unit = {}
) {
    if (products.isEmpty()) {
        Column(
            modifier            = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(2) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(2) { SkeletonProductCardGrid(Modifier.weight(1f)) }
                }
            }
        }
        return
    }

    Column(
        modifier            = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        products.chunked(2).forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { product ->
                    ProductCardGrid(
                        product = product,
                        modifier = Modifier.weight(1f),
                        onClick = { onProductClick(product) }
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProductCardGrid(
    product: Product,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.85f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
            ) {
                AsyncImage(
                    model              = product.imageUrl.ifBlank { null },
                    contentDescription = product.name,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop,
                    error              = painterResource(R.drawable.ic_launcher_foreground)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text     = product.name,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color    = TextGray,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = "₫${formatPrice(product.price)}",
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp,
                color      = TextDark
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SKELETON PLACEHOLDERS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SkeletonProductCard() {
    Column(modifier = Modifier.width(140.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFE8E8E8))
        )
        Spacer(Modifier.height(7.dp))
        Box(Modifier.fillMaxWidth(0.8f).height(11.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE8E8E8)))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth(0.5f).height(11.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE8E8E8)))
    }
}

@Composable
private fun SkeletonProductCardGrid(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.82f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFE8E8E8))
        )
        Spacer(Modifier.height(7.dp))
        Box(Modifier.fillMaxWidth(0.8f).height(11.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE8E8E8)))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth(0.5f).height(11.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE8E8E8)))
    }
}

private fun formatPrice(price: Double): String =
    "%.0f".format(price).reversed().chunked(3).joinToString(".").reversed()
