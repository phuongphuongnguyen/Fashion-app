package com.example.fashionapp.ui.app.profile

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fashionapp.R.drawable
import com.example.fashionapp.model.Post
import com.example.fashionapp.ui.components.*

@Composable
fun PostView(
    post: Post,
    onDoubleClick: (Post) -> Unit,
    onLikeToggle: (Post) -> Unit
) {
    Column {
        PostHeader(post)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color.LightGray)
        ) {
            AsyncImage(
                model = post.image,
                contentDescription = "Post image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            DoubleTapPhotoLikeAnimation(
                onDoubleTap = { onDoubleClick(post) }
            )
        }

        PostFooter(post, onLikeToggle)

        HorizontalDivider()
    }
}

@Composable
private fun PostHeader(post: Post) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            AsyncImage(
                model = post.user.image,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = post.user.username,
                style = MaterialTheme.typography.titleSmall
            )
        }

        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "More options"
        )
    }
}

@Composable
private fun PostFooter(
    post: Post,
    onLikeToggle: (Post) -> Unit
) {
    PostFooterIconSection(post, onLikeToggle)
    PostFooterTextSection(post)
}

@Composable
private fun PostFooterIconSection(
    post: Post,
    onLikeToggle: (Post) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            AnimLikeButton(post, onLikeToggle)

            PostIconButton {
                Icon(
                    painter = painterResource(id = drawable.ic_outlined_comment),
                    contentDescription = "Comment"
                )
            }

            PostIconButton {
                Icon(
                    painter = painterResource(id = drawable.ic_dm),
                    contentDescription = "Send"
                )
            }
        }

        PostIconButton {
            Icon(
                painter = painterResource(id = drawable.ic_bookmark),
                contentDescription = "Bookmark"
            )
        }
    }
}

@Composable
private fun PostFooterTextSection(post: Post) {
    Column(
        modifier = Modifier.padding(
            start = 12.dp,
            end = 12.dp,
            bottom = 8.dp
        )
    ) {

        Text(
            text = "${post.likesCount} likes",
            style = MaterialTheme.typography.titleSmall
        )

        Text(
            text = "View all ${post.commentsCount} comments",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = post.timeStamp.getTimeElapsedText(),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp)
        )
    }
}

/**
 * Convert timestamp
 */
private fun Long.getTimeElapsedText(): String {
    val now = System.currentTimeMillis()

    return DateUtils.getRelativeTimeSpanString(
        this,
        now,
        0L,
        DateUtils.FORMAT_ABBREV_TIME
    ).toString()
}