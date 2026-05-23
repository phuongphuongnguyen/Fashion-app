package com.example.fashionapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.app.settings.AppSettingsViewModel
import com.example.fashionapp.ui.app.settings.AppLanguage
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FeedPostItem(
    post: Post,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    val settings = LocalAppSettings.current
    val isDark = settings.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF1A1A2E)

    Column {
        PostHeader(
            avatarUrl = post.authorAvt,
            username = post.authorName,
            isDark = isDark,
            textColor = textColor
        )

        if (post.imageUrls.isNotEmpty()) {
            PostImagesRow(
                imageUrls = post.imageUrls,
                isDark = isDark
            )
        }

        if (post.taggedProducts.isNotEmpty()) {
            ProductTagsRow(
                tags = post.taggedProducts,
                isDark = isDark
            )
        }

        ActionRow(
            isLiked = isLiked,
            onLikeClick = onLikeClick,
            onCommentClick = onCommentClick,
            imageCount = post.imageUrls.size,
            isDark = isDark
        )

        Text(
            text = "${formatLikeCount(post.likeCount)} " + settings.t(
                en = "likes",
                vi = "lượt thích",
                fr = "j'aime",
                ja = "いいね",
                ko = "좋아요",
                zh = "次赞"
            ),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(4.dp))

        CaptionRow(
            username = post.authorName,
            caption = post.caption,
            timestamp = post.createdAt?.toDate(),
            textColor = textColor,
            settings = settings
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun PostHeader(
    avatarUrl: String,
    username: String,
    isDark: Boolean,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE)).padding(2.dp)) {
            AsyncImage(
                model = avatarUrl.ifBlank { null },
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_profile)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = username,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = null,
                tint = if (isDark) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun PostImagesRow(imageUrls: List<String>, isDark: Boolean) {
    val bg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF9F9F9)
    if (imageUrls.size == 1) {
        AsyncImage(
            model = imageUrls.first().ifBlank { null },
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(bg),
            contentScale = ContentScale.Fit,
            error = painterResource(R.drawable.ic_launcher_foreground)
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
                    modifier = Modifier.fillParentMaxWidth().fillMaxHeight().background(bg),
                    contentScale = ContentScale.Fit,
                    error = painterResource(R.drawable.ic_launcher_foreground)
                )
            }
        }
    }
}

@Composable
fun ProductTagsRow(tags: List<ProductTag>, isDark: Boolean) {
    val bg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF0F0F0)
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.take(4).forEach { tag ->
            AsyncImage(
                model = tag.thumbnailUrl.ifBlank { null },
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(bg),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_launcher_foreground)
            )
        }
    }
}

@Composable
fun ActionRow(
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    imageCount: Int,
    isDark: Boolean
) {
    val iconColor = if (isDark) Color.White else Color.Black
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLikeClick) {
                Icon(
                    painter = painterResource(if (isLiked) R.drawable.ic_like_full else R.drawable.ic_like),
                    contentDescription = null,
                    tint = if (isLiked) Color(0xFFF04957) else iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onCommentClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_comment),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = { }) {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        if (imageCount > 1) {
            Row(modifier = Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(imageCount.coerceAtMost(5)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF3669C9)))
                }
            }
        }
        var bookmarked by remember { mutableStateOf(false) }
        IconButton(modifier = Modifier.align(Alignment.CenterEnd), onClick = { bookmarked = !bookmarked }) {
            Icon(
                painter = painterResource(if (bookmarked) R.drawable.ic_bookmark_full else R.drawable.ic_bookmark),
                contentDescription = null,
                tint = if (bookmarked) Color(0xFF3669C9) else iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun CaptionRow(
    username: String,
    caption: String,
    timestamp: Date?,
    textColor: Color,
    settings: AppSettingsViewModel
) {
    val isDark = settings.isDarkMode
    val subTextColor = if (isDark) Color(0xFFB0B0B0) else Color.Gray

    val locale = when (settings.language) {
        AppLanguage.VIETNAMESE -> Locale("vi", "VN")
        AppLanguage.FRENCH -> Locale.FRANCE
        AppLanguage.JAPANESE -> Locale.JAPAN
        AppLanguage.KOREAN -> Locale.KOREA
        AppLanguage.CHINESE -> Locale.CHINA
        else -> Locale.ENGLISH
    }
    
    val dateStr = timestamp?.let { SimpleDateFormat("MMMM d, yyyy", locale).format(it) } ?: ""
    
    Text(
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold, color = textColor)) { append(username) }
            append(" ")
            withStyle(style = SpanStyle(color = textColor)) { append(caption) }
        },
        fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
    )
    if (dateStr.isNotEmpty()) {
        Text(
            text = "$dateStr •",
            fontSize = 11.sp,
            color = subTextColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )
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
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit
) {
    val settings = LocalAppSettings.current
    val isDark = settings.isDarkMode
    val sheetBgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1A1A2E)
    val subTextColor = if (isDark) Color(0xFFB0B0B0) else Color.Gray
    val dividerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE)

    var commentText by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBgColor,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        contentWindowInsets = { WindowInsets.ime }
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.85f).fillMaxWidth().navigationBarsPadding()) {
            Text(
                text = settings.t(
                    en = "Comments",
                    vi = "Bình luận",
                    fr = "Commentaires",
                    ja = "コメント",
                    ko = "댓글",
                    zh = "评论"
                ),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = textColor,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    CommentItem(
                        username = post.authorName,
                        avatarUrl = post.authorAvt,
                        text = post.caption,
                        isCaption = true,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        settings = settings
                    )
                }
                items(post.comments) { comment ->
                    CommentItem(
                        username = comment.username,
                        avatarUrl = comment.avatarUrl,
                        text = comment.text,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        settings = settings
                    )
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            Row(modifier = Modifier.fillMaxWidth().background(sheetBgColor).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE))) {
                    Icon(painter = painterResource(R.drawable.ic_profile), contentDescription = null, modifier = Modifier.fillMaxSize().padding(4.dp), tint = Color.Unspecified)
                }
                TextField(
                    value = commentText, onValueChange = { commentText = it },
                    placeholder = {
                        Text(
                            settings.t(
                                en = "Add a comment...",
                                vi = "Thêm bình luận...",
                                fr = "Ajouter un commentaire...",
                                ja = "コメントを追加...",
                                ko = "댓글 추가...",
                                zh = "添加评论..."
                            ),
                            fontSize = 14.sp,
                            color = subTextColor
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
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
fun CommentItem(
    username: String,
    avatarUrl: String,
    text: String,
    isCaption: Boolean = false,
    textColor: Color,
    subTextColor: Color,
    settings: AppSettingsViewModel
) {
    val isDark = settings.isDarkMode
    Row(verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE))) {
            AsyncImage(model = avatarUrl.ifBlank { null }, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, error = painterResource(R.drawable.ic_profile))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold, color = textColor)) { append(username) }
                    append(" ")
                    withStyle(style = SpanStyle(color = textColor)) { append(text) }
                },
                fontSize = 13.sp
            )
            if (!isCaption) {
                Text(
                    text = settings.t(
                        en = "Reply",
                        vi = "Trả lời",
                        fr = "Répondre",
                        ja = "返信",
                        ko = "답글",
                        zh = "回复"
                    ),
                    fontSize = 11.sp,
                    color = subTextColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
