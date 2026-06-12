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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fashionapp.data.ProductReview
import com.example.fashionapp.R
import com.example.fashionapp.model.Product
import com.example.fashionapp.model.ProductVariant
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import com.example.fashionapp.data.StorageUrlResolver
import com.example.fashionapp.data.shop.ShopRepository
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.navigation.openProfileOrShop
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.theme.AppTheme

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
    val context = LocalContext.current
    val settings = LocalAppSettings.current

    LaunchedEffect(state.isAddedToCart, state.cartError) {
        when {
            state.isAddedToCart -> {
                android.widget.Toast.makeText(context, settings.t("Added to cart", "Đã thêm vào giỏ hàng"), android.widget.Toast.LENGTH_SHORT).show()
                viewModel.consumeCartResult()
                navController.navigate(Screen.Cart.route) {
                    launchSingleTop = true
                }
            }
            state.cartError != null -> {
                android.widget.Toast.makeText(context, state.cartError, android.widget.Toast.LENGTH_SHORT).show()
                viewModel.consumeCartResult()
            }
        }
    }

    LaunchedEffect(state.buyNowCartItemId) {
        state.buyNowCartItemId?.let { cartItemId ->
            viewModel.consumeBuyNow()
            navController.navigate(Screen.Payment.createRoute(setOf(cartItemId))) {
                launchSingleTop = true
            }
        }
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
        }
        return
    }

    val product = state.product
    if (product == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.error ?: settings.t("An error occurred", "Có lỗi xảy ra"), color = AppTheme.colors.textSecondary)
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
            onConfirm = { variant, quantity ->
                viewModel.addToCart(variant = variant, quantity = quantity)
            }
        )
    }
    if (showBuyNow) {
        AddQuantity (
            product   = product,
            isBuyNow  = true,
            onDismiss = { showBuyNow = false },
            onConfirm = { variant, quantity ->
                viewModel.buyNow(variant = variant, quantity = quantity)
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── Ảnh sản phẩm (pager) ────────────────────────────────────
            item {
                ImageSection(
                    imageUrls       = product.imageUrls,
                    selectedIndex   = state.selectedImageIndex,
                    onBack          = { navController.popBackStack() },
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
            // ── Shop Info ───────────────────────────────────────────
            item {
                ShopSection(
                    shopId   = product.shopId,
                    onVisitShop = { navController.openProfileOrShop(product.shopId) }
                )
            }

            // ── Rating & Reviews ──────────────────────────────────────────
            item { 
                ProductReviewSection(
                    product = product, 
                    reviews = state.productReviews,
                    navController = navController 
                ) 
            }

            // ── Most Popular ─────────────────────────────────────────────
            if (state.relatedProducts.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 8.dp)
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
            isLoading       = state.isAddingToCart,
            onAddToCart     = { showAddToCart = true },
            onBuyNow        = { showBuyNow = true },
            modifier        = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  IMAGE SECTION
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductReviewsScreen(
    productId: String,
    navController: NavController
) {
    val settings = LocalAppSettings.current
    val reviews by remember(productId) {
        ShopRepository.getReviewsForProductFlow(productId)
    }.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(settings.t("Product Reviews", "Đánh giá sản phẩm"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = settings.t("Back", "Quay lại"))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (reviews.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(settings.t("No reviews yet", "Chưa có đánh giá"), color = AppTheme.colors.textSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(reviews, key = { it.id }) { review ->
                    RealReviewItem(review = review)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageSection(
    imageUrls: List<String>,
    selectedIndex: Int,
    onBack: () -> Unit,
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
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) { Text("📷", fontSize = 48.sp) }
        } else {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model              = imageUrls[page].ifBlank { null },
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop,
                    error              = painterResource(R.drawable.ic_shopping)
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
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Black)
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
    val settings = LocalAppSettings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // Giá + share icon
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text       = "₫${formatPrice(product.price)}",
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 26.sp,
                color      = MaterialTheme.colorScheme.onBackground,
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
                color = AppTheme.colors.textSecondary,
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
            color     = AppTheme.colors.textSecondary,
            lineHeight= 21.sp
        )

        Spacer(Modifier.height(8.dp))

        // Sold count + rating tóm tắt
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, null, tint = AppTheme.colors.star, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text("${product.rating}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(" (${product.reviewCount})", fontSize = 12.sp, color = AppTheme.colors.textSecondary)
            }
            Text("•", color = AppTheme.colors.textSecondary)
            Text(settings.t("${product.soldCount} sold", "Đã bán ${product.soldCount}"), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
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
    val settings = LocalAppSettings.current
    // Nhóm variants theo màu để hiển thị thumbnail màu
    val sizes  = variants.map { it.size }.distinct()
    val colors = variants.map { it.color }.distinct()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(settings.t("Variations", "Phân loại"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.width(12.dp))
            // Hiển thị màu + size đang chọn
            selectedVariant?.let { v ->
                Text(v.color, color = AppTheme.colors.textSecondary, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(v.size, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp,
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

    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
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
                    catch (_: Exception) { MaterialTheme.colorScheme.surfaceVariant }
                )
                .then(
                    if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(10.dp))
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
                            .background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(variant.size, fontSize = 11.sp, color = if (isSelected) MaterialTheme.colorScheme.secondary else AppTheme.colors.textSecondary)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SPECIFICATIONS SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpecificationsSection(specs: Map<String, String>) {
    val settings = LocalAppSettings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(settings.t("Specifications", "Thông số kỹ thuật"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))

        specs.forEach { (key, value) ->
            Text(key, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(6.dp))
            // Hiển thị value dưới dạng chip(s) — tách bằng dấu phẩy nếu nhiều
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(value.split(",").map { it.trim() }.filter { it.isNotEmpty() }) { v ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            v,
                            color    = MaterialTheme.colorScheme.secondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

// ─────────────────────────────────────────────────────────────────────────────
//  DELIVERY SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeliverySection(freeShipping: Boolean) {
    val settings = LocalAppSettings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(settings.t("Delivery", "Vận chuyển"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))

        // Standard
        DeliveryRow(
            label    = settings.t("Standard", "Tiêu chuẩn"),
            duration = settings.t("5-7 days", "5-7 ngày"),
            price    = if (freeShipping) settings.t("Free", "Miễn phí") else "₫30.000",
            isFree   = freeShipping,
            color    = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(10.dp))
        // Express
        DeliveryRow(
            label    = settings.t("Express", "Hỏa tốc"),
            duration = settings.t("1-2 days", "1-2 ngày"),
            price    = "₫80.000",
            isFree   = false,
            color    = Color(0xFFFF6B00)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
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
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
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
            color      = if (isFree) AppTheme.colors.success else MaterialTheme.colorScheme.onBackground
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  RATING & REVIEWS SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProductReviewSection(
    product: Product,
    reviews: List<ProductReview>,
    navController: NavController
) {
    val settings = LocalAppSettings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(settings.t("Product Reviews", "Đánh giá sản phẩm"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            StarRow(rating = product.rating)
            Spacer(Modifier.width(8.dp))
            Text("${product.rating}/5", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
        }

        Spacer(Modifier.height(14.dp))

        val previewReviews = reviews.take(2)
        if (previewReviews.isEmpty()) {
            Text(settings.t("No reviews yet", "Chưa có đánh giá"), color = AppTheme.colors.textSecondary, fontSize = 13.sp)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                previewReviews.forEach { review ->
                    RealReviewItem(review = review)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        OutlinedButton(
            onClick = { navController.navigate(Screen.ProductReviews.createRoute(product.id)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
        ) {
            Text(settings.t("See all ${product.reviewCount} reviews", "Xem tất cả ${product.reviewCount} đánh giá"), fontWeight = FontWeight.Medium)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun RealReviewItem(review: ProductReview) {
    val settings = LocalAppSettings.current
    val name = review.userName.ifBlank { settings.t("Customer", "Khách hàng") }
    Row(verticalAlignment = Alignment.Top) {
        if (review.userAvatarUrl.isNotBlank()) {
            AsyncImage(
                model = review.userAvatarUrl,
                contentDescription = name,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(name.firstOrNull()?.toString() ?: "C", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            StarRow(rating = review.rating.toFloat().coerceIn(0f, 5f))
            val variantText = listOf(review.color, review.size)
                .filter { it.isNotBlank() }
                .joinToString(" | ")
            val metaText = listOf(
                formatReviewDate(review.updatedAtMillis.takeIf { it > 0L } ?: review.createdAtMillis),
                variantText
            )
                .filter { it.isNotBlank() }
                .joinToString(" • ")
            if (metaText.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(metaText, fontSize = 12.sp, color = AppTheme.colors.textSecondary)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = review.comment.ifBlank { settings.t("No comment", "Không có bình luận") },
                fontSize = 13.sp,
                color = AppTheme.colors.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp
            )
        }
    }
}

private fun formatReviewDate(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return ""
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestampMillis))
}

@Composable
private fun StarRow(rating: Float) {
    Row {
        val full = rating.toInt()
        val hasHalf = rating - full >= 0.5f && full < 5
        repeat(5) { i ->
            Box(modifier = Modifier.size(18.dp)) {
                Icon(
                    imageVector = Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = AppTheme.colors.star,
                    modifier = Modifier.size(18.dp)
                )
                if (i < full) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = AppTheme.colors.star,
                        modifier = Modifier.size(18.dp)
                    )
                } else if (i == full && hasHalf) {
                    Box(
                        modifier = Modifier
                            .width(9.dp)
                            .height(18.dp)
                            .clipToBounds()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = AppTheme.colors.star,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  MOST POPULAR SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MostPopularSection(products: List<Product>, navController: NavController) {
    val settings = LocalAppSettings.current
    Column {
        // Header
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(settings.t("Most Popular", "Phổ biến nhất"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(settings.t("See All", "Xem tất cả"), color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier         = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary),
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
                .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = product.imageUrl.ifBlank { null },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_shopping)
            )
            // Badge
            Row(
                modifier = Modifier.align(Alignment.BottomStart).padding(5.dp)
                    .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val settings = LocalAppSettings.current
                Text(settings.t("${product.soldCount} sold", "Đã bán ${product.soldCount}"), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(product.name, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = AppTheme.colors.textSecondary)
        Text("₫${formatPrice(product.price)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun GridProductCard(product: Product, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.85f)
                .clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = product.imageUrl.ifBlank { null },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_shopping)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(product.name, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = AppTheme.colors.textSecondary)
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
    isLoading: Boolean,
    onAddToCart: () -> Unit,
    onBuyNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = LocalAppSettings.current
    Surface(
        modifier      = modifier.fillMaxWidth(),
        color         = MaterialTheme.colorScheme.surface,
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
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                border   = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary),
                enabled  = !isLoading
            ) {
                Text(if (isLoading) settings.t("Adding...", "Đang thêm...") else settings.t("Add to cart", "Thêm vào giỏ"), fontWeight = FontWeight.SemiBold)
            }

            // Buy now
            Button(
                onClick  = onBuyNow,
                modifier = Modifier.weight(1f).height(50.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                enabled  = !isLoading
            ) {
                Text(if (isLoading) settings.t("Please wait", "Vui lòng đợi...") else settings.t("Buy now", "Mua ngay"), fontWeight = FontWeight.SemiBold)
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

// ─────────────────────────────────────────────────────────────────────────────
//  SHOP SECTION  (giống Shopee: avatar + tên shop + stats + nút Visit)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ShopSection(
    shopId: String,
    onVisitShop: () -> Unit,
) {
    var shopName      by remember { mutableStateOf("") }
    var shopAvatar    by remember { mutableStateOf("") }
    var followerCount by remember { mutableStateOf(0) }
    var rating        by remember { mutableStateOf(0f) }
    var isLoading     by remember { mutableStateOf(true) }

    LaunchedEffect(shopId) {
        if (shopId.isBlank()) { isLoading = false; return@LaunchedEffect }
        try {
            val db  = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val doc = db.collection("shops").document(shopId).get().await()

            shopName      = doc.getString("name").orEmpty().ifBlank { "Shop" }
            shopAvatar    = StorageUrlResolver.resolve(doc.getString("logoRef").orEmpty()) // ← logoRef
            followerCount = (doc.getLong("followerCount") ?: 0L).toInt()
            rating        = (doc.get("rating") as? Number)?.toFloat() ?: 0f
        } catch (_: Exception) {
            shopName = "Shop"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        val settings = LocalAppSettings.current
        Text(settings.t("Shop", "Cửa hàng"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = !isLoading) { onVisitShop() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar / Logo
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!isLoading) {
                    AsyncImage(
                        model              = shopAvatar.ifBlank { null },
                        contentDescription = shopName,
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop,
                        error              = painterResource(R.drawable.ic_profile)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Tên + stats
            if (isLoading) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .width(120.dp).height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Box(
                        modifier = Modifier
                            .width(80.dp).height(11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    // Tên
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text       = shopName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 15.sp,
                            color      = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(Modifier.height(3.dp))

                    // Followers + Rating
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text     = settings.t("${formatFollowerCount(followerCount)} followers", "${formatFollowerCount(followerCount)} người theo dõi"),
                            fontSize = 12.sp,
                            color    = AppTheme.colors.textSecondary
                        )
                        if (rating > 0f) {
                            Text("•", fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = AppTheme.colors.star,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text     = "$rating",
                                    fontSize = 12.sp,
                                    color    = AppTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint               = AppTheme.colors.textSecondary,
                    modifier           = Modifier.size(16.dp)
                )
            }
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

// ── Helper format follower count ──────────────────────────────────────────────

private fun formatFollowerCount(count: Int): String = when {
    count >= 1_000_000 -> "${"%.1f".format(count / 1_000_000.0)}M"
    count >= 1_000     -> "${"%.1f".format(count / 1_000.0)}K"
    else               -> count.toString()
}
