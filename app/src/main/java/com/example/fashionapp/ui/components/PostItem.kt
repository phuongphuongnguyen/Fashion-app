package com.example.fashionapp.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fashionapp.R
import com.example.fashionapp.model.Post
import com.example.fashionapp.model.ProductTag
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FeedPostItem(
    post: Post,
    isLiked: Boolean,
    isSaved: Boolean,
    isVerified: Boolean = false,
    isLikePending: Boolean = false,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCommentClick: () -> Unit,
    onHeaderClick: () -> Unit = {},
    onProductClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    Column {
        PostHeader(
            postId = post.id,
            avatarUrl = post.authorAvt,
            username = post.authorName,
            isVerified = isVerified,
            onHeaderClick = onHeaderClick
        )

        if (post.imageUrls.isNotEmpty()) {
            PostImagesRow(imageUrls = post.imageUrls)
        }

        if (post.taggedProducts.isNotEmpty()) {
            ProductTagsRow(tags = post.taggedProducts, onProductClick = onProductClick)
        }

        ActionRow(
            isLiked = isLiked,
            isSaved = isSaved,
            isLikePending = isLikePending,
            onLikeClick = onLikeClick,
            onSaveClick = onSaveClick,
            onCommentClick = onCommentClick,
            onShareClick = {
                shareText(
                    context = context,
                    text = "${post.authorName}: ${post.caption}".trim()
                )
            },
            imageCount = post.imageUrls.size
        )

        Text(
            text = "${formatLikeCount(post.likeCount)} Likes",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(4.dp))

        CaptionRow(
            username = post.authorName,
            caption = post.caption,
            timestamp = post.createdAt?.toDate()
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun PostHeader(
    postId: String,
    avatarUrl: String,
    username: String,
    isVerified: Boolean = false,
    onHeaderClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onHeaderClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFEEEEEE)).padding(2.dp)) {
            AsyncImage(
                model = avatarUrl.ifBlank { null },
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_profile),
                fallback = painterResource(R.drawable.ic_profile)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(text = username, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        if (isVerified) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF0057FF), modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.weight(1f))
        Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = null)
        }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Report") },
                    onClick = {
                        showMenu = false
                        reportPost(
                            postId = postId,
                            onSuccess = {
                                Toast.makeText(context, "Report submitted", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = {
                                Toast.makeText(context, "Could not submit report", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Copy link") },
                    onClick = {
                        showMenu = false
                        copyPostLink(clipboardManager, postId)
                        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

private fun reportPost(
    postId: String,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    if (postId.isBlank()) {
        onFailure()
        return
    }

    val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val data = hashMapOf(
        "postId" to postId,
        "reporterId" to userId,
        "type" to "post",
        "status" to "open",
        "createdAt" to FieldValue.serverTimestamp()
    )
    FirebaseFirestore.getInstance()
        .collection("reports")
        .add(data)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onFailure() }
}

private fun copyPostLink(clipboardManager: ClipboardManager, postId: String) {
    val link = "fashion-app://post/$postId"
    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(link))
}

@Composable
fun PostImagesRow(imageUrls: List<String>) {
    if (imageUrls.size == 1) {
        AsyncImage(
            model = imageUrls.first().ifBlank { null },
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFFF9F9F9)),
            contentScale = ContentScale.Fit,
            error = painterResource(R.drawable.ic_shopping)
        )
    } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(imageUrls) { url ->
                AsyncImage(
                    model = url.ifBlank { null },
                    contentDescription = null,
                    modifier = Modifier.fillParentMaxWidth().fillMaxHeight().background(Color(0xFFF9F9F9)),
                    contentScale = ContentScale.Fit,
                    error = painterResource(R.drawable.ic_shopping),
                    fallback = painterResource(R.drawable.ic_shopping)
                )
            }
        }
    }
}

@Composable
fun ProductTagsRow(tags: List<ProductTag>, onProductClick: (String) -> Unit = {}) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.take(4).forEach { tag ->
            AsyncImage(
                model = tag.thumbnailUrl.ifBlank { null },
                contentDescription = tag.label.ifBlank { null },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F0F0))
                    .clickable(enabled = tag.productId.isNotBlank()) {
                        onProductClick(tag.productId)
                    },
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_shopping)
            )
        }
    }
}

@Composable
fun ActionRow(
    isLiked: Boolean,
    isSaved: Boolean,
    isLikePending: Boolean,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    imageCount: Int
) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLikeClick, enabled = !isLikePending) {
                Icon(
                    painter = painterResource(if (isLiked) R.drawable.ic_like_full else R.drawable.ic_like),
                    contentDescription = null,
                    tint = if (isLiked) Color(0xFFF04957) else Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onCommentClick) {
                Icon(painter = painterResource(R.drawable.ic_comment), contentDescription = null, modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = onShareClick) {
                Icon(painter = painterResource(R.drawable.ic_share), contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
        if (imageCount > 1) {
            Row(modifier = Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(imageCount.coerceAtMost(5)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF3669C9)))
                }
            }
        }
        IconButton(modifier = Modifier.align(Alignment.CenterEnd), onClick = onSaveClick) {
            Icon(painter = painterResource(if (isSaved) R.drawable.ic_bookmark_full else R.drawable.ic_bookmark), contentDescription = null, modifier = Modifier.size(22.dp))
        }
    }
}

private fun shareText(context: android.content.Context, text: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text.ifBlank { "Fashion app post" })
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share"))
}

@Composable
fun CaptionRow(username: String, caption: String, timestamp: Date?) {
    val dateStr = timestamp?.let { SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).format(it) } ?: ""
    Text(
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) { append(username) }
            append(" ")
            append(caption)
        },
        fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
    )
    if (dateStr.isNotEmpty()) {
        Text(text = "$dateStr •", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
    }
}

fun formatLikeCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%,.0f", count.toDouble())
        else -> count.toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    post: Post,
    sheetState: SheetState,
    currentUserAvatarUrl: String = "",
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        contentWindowInsets = { WindowInsets.ime }
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.85f).fillMaxWidth().navigationBarsPadding()) {
            Text(text = "Comments", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { CommentItem(username = post.authorName, avatarUrl = post.authorAvt, text = post.caption, isCaption = true) }
                items(post.comments) { comment -> CommentItem(username = comment.username, avatarUrl = comment.avatarUrl, text = comment.text) }
            }
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))
            Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFEEEEEE))) {
                    AsyncImage(
                        model = currentUserAvatarUrl.ifBlank { null },
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_profile),
                        fallback = painterResource(R.drawable.ic_profile)
                    )
                }
                TextField(
                    value = commentText, onValueChange = { commentText = it },
                    placeholder = { Text("Add a comment...", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                    maxLines = 5
                )
                IconButton(onClick = { onSendComment(commentText); commentText = "" }, enabled = commentText.isNotBlank()) {
                    Icon(painter = painterResource(R.drawable.ic_share), contentDescription = null, modifier = Modifier.size(32.dp), tint = if (commentText.isNotBlank()) Color(0xFF3669C9) else Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun CommentItem(username: String, avatarUrl: String, text: String, isCaption: Boolean = false) {
    Row(verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFEEEEEE))) {
            AsyncImage(
                model = avatarUrl.ifBlank { null },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_profile),
                fallback = painterResource(R.drawable.ic_profile)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = buildAnnotatedString { withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) { append(username) }; append(" "); append(text) }, fontSize = 13.sp)
            if (!isCaption) { Text(text = "Reply", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp)) }
        }
    }
}
