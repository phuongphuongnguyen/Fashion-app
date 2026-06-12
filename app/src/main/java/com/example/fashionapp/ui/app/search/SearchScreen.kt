package com.example.fashionapp.ui.app.search

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fashionapp.R
import com.example.fashionapp.data.search.SubCategory
import com.example.fashionapp.model.Product
import com.example.fashionapp.model.User
import com.example.fashionapp.navigation.Screen

// ── Palette ───────────────────────────────────────────────────────────────────
private val PrimaryBlue  = Color(0xFF3669C9)
private val SearchBg     = Color(0xFFEBEEF8)
private val TextDark     = Color(0xFF1A1A1A)
private val TextGray     = Color(0xFF888888)
private val ChipBg       = Color(0xFFF3F5FA)
private val StarYellow   = Color(0xFFFFC107)

// ─────────────────────────────────────────────────────────────────────────────
//  SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SearchScreen(
    navController: NavController,
    initialQuery: String = "",
    initialCategoryId: String? = null,
    initialSort: String? = null,
    viewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(initialQuery, initialCategoryId, initialSort, LocalContext.current)
    )
) {
    val state        by viewModel.uiState.collectAsState()
    val focusManager =  LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val context      = LocalContext.current

    // Auto-focus search bar khi mở màn hình
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // ── Top Search Bar ───────────────────────────────────────────────
        SearchTopBar(
            query          = state.query,
            onQueryChange  = viewModel::onQueryChange,
            onClear        = viewModel::clearQuery,
            onBack         = { navController.popBackStack() },
            focusRequester = focusRequester,
            onSearch       = {
                viewModel.onSearchSubmit()
                focusManager.clearFocus()
            },
            onCameraClick  = {
                Toast.makeText(context, "Image search is coming soon", Toast.LENGTH_SHORT).show()
            }
        )

        // Thanh tiến trình khi đang search
        if (state.isSearching && !state.isLoadingInitial) {
            LinearProgressIndicator(
                modifier  = Modifier.fillMaxWidth(),
                color     = PrimaryBlue,
                trackColor = SearchBg
            )
        }

        // ── Nội dung ────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize()) {

            // SubCategories (circles)
            if (state.subCategories.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SubCategoriesRow(
                    subCategories      = state.subCategories,
                    selectedCategoryId = state.selectedCategoryId,
                    onSelect           = { id ->
                        viewModel.onCategorySelected(id)
                        focusManager.clearFocus()
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            when {
                // Lần đầu fetch Firestore
                state.isLoadingInitial -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
                // Gợi ý khi chưa nhập gì và chưa chọn category
                state.showSuggestions -> {
                    SuggestionsContent(
                        history       = state.history,
                        popular       = state.popular,
                        onPick        = { keyword ->
                            viewModel.onPickSuggestion(keyword)
                            focusManager.clearFocus()
                        },
                        onRemoveItem  = viewModel::removeHistoryItem,
                        onClearAll    = viewModel::clearHistory,
                    )
                }
                // Có query hoặc category → user (trên) + sản phẩm (dưới)
                else -> {
                    if (state.users.isEmpty() && state.products.isEmpty()) {
                        ResultsHeader(
                            query              = state.submittedQuery,
                            selectedCategoryId = state.selectedCategoryId,
                            sortMode           = state.sortMode,
                            subCategories      = state.subCategories,
                            resultCount        = 0,
                        )
                        EmptyState(query = state.submittedQuery)
                    } else {
                        SearchResults(
                            users              = state.users,
                            products           = state.products,
                            query              = state.submittedQuery,
                            selectedCategoryId = state.selectedCategoryId,
                            sortMode           = state.sortMode,
                            subCategories      = state.subCategories,
                            onUserClick        = { user ->
                                if (user.id.isNotBlank()) {
                                    navController.navigate(Screen.ShopDetail.createRoute(user.id))
                                }
                            },
                            onProductClick     = { product ->
                                navController.navigate("product_detail/${product.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SEARCH TOP BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester,
    onSearch: () -> Unit,
    onCameraClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Back button
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint     = TextDark,
                modifier = Modifier.size(22.dp)
            )
        }

        // Search field — dùng String đơn giản cho ô nhập liệu để tránh lỗi IME tiếng Việt với TextFieldValue
        var textState by remember { mutableStateOf(query) }

        // Thanh nhập liệu chính
        OutlinedTextField(
            value = textState,
            onValueChange = { 
                textState = it
                onQueryChange(it)
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = { Text("Search", fontSize = 14.sp) },
            trailingIcon = {
                if (textState.isNotEmpty()) {
                    IconButton(onClick = {
                        textState = ""
                        onClear()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SearchBg,
                unfocusedContainerColor = SearchBg,
                disabledContainerColor = SearchBg,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = TextDark,
                unfocusedTextColor = TextDark
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
        )

        // Camera icon
        IconButton(onClick = onCameraClick, modifier = Modifier.size(40.dp)) {
            Icon(
                painter            = painterResource(R.drawable.ic_camera),
                contentDescription = "Camera search",
                tint               = PrimaryBlue,
                modifier           = Modifier.size(24.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  RESULTS HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResultsHeader(
    query: String,
    selectedCategoryId: String?,
    sortMode: String?,
    subCategories: List<SubCategory>,
    resultCount: Int,
) {
    val title = when {
        query.isNotBlank() -> "Results for \"$query\""
        sortMode == "popular" -> "Most Popular"
        selectedCategoryId != null -> {
            val name = subCategories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Category"
            "Category: $name"
        }
        else -> "All products"
    }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text       = title,
            fontWeight = FontWeight.Bold,
            fontSize   = 18.sp,
            color      = TextDark
        )
        Text(
            text     = "$resultCount products",
            fontSize = 12.sp,
            color    = TextGray,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SUGGESTIONS (history + popular)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SuggestionsContent(
    history: List<String>,
    popular: List<String>,
    onPick: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Recent searches",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = TextDark
                )
                Text(
                    "Clear all",
                    fontSize = 13.sp,
                    color    = PrimaryBlue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClearAll() }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            history.forEach { item ->
                HistoryRow(
                    text     = item,
                    onClick  = { onPick(item) },
                    onRemove = { onRemoveItem(item) }
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        if (popular.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint     = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "Popular searches",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = TextDark
                )
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(popular, key = { it }) { keyword ->
                    KeywordChip(text = keyword, onClick = { onPick(keyword) })
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    text: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            tint     = TextGray,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text       = text,
            fontSize   = 14.sp,
            color      = TextDark,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Xoá",
                tint     = TextGray,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun KeywordChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ChipBg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, color = TextDark)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SUBCATEGORIES ROW (circles)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SubCategoriesRow(
    subCategories: List<SubCategory>,
    selectedCategoryId: String?,
    onSelect: (String?) -> Unit,
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(subCategories, key = { it.id }) { sub ->
            SubCategoryItem(
                sub        = sub,
                isSelected = sub.id == selectedCategoryId,
                onClick    = { onSelect(sub.id) }
            )
        }
    }
}

@Composable
private fun SubCategoryItem(
    sub: SubCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier            = Modifier
            .width(68.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F2F5))
                .then(
                    if (isSelected) Modifier.border(2.5.dp, PrimaryBlue, CircleShape)
                    else Modifier
                )
        ) {
            if (sub.previewImageUrl.isNotBlank()) {
                AsyncImage(
                    model              = sub.previewImageUrl,
                    contentDescription = sub.name,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text     = categoryEmoji(sub.name),
                        fontSize = 28.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(3.dp, Color.White, CircleShape)
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text       = sub.name,
            fontSize   = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color      = if (isSelected) PrimaryBlue else TextDark,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SEARCH RESULTS (user trên · sản phẩm dưới)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchResults(
    users: List<User>,
    products: List<Product>,
    query: String,
    selectedCategoryId: String?,
    sortMode: String?,
    subCategories: List<SubCategory>,
    onUserClick: (User) -> Unit,
    onProductClick: (Product) -> Unit,
) {
    LazyVerticalGrid(
        columns               = GridCells.Fixed(2),
        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement   = Arrangement.spacedBy(16.dp),
        modifier              = Modifier.fillMaxSize()
    ) {
        // ── Người dùng ───────────────────────────────────────────────────
        if (users.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("Người dùng", "${users.size} người dùng")
            }
            items(
                users,
                span = { GridItemSpan(maxLineSpan) },
                key  = { "user_${it.id}" }
            ) { user ->
                UserResultRow(user = user, onClick = { onUserClick(user) })
            }
        }

        // ── Sản phẩm ─────────────────────────────────────────────────────
        if (products.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val title = when {
                    query.isNotBlank()         -> "Products"
                    sortMode == "new"          -> "New Items"
                    sortMode == "popular"      -> "Most Popular"
                    selectedCategoryId != null ->
                        subCategories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Products"
                    else                       -> "Products"
                }
                SectionTitle(title, "${products.size} products")
            }
            itemsIndexed(products, key = { _, p -> p.id }) { index, product ->
                ProductGridCard(
                    product  = product,
                    tallCard = index % 2 == 1,
                    onClick  = { onProductClick(product) }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text       = title,
            fontWeight = FontWeight.Bold,
            fontSize   = 18.sp,
            color      = TextDark
        )
        Text(
            text     = subtitle,
            fontSize = 12.sp,
            color    = TextGray,
        )
    }
}

@Composable
private fun UserResultRow(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFEEEEEE))
        ) {
            AsyncImage(
                model              = user.avatarUrl.ifBlank { null },
                contentDescription = user.name,
                modifier           = Modifier.fillMaxSize(),
                contentScale       = ContentScale.Crop,
                error              = painterResource(R.drawable.ic_profile),
                fallback           = painterResource(R.drawable.ic_profile)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = user.name,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = TextDark,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                text     = "${user.followersCount} followers",
                fontSize = 12.sp,
                color    = TextGray,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PRODUCT CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProductGridCard(
    product: Product,
    tallCard: Boolean,
    onClick: () -> Unit,
) {
    val imageHeight = if (tallCard) 220.dp else 180.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEEEEEE))
        ) {
            AsyncImage(
                model              = product.imageUrl.ifBlank { null },
                contentDescription = product.name,
                modifier           = Modifier.fillMaxSize(),
                contentScale       = ContentScale.Crop,
                error              = painterResource(R.drawable.ic_shopping)
            )

            if (product.discountPercent > 0) {
                Surface(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart),
                    shape    = RoundedCornerShape(6.dp),
                    color    = Color(0xFFFF3B30)
                ) {
                    Text(
                        "-${product.discountPercent}%",
                        color    = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text      = product.name,
            fontSize  = 13.sp,
            color     = TextDark,
            maxLines  = 2,
            overflow  = TextOverflow.Ellipsis,
            lineHeight= 18.sp
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text       = "VND ${formatPrice(product.price)}",
            fontWeight = FontWeight.Bold,
            fontSize   = 15.sp,
            color      = TextDark
        )

        if (product.rating > 0) {
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter            = painterResource(R.drawable.ic_like_full),
                    contentDescription = null,
                    tint               = StarYellow,
                    modifier           = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text("${product.rating}", fontSize = 11.sp, color = TextGray)
                Text(" • ${product.soldCount} sold", fontSize = 11.sp, color = TextGray)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  EMPTY STATE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(query: String) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        if (query.isBlank()) {
            Text("No products found", color = TextGray, fontSize = 15.sp)
        } else {
            Text(
                "No results found for",
                color    = TextGray,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "\"$query\"",
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
                color      = TextDark
            )
            Spacer(Modifier.height(8.dp))
            Text("Try another keyword", color = TextGray, fontSize = 13.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HELPERS
// ─────────────────────────────────────────────────────────────────────────────

private fun formatPrice(price: Double): String =
    "%.0f".format(price).reversed().chunked(3).joinToString(".").reversed()

private fun categoryEmoji(name: String): String = when {
    name.contains("váy", true) || name.contains("dress", true)  -> "👗"
    name.contains("quần", true) || name.contains("pant", true)  -> "👖"
    name.contains("giày", true) || name.contains("shoe", true)  -> "👟"
    name.contains("túi", true)  || name.contains("bag", true)   -> "👜"
    name.contains("đồng hồ", true) || name.contains("watch", true) -> "⌚"
    name.contains("áo khoác", true) || name.contains("jacket", true) -> "🧥"
    name.contains("hoodie", true)                               -> "🧢"
    name.contains("sơ mi", true) || name.contains("shirt", true) -> "👔"
    name.contains("thể thao", true) || name.contains("sport", true) -> "🏃"
    else                                                         -> "🛍️"
}
