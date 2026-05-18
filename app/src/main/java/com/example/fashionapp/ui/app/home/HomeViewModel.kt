package com.example.fashionapp.ui.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.feed.FeedRepository
import com.example.fashionapp.model.Comment
import com.example.fashionapp.model.Post
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.UUID

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    // Map postId -> isLiked (local optimistic state)
    val likedPosts: Map<String, Boolean> = emptyMap()
)

class HomeViewModel : ViewModel() {
    private val repository = FeedRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            repository.getPostsFlow().collect { posts ->
                _uiState.value = _uiState.value.copy(
                    posts     = posts,
                    isLoading = false
                )
            }
        }
    }

    fun toggleLike(postId: String) {
        val currentState = _uiState.value
        val currentLiked = currentState.likedPosts[postId] ?: false
        val post = currentState.posts.find { it.id == postId } ?: return

        // 1. Cập nhật local liked state
        val newLikedPosts = currentState.likedPosts.toMutableMap().apply {
            put(postId, !currentLiked)
        }

        // 2. Cập nhật local like count trong danh sách posts (để UI đổi số ngay lập tức)
        val newPosts = currentState.posts.map {
            if (it.id == postId) {
                val delta = if (currentLiked) -1L else 1L
                it.copy(likeCount = (it.likeCount + delta).coerceAtLeast(0))
            } else it
        }

        _uiState.value = currentState.copy(
            posts      = newPosts,
            likedPosts = newLikedPosts
        )

        viewModelScope.launch {
            //repository.toggleLike(postId, post.likeCount, currentLiked)
        }
    }

    fun addComment(postId: String, text: String) {
        if (text.isBlank()) return

        val currentState = _uiState.value
        val newComment = Comment(
            id = UUID.randomUUID().toString(),
            username = "You", 
            text = text,
            createdAt = Timestamp.now()
        )

        val newPosts = currentState.posts.map { post ->
            if (post.id == postId) {
                post.copy(
                    comments = post.comments + newComment,
                    commentCount = post.commentCount + 1
                )
            } else post
        }

        _uiState.value = currentState.copy(posts = newPosts)
    }
}
