package com.example.fashionapp.ui.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.feed.FeedRepository
import com.example.fashionapp.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    // Map postId -> isLiked (local state)
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
                    posts = posts,
                    isLoading = false
                )
            }
        }
    }

    fun toggleLike(postId: String) {
        val currentLiked = _uiState.value.likedPosts[postId] ?: false
        val post = _uiState.value.posts.find { it.id == postId } ?: return

        // Cập nhật UI ngay lập tức (optimistic)
        _uiState.value = _uiState.value.copy(
            likedPosts = _uiState.value.likedPosts + (postId to !currentLiked)
        )

        viewModelScope.launch {
            repository.toggleLike(postId, post.likeCount, currentLiked)
        }
    }
}