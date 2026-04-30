package com.example.fashionapp.ui.app.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fashionapp.ui.components.fadeInDiagonalGradientBorder
import com.example.fashionapp.data.PostsRepository
import com.example.fashionapp.data.StoriesRepository
import com.example.fashionapp.model.Post
import com.example.fashionapp.model.Story
import com.example.fashionapp.ui.app.profile.PostView
import kotlinx.coroutines.launch
import com.example.fashionapp.R.drawable
import com.example.fashionapp.ui.components.icon


@Composable
fun ProfileScreen(navController: NavController) {

    val coroutineScope = rememberCoroutineScope()

    val posts by PostsRepository.posts
    val stories by StoriesRepository.observeStories()

    LazyColumn {

        // HEADER + PROFILE + STORIES + GRID
        item {
            Column(modifier = Modifier.padding(16.dp)) {

                ProfileTopBar()

                Spacer(modifier = Modifier.height(16.dp))

                ProfileHeader()

                Spacer(modifier = Modifier.height(16.dp))

                //StoryHighlights()

                //Spacer(modifier = Modifier.height(16.dp))

                StoriesSection(stories)

                Spacer(modifier = Modifier.height(16.dp))

                // ProfileGridPreview()

                // Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // FEED POSTS (reuse PostView)
        items(posts) { post ->
            PostView(
                post = post,
                onDoubleClick = {
                    coroutineScope.launch {
                        PostsRepository.performLike(post.id)
                    }
                },
                onLikeToggle = {
                    coroutineScope.launch {
                        PostsRepository.toggleLike(post.id)
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileHeader() {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        AsyncImage(
            model = "https://randomuser.me/api/portraits/men/1.jpg",
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .fadeInDiagonalGradientBorder(
                    showBorder = true,
                    colors = listOf(
                        Color(0xFFFEDA75),
                        Color(0xFFFA7E1E),
                        Color(0xFFD62976),
                        Color(0xFF962FBF)
                    ),
                    shape = CircleShape
                )
                .padding(4.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProfileStat("12", "Posts")
            ProfileStat("1.2K", "Followers")
            ProfileStat("180", "Following")
        }
    }
}

@Composable
private fun ProfileStat(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count, style = MaterialTheme.typography.titleMedium)
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StoryHighlights() {

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

        repeat(5) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.LightGray, CircleShape)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text("Story", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StoriesSection(stories: List<Story>) {

    LazyRow {
        items(stories.size) { index ->
            val story = stories[index]

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(6.dp)
            ) {
                AsyncImage(
                    model = story.image,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = story.name,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopBar() {

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Romina",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        actions = {

            IconButton(onClick = { }) {
                Icon(
                    painter = painterResource(id = drawable.ic_outlined_add), // ➜ icon +
                    contentDescription = "Add"
                )
            }

            IconButton(onClick = { }) {
                Icon(
                    painter = painterResource(id = drawable.ic_outlined_add), // ➜ icon menu
                    contentDescription = "Menu"
                )
            }
        }
    )
}