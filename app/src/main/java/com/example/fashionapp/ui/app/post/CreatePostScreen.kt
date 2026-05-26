package com.example.fashionapp.ui.app.post

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fashionapp.R
import com.example.fashionapp.data.feed.FeedRepository
import com.example.fashionapp.data.user.UserSession
import com.example.fashionapp.ui.components.FashionTopBar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreatePostUiState(
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class CreatePostViewModel : ViewModel() {
    private val repository = FeedRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun submitPost(authorIdOverride: String?, caption: String, imageUris: List<Uri>) {
        val currentUser = UserSession.currentUser.value
        val authorId = authorIdOverride?.takeIf { it.isNotBlank() } ?: auth.currentUser?.uid.orEmpty()
        if (authorId.isBlank()) {
            _uiState.value = CreatePostUiState(error = "Please sign in before posting")
            return
        }
        if (caption.isBlank() && imageUris.isEmpty()) {
            _uiState.value = CreatePostUiState(error = "Add a caption or at least one image")
            return
        }

        viewModelScope.launch {
            _uiState.value = CreatePostUiState(isSubmitting = true)
            try {
                repository.createPost(
                    authorId = authorId,
                    caption = caption,
                    imageUris = imageUris,
                    fallbackAuthorName = currentUser?.name.orEmpty(),
                    fallbackAuthorAvt = currentUser?.avatarUrl.orEmpty()
                )
                _uiState.value = CreatePostUiState(isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = CreatePostUiState(error = e.message ?: "Failed to create post")
            }
        }
    }

    fun consumeResult() {
        _uiState.value = CreatePostUiState()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    navController: NavController,
    authorId: String? = null,
    viewModel: CreatePostViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val currentUser by UserSession.currentUser.collectAsState()
    var caption by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        imageUris = (imageUris + uris).distinct().take(6)
    }

    LaunchedEffect(uiState.isSuccess, uiState.error) {
        when {
            uiState.isSuccess -> {
                Toast.makeText(context, "Post created", Toast.LENGTH_SHORT).show()
                viewModel.consumeResult()
                navController.popBackStack()
            }
            uiState.error != null -> {
                Toast.makeText(context, uiState.error, Toast.LENGTH_SHORT).show()
                viewModel.consumeResult()
            }
        }
    }

    Scaffold(
        topBar = {
            FashionTopBar(
                title = "Create Post",
                onBackClick = { navController.popBackStack() },
                actions = {
                    TextButton(
                        enabled = !uiState.isSubmitting && (caption.isNotBlank() || imageUris.isNotEmpty()),
                        onClick = { viewModel.submitPost(authorId, caption, imageUris) }
                    ) {
                        Text("Post", color = Color(0xFF0056FF), fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        containerColor = Color.White
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PostComposerCard(
                    authorName = currentUser?.name.orEmpty().ifBlank { if (authorId.isNullOrBlank()) "User" else "Shop" },
                    authorAvatar = currentUser?.avatarUrl.orEmpty(),
                    isShopPost = !authorId.isNullOrBlank(),
                    caption = caption,
                    onCaptionChange = { caption = it },
                    imageUris = imageUris,
                    onPickImages = { imagePicker.launch("image/*") },
                    onRemoveImage = { uri -> imageUris = imageUris.filterNot { it == uri } }
                )
            }

            item {
                if (uiState.isSubmitting) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE8EAF0), RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F3F6))
            ) {
                AsyncImage(
                    model = authorAvatar.ifBlank { null },
                    contentDescription = authorName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = androidx.compose.ui.res.painterResource(R.drawable.ic_profile),
                    fallback = androidx.compose.ui.res.painterResource(R.drawable.ic_profile)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(authorName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(if (isShopPost) "Shop post" else "Profile post", color = Color.Gray, fontSize = 12.sp)
            }
        }

        OutlinedTextField(
            value = caption,
            onValueChange = onCaptionChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 8,
            placeholder = { Text("Write a caption...") },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color(0xFFE0E4EC),
                unfocusedIndicatorColor = Color(0xFFE8EAF0),
                focusedContainerColor = Color(0xFFFAFBFC),
                unfocusedContainerColor = Color(0xFFFAFBFC)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        MediaPreview(imageUris = imageUris, onPickImages = onPickImages)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Content images", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("${imageUris.size}/6", color = Color.Gray, fontSize = 12.sp)
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            item { AddImageTile(onClick = onPickImages) }
            items(imageUris, key = { it.toString() }) { uri ->
                SelectedImageTile(uri = uri, onRemove = { onRemoveImage(uri) })
            }
        }
    }
}

@Composable
private fun MediaPreview(imageUris: List<Uri>, onPickImages: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF1F3F6))
            .clickable(enabled = imageUris.isEmpty()) { onPickImages() },
        contentAlignment = Alignment.Center
    ) {
        if (imageUris.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add images", tint = Color(0xFF0056FF), modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(6.dp))
                Text("Add post images", color = Color(0xFF0056FF), fontWeight = FontWeight.Medium)
            }
        } else {
            AsyncImage(
                model = imageUris.first(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (imageUris.size > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    color = Color.Black.copy(alpha = 0.62f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "+${imageUris.size - 1}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddImageTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F3F6))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add image", tint = Color(0xFF0056FF))
            Spacer(Modifier.height(4.dp))
            Text("Add", color = Color(0xFF0056FF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SelectedImageTile(uri: Uri, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F3F6))
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Remove image", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}
