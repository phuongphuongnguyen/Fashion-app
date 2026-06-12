package com.example.fashionapp.ui.app.post

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fashionapp.data.feed.FeedRepository
import com.example.fashionapp.data.product.ProductRepository
import com.example.fashionapp.data.user.UserSession
import com.example.fashionapp.model.ProductVariant
import com.example.fashionapp.ui.components.FashionTopBar
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.theme.AppTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CreateMode { POST, PRODUCT }

data class CreatePostUiState(
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val mode: CreateMode = CreateMode.POST
)

class CreatePostViewModel : ViewModel() {
    private val feedRepository = FeedRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    // Đặt chế độ tạo bài viết (POST) hoặc sản phẩm mới (PRODUCT) trong biểu mẫu tạo bài đăng
    fun setMode(mode: CreateMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
    }

    // Đăng bài viết mới kèm ảnh và chú thích lên mạng xã hội của ứng dụng, có thể liên kết tài khoản shop
    fun submitPost(authorIdOverride: String?, caption: String, imageUris: List<Uri>) {
        val currentUser = UserSession.currentUser.value
        val authorId = authorIdOverride?.takeIf { it.isNotBlank() } ?: auth.currentUser?.uid.orEmpty()
        if (authorId.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please sign in before posting")
            return
        }
        if (caption.isBlank() && imageUris.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Add a caption or at least one image")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            try {
                feedRepository.createPost(
                    authorId = authorId,
                    caption = caption,
                    imageUris = imageUris,
                    fallbackAuthorName = currentUser?.username.orEmpty().ifBlank {
                        currentUser?.name.orEmpty()
                    },
                    fallbackAuthorAvt = currentUser?.avatarRef.orEmpty().ifBlank {
                        currentUser?.avatarUrl.orEmpty()
                    }
                )
                _uiState.value = _uiState.value.copy(isSubmitting = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message ?: "Failed to create post")
            }
        }
    }

    // Tạo sản phẩm mới kèm các biến thể (kích cỡ/màu sắc) và hình ảnh rồi cập nhật lên cửa hàng
    fun submitProduct(
        shopId: String,
        name: String,
        description: String,
        priceStr: String,
        stockStr: String,
        categoryId: String,
        imageUris: List<Uri>,
        variants: List<ProductVariant>
    ) {
        if (shopId.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Shop session required")
            return
        }
        val price = priceStr.toDoubleOrNull() ?: 0.0
        val stock = stockStr.toIntOrNull() ?: 0
        if (name.isBlank() || imageUris.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Name and at least one image are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            try {
                ProductRepository.createProduct(
                    shopId = shopId,
                    name = name,
                    description = description,
                    price = price,
                    stock = stock,
                    categoryId = categoryId,
                    imageUris = imageUris,
                    variants = variants
                )
                _uiState.value = _uiState.value.copy(isSubmitting = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message ?: "Failed to add product")
            }
        }
    }

    // Reset lại trạng thái kết quả đăng tin (thành công/lỗi) để chuẩn bị cho lần thực thi tiếp theo
    fun consumeResult() {
        _uiState.value = _uiState.value.copy(isSuccess = false, error = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    navController: NavController,
    authorId: String? = null, // if present, it's a shop
    viewModel: CreatePostViewModel = viewModel()
) {
    val settings = LocalAppSettings.current
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by UserSession.currentUser.collectAsState()
    val context = LocalContext.current

    // Shared state
    var caption by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Product specific state
    var productName by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var selectedSizes by remember { mutableStateOf(setOf<String>()) }
    val availableSizes = listOf("XS", "S", "M", "L", "XL", "XXL")

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        imageUris = (imageUris + uris).distinct().take(6)
    }

    LaunchedEffect(uiState.isSuccess, uiState.error) {
        if (uiState.isSuccess) {
            Toast.makeText(context, settings.t("Action completed", "Thao tác thành công"), Toast.LENGTH_SHORT).show()
            viewModel.consumeResult()
            navController.popBackStack()
        } else if (uiState.error != null) {
            Toast.makeText(context, uiState.error, Toast.LENGTH_SHORT).show()
            viewModel.consumeResult()
        }
    }

    Scaffold(
        topBar = {
            FashionTopBar(
                title = if (uiState.mode == CreateMode.POST) settings.t("Create Post", "Tạo bài viết") else settings.t("Add Product", "Thêm sản phẩm"),
                onBackClick = { navController.popBackStack() },
                actions = {
                    TextButton(
                        enabled = !uiState.isSubmitting,
                        onClick = {
                            if (uiState.mode == CreateMode.POST) {
                                viewModel.submitPost(authorId, caption, imageUris)
                            } else {
                                val variants = selectedSizes.map { size ->
                                    ProductVariant(size = size, stock = stock.toIntOrNull() ?: 0)
                                }
                                viewModel.submitProduct(
                                    shopId = authorId ?: "",
                                    name = productName,
                                    description = caption,
                                    priceStr = price,
                                    stockStr = stock,
                                    categoryId = "general",
                                    imageUris = imageUris,
                                    variants = variants
                                )
                            }
                        }
                    ) {
                        Text(settings.t("Submit", "Gửi"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PostComposerCard(
                    authorName = currentUser?.username.orEmpty()
                        .ifBlank { currentUser?.name.orEmpty() }
                        .ifBlank { if (authorId.isNullOrBlank()) settings.t("User", "Người dùng") else settings.t("Shop", "Cửa hàng") },
                    authorAvatar = currentUser?.avatarUrl.orEmpty(),
                    isShopPost = !authorId.isNullOrBlank(),
                    caption = caption,
                    onCaptionChange = { caption = it },
                    imageUris = imageUris,
                    onPickImages = { imagePicker.launch("image/*") },
                    onRemoveImage = { uri -> imageUris = imageUris.filterNot { it == uri } }
                )
            }
            // Mode Toggle (Only if authorId is present - i.e. it's a Shop)
            if (!authorId.isNullOrBlank()) {
                item {
                    TabRow(
                        selectedTabIndex = if (uiState.mode == CreateMode.POST) 0 else 1,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = uiState.mode == CreateMode.POST,
                            onClick = { viewModel.setMode(CreateMode.POST) },
                            text = { Text(settings.t("Post", "Bài viết")) }
                        )
                        Tab(
                            selected = uiState.mode == CreateMode.PRODUCT,
                            onClick = { viewModel.setMode(CreateMode.PRODUCT) },
                            text = { Text(settings.t("Product", "Sản phẩm")) }
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.mode == CreateMode.PRODUCT) {
                        OutlinedTextField(
                            value = productName,
                            onValueChange = { productName = it },
                            label = { Text(settings.t("Product Name", "Tên sản phẩm")) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = price,
                                onValueChange = { if (it.all { c -> c.isDigit() }) price = it },
                                label = { Text(settings.t("Price", "Giá")) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = stock,
                                onValueChange = { if (it.all { c -> c.isDigit() }) stock = it },
                                label = { Text(settings.t("Stock", "Kho")) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Text(settings.t("Select Sizes", "Chọn kích thước"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableSizes.forEach { size ->
                                FilterChip(
                                    selected = selectedSizes.contains(size),
                                    onClick = {
                                        selectedSizes = if (selectedSizes.contains(size)) selectedSizes - size else selectedSizes + size
                                    },
                                    label = { Text(size) }
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }

                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 8,
                        placeholder = { Text(if (uiState.mode == CreateMode.POST) settings.t("Write a caption...", "Viết chú thích...") else settings.t("Description...", "Mô tả sản phẩm...")) },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    MediaPreview(imageUris = imageUris, onPickImages = { imagePicker.launch("image/*") })

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        item { AddImageTile { imagePicker.launch("image/*") } }
                        items(imageUris) { uri ->
                            SelectedImageTile(uri = uri) { imageUris = imageUris.filterNot { it == uri } }
                        }
                    }
                }
            }

            if (uiState.isSubmitting) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun PostComposerCard(
    authorName: String,
    authorAvatar: String,
    isShopPost: Boolean,
    caption: String,
    onCaptionChange: (String) -> Unit,
    imageUris: List<Uri>,
    onPickImages: () -> Unit,
    onRemoveImage: (Uri) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = authorAvatar.ifBlank { null },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(com.example.fashionapp.R.drawable.ic_profile),
                fallback = painterResource(com.example.fashionapp.R.drawable.ic_profile)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = authorName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                text = if (isShopPost) "Posting as Shop" else "Public Post",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MediaPreview(imageUris: List<Uri>, onPickImages: () -> Unit) {
    val settings = LocalAppSettings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = imageUris.isEmpty()) { onPickImages() },
        contentAlignment = Alignment.Center
    ) {
        if (imageUris.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                Text(settings.t("Add Images", "Thêm hình ảnh"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
        } else {
            AsyncImage(model = imageUris.first(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (imageUris.size > 1) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+${imageUris.size - 1}", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AddImageTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SelectedImageTile(uri: Uri, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp))) {
        AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)) {
            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}
