package com.example.fashionapp.ui.app.productdetail

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
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
import com.example.fashionapp.model.Product
import com.example.fashionapp.model.ProductVariant

// ── Palette ───────────────────────────────────────────────────────────────────
private val PrimaryBlue   = Color(0xFF3669C9)
private val StarYellow    = Color(0xFFFFC107)
private val TagBg         = Color(0xFFF0F4FF)
private val TagText       = Color(0xFF3669C9)
private val SectionBg     = Color(0xFFF7F8FA)
private val TextDark      = Color(0xFF1A1A1A)
private val TextGray      = Color(0xFF888888)
private val DividerColor  = Color(0xFFEEEEEE)
private val GreenShipping = Color(0xFF00B248)

// ─────────────────────────────────────────────────────────────────────────────
//  SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    navController: NavController,
    viewModel: ProductDetailViewModel = viewModel(
        factory = ProductDetailViewModelFactory(productId)
    )
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
        return
    }

    val product = state.product
    if (product == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.error ?: "Có lỗi xảy ra", color = TextGray)
        }
        return
    }

    // ── Bottom sheet state ─────────────────────────────────────────────────
    var showAddToCart by remember { mutableStateOf(false) }
    var showBuyNow    by remember { mutableStateOf(false) }

    if (showAddToCart) {
        AddQuantity (
            product   = product,
            isBuyNow  = false,
            onDismiss = { showAddToCart = false },
            onConfirm = { _, _ -> /* TODO: thêm vào cart */ }
        )
    }
    if (showBuyNow) {
        AddQuantity (
            product   = product,
            isBuyNow  = true,
            onDismiss = { showBuyNow = false },
            onConfirm = { _, _ -> /* TODO: navigate tới checkout */ }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── Ảnh sản phẩm (pager) ────────────────────────────────────
            item {
                ImageSection(
                    imageUrls       = product.imageUrls,
                    selectedIndex   = state.selectedImageIndex,
                    isWishlisted    = state.isWishlisted,
                    onBack          = { navController.popBackStack() },
                    onWishlist      = { viewModel.toggleWishlist() },
                    onImageSelected = { viewModel.selectImage(it) }
                )
            }

            // ── Giá + mô tả ─────────────────────────────────────────────
            item {
                PriceSection(product = product)
            }

            // ── Variations ───────────────────────────────────────────────
            if (product.variants.isNotEmpty()) {
                item {
                    VariationsSection(
                        variants        = product.variants,
                        selectedVariant = state.selectedVariant,
                        onSelect        = { viewModel.selectVariant(it) }
                    )
                }
            }

            // ── Specifications ───────────────────────────────────────────
            if (product.specifications.isNotEmpty()) {
                item { SpecificationsSection(specs = product.specifications) }
            }

            // ── Delivery ─────────────────────────────────────────────────
            item { DeliverySection(freeShipping = product.freeShipping) }

            // ── Rating & Reviews (mock) ───────────────────────────────────
            item { 
                RatingSection(
                    product = product, 
                    navController = navController 
                ) 
            }

            // ── Most Popular ─────────────────────────────────────────────
            if (state.relatedProducts.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = SectionBg, thickness = 8.dp)
                    Spacer(Modifier.height(16.dp))
                    MostPopularSection(
                        products    = state.relatedProducts,
                        navController = navController
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // ── Bottom Bar cố định ───────────────────────────────────────────
        BottomActionBar(
            product         = product,
            selectedVariant = state.selectedVariant,
            onAddToCart     = { showAddToCart = true },
            onBuyNow        = { showBuyNow = true },
            modifier        = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  IMAGE SECTION
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageSection(
    imageUrls: List<String>,
    selectedIndex: Int,
    isWishlisted: Boolean,
    onBack: () -> Unit,
    onWishlist: () -> Unit,
    onImageSelected: (Int) -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount   = { imageUrls.size.coerceAtLeast(1) }
    )

    LaunchedEffect(pagerState.currentPage) {
        onImageSelected(pagerState.currentPage)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        // Ảnh chính
        if (imageUrls.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) { Text("📷", fontSize = 48.sp) }
        } else {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model              = imageUrls[page].ifBlank { null },
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop,
                    error              = painterResource(R.drawable.ic_launcher_foreground)
                )
            }
        }

        // Gradient overlay dưới cùng
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f))
                    )
                )
        )

        // Back button
        IconButton(
            onClick  = onBack,
            modifier = Modifier
                .padding(12.dp)
                .size(38.dp)
                .align(Alignment.TopStart)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.85f))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextDark)
        }

        // Wishlist + Share buttons
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick  = onWishlist,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f))
            ) {
                Icon(
                    imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Wishlist",
                    tint = if (isWishlisted) Color(0xFFF04957) else TextDark
                )
            }
            IconButton(
                onClick  = { },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f))
            ) {
                Icon(Icons.Default.Share, null, tint = TextDark, modifier = Modifier.size(18.dp))
            }
        }

        // Dot indicator
        if (imageUrls.size > 1) {
            Row(
                modifier              = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                imageUrls.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == pagerState.currentPage) 8.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == pagerState.currentPage) Color.White
                                else Color.White.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PRICE SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PriceSection(product: Product) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // Giá + share icon
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text       = "₫${formatPrice(product.price)}",
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 26.sp,
                color      = TextDark,
                modifier   = Modifier.weight(1f)
            )
            if (product.discountPercent > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFFEBEB)
                ) {
                    Text(
                        "-${product.discountPercent}%",
                        color    = Color(0xFFCC0000),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Giá gốc (nếu có giảm giá)
        if (product.discountPercent > 0 && product.originalPrice > 0) {
            Text(
                text  = "₫${formatPrice(product.originalPrice)}",
                color = TextGray,
                fontSize = 13.sp,
                style = androidx.compose.ui.text.TextStyle(
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                )
            )
        }

        Spacer(Modifier.height(10.dp))

        // Mô tả
        Text(
            text      = product.description,
            fontSize  = 14.sp,
            color     = TextGray,
            lineHeight= 21.sp
        )

        Spacer(Modifier.height(8.dp))

        // Sold count + rating tóm tắt
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, null, tint = StarYellow, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text("${product.rating}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(" (${product.reviewCount})", fontSize = 12.sp, color = TextGray)
            }
            Text("•", color = TextGray)
            Text("Đã bán ${product.soldCount}", fontSize = 12.sp, color = TextGray)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  VARIATIONS SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VariationsSection(
    variants: List<ProductVariant>,
    selectedVariant: ProductVariant?,
    onSelect: (ProductVariant) -> Unit,
) {
    // Nhóm variants theo màu để hiển thị thumbnail màu
    val sizes  = variants.map { it.size }.distinct()
    val colors = variants.map { it.color }.distinct()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Variations", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.width(12.dp))
            // Hiển thị màu + size đang chọn
            selectedVariant?.let { v ->
                Text(v.color, color = TextGray, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = TagBg
                ) {
                    Text(v.size, color = TagText, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Ô chọn variant (thumbnail màu + size)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(variants) { variant ->
                val isSelected = variant.id == selectedVariant?.id
                VariantChip(
                    variant    = variant,
                    isSelected = isSelected,
                    onClick    = { onSelect(variant) }
                )
            }
        }
    }

    HorizontalDivider(color = DividerColor)
}

@Composable
private fun VariantChip(
    variant: ProductVariant,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier          = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    try { Color(android.graphics.Color.parseColor(variant.colorHex)) }
                    catch (_: Exception) { Color(0xFFEEEEEE) }
                )
                .then(
                    if (isSelected) Modifier.border(2.5.dp, PrimaryBlue, RoundedCornerShape(10.dp))
                    else Modifier
                )
        ) {
            if (isSelected) {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(variant.size, fontSize = 11.sp, color = if (isSelected) PrimaryBlue else TextGray)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SPECIFICATIONS SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpecificationsSection(specs: Map<String, String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text("Specifications", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))

        specs.forEach { (key, value) ->
            Text(key, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TextDark)
            Spacer(Modifier.height(6.dp))
            // Hiển thị value dưới dạng chip(s) — tách bằng dấu phẩy nếu nhiều
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(value.split(",").map { it.trim() }.filter { it.isNotEmpty() }) { v ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = TagBg
                    ) {
                        Text(
                            v,
                            color    = TagText,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    HorizontalDivider(color = DividerColor)
}

// ─────────────────────────────────────────────────────────────────────────────
//  DELIVERY SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeliverySection(freeShipping: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text("Delivery", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))

        // Standard
        DeliveryRow(
            label    = "Standard",
            duration = "5-7 ngày",
            price    = if (freeShipping) "Miễn phí" else "₫30.000",
            isFree   = freeShipping,
            color    = Color(0xFF3669C9)
        )
        Spacer(Modifier.height(10.dp))
        // Express
        DeliveryRow(
            label    = "Express",
            duration = "1-2 ngày",
            price    = "₫80.000",
            isFree   = false,
            color    = Color(0xFFFF6B00)
        )
    }
    HorizontalDivider(color = DividerColor)
}

@Composable
private fun DeliveryRow(
    label: String,
    duration: String,
    price: String,
    isFree: Boolean,
    color: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TextDark)
        Spacer(Modifier.width(10.dp))
        Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.12f)) {
            Text(duration, color = color, fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(
            text       = price,
            fontWeight = FontWeight.Bold,
            fontSize   = 14.sp,
            color      = if (isFree) GreenShipping else TextDark
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  RATING & REVIEWS SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RatingSection(product: Product, navController: NavController) {
    val rating = product.rating
    val reviewCount = product.reviewCount
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text("Rating & Reviews", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))

        // Tổng rating
        Row(verticalAlignment = Alignment.CenterVertically) {
            StarRow(rating = rating)
            Spacer(Modifier.width(8.dp))
            Text(
                "${rating}/5",
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp,
                color      = TextDark
            )
        }

        Spacer(Modifier.height(14.dp))

        // Mock review
        MockReviewItem(
            name   = "Khách hàng",
            rating = rating.coerceAtMost(5f),
            text   = "Sản phẩm rất đẹp, chất lượng tốt, giao hàng nhanh. Sẽ ủng hộ shop lần sau!"
        )

        Spacer(Modifier.height(14.dp))

        // View all button
        OutlinedButton(
            onClick = { navController.navigate("reviews/${product.id}") },
            modifier = Modifier.fillMaxWidth(),
            shape  = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue)
        ) {
            Text("Xem tất cả $reviewCount đánh giá", fontWeight = FontWeight.Medium)
        }
    }
    HorizontalDivider(color = DividerColor)
}

@Composable
private fun StarRow(rating: Float) {
    Row {
        val full = rating.toInt()
        repeat(5) { i ->
            Icon(
                imageVector = if (i < full) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = StarYellow,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MockReviewItem(name: String, rating: Float, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        // Avatar
        Box(
            modifier         = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(name.firstOrNull()?.toString() ?: "K", color = PrimaryBlue, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            StarRow(rating = rating.coerceAtMost(5f))
            Spacer(Modifier.height(4.dp))
            Text(
                text     = text,
                fontSize = 13.sp,
                color    = TextGray,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  MOST POPULAR SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MostPopularSection(products: List<Product>, navController: NavController) {
    Column {
        // Header
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Most Popular", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("See All", color = PrimaryBlue, fontSize = 13.sp)
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier         = Modifier.size(24.dp).clip(CircleShape).background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null,
                        tint = Color.White, modifier = Modifier.size(13.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Horizontal row
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products.take(6), key = { it.id }) { p ->
                PopularProductCard(
                    product  = p,
                    onClick  = { navController.navigate("product_detail/${p.id}") }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Grid 2 cột
        Column(
            modifier            = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            products.drop(6).chunked(2).forEach { row ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { p ->
                        GridProductCard(
                            product  = p,
                            modifier = Modifier.weight(1f),
                            onClick  = { navController.navigate("product_detail/${p.id}") }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PopularProductCard(product: Product, onClick: () -> Unit) {
    Column(modifier = Modifier.width(130.dp).clickable { onClick() }) {
        Box(
            modifier = Modifier.fillMaxWidth().height(130.dp)
                .clip(RoundedCornerShape(12.dp)).background(Color(0xFFEEEEEE))
        ) {
            AsyncImage(
                model = product.imageUrl.ifBlank { null },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_launcher_foreground)
            )
            // Badge
            Row(
                modifier = Modifier.align(Alignment.BottomStart).padding(5.dp)
                    .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("❤", fontSize = 9.sp)
                Spacer(Modifier.width(2.dp))
                Text("${product.soldCount}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(product.name, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = TextGray)
        Text("₫${formatPrice(product.price)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun GridProductCard(product: Product, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.85f)
                .clip(RoundedCornerShape(14.dp)).background(Color(0xFFEEEEEE))
        ) {
            AsyncImage(
                model = product.imageUrl.ifBlank { null },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_launcher_foreground)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(product.name, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = TextGray)
        Text("₫${formatPrice(product.price)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BOTTOM ACTION BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BottomActionBar(
    product: Product,
    selectedVariant: ProductVariant?,
    onAddToCart: () -> Unit,
    onBuyNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier      = modifier.fillMaxWidth(),
        color         = Color.White,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Add to cart
            OutlinedButton(
                onClick  = onAddToCart,
                modifier = Modifier.weight(1f).height(50.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                border   = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryBlue)
            ) {
                Text("Add to cart", fontWeight = FontWeight.SemiBold)
            }

            // Buy now
            Button(
                onClick  = onBuyNow,
                modifier = Modifier.weight(1f).height(50.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Buy now", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HELPER
// ─────────────────────────────────────────────────────────────────────────────

private fun formatPrice(price: Double): String {
    return "%.0f".format(price).reversed().chunked(3).joinToString(".").reversed()
}