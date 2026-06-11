package com.example.fashionapp.ui.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.feed.FeedRepository
import com.example.fashionapp.data.user.UserSession
import com.example.fashionapp.model.Comment
import com.example.fashionapp.model.Post
import com.example.fashionapp.model.User
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val user: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    // Map postId -> isLiked (local optimistic state)
    val likedPosts: Map<String, Boolean> = emptyMap(),
    val pendingLikePostIds: Set<String> = emptySet()
)

class HomeViewModel : ViewModel() {
    private val repository = FeedRepository()
    private var likedPostsJob: Job? = null

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
        observeUserSession()
    }

    private fun observeUserSession() {
        viewModelScope.launch {
            UserSession.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(user = user)
                observeLikedPosts(user?.id.orEmpty())
            }
        }
    }

    private fun observeLikedPosts(userId: String) {
        likedPostsJob?.cancel()
        if (userId.isBlank()) {
            _uiState.value = _uiState.value.copy(likedPosts = emptyMap())
            return
        }
        likedPostsJob = viewModelScope.launch {
            repository.getLikedPostIdsFlow(userId).collect { ids ->
                _uiState.value = _uiState.value.copy(
                    likedPosts = ids.associateWith { true }
                )
            }
        }
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
        val userId = currentState.user?.id.orEmpty()
        if (userId.isBlank()) return
        if (postId in currentState.pendingLikePostIds) return
        val currentLiked = currentState.likedPosts[postId] ?: false

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
            likedPosts = newLikedPosts,
            pendingLikePostIds = currentState.pendingLikePostIds + postId
        )

        viewModelScope.launch {
            runCatching {
                repository.toggleLike(postId, userId)
            }.onFailure {
                _uiState.value = currentState
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    pendingLikePostIds = _uiState.value.pendingLikePostIds - postId
                )
            }
        }
    }

    fun addComment(postId: String, text: String) {
        if (text.isBlank()) return

        val currentState = _uiState.value
        val user = currentState.user
        
        val newComment = Comment(
            id = UUID.randomUUID().toString(),
            username = user?.name ?: "You", 
            avatarUrl = user?.avatarUrl ?: "",
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

        viewModelScope.launch {
            runCatching {
                repository.addComment(postId, newComment)
            }.onFailure {
                _uiState.value = currentState
            }
        }
    }
}
