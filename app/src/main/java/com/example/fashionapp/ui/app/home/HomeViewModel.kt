package com.example.fashionapp.ui.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.feed.FeedRepository
import com.example.fashionapp.data.user.UserRepository
import com.example.fashionapp.data.user.UserSession
import com.example.fashionapp.model.Comment
import com.example.fashionapp.model.Post
import com.example.fashionapp.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val user: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    // Map postId -> isLiked (local optimistic state)
    val likedPosts: Map<String, Boolean> = emptyMap()
)

class HomeViewModel : ViewModel() {
    private val repository = FeedRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
        observeUserSession()
        loadUserProfile()
    }

    private fun observeUserSession() {
        viewModelScope.launch {
            UserSession.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(user = user)
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

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: ""
        
        viewModelScope.launch {
            // Thử lấy trực tiếp một lần trước (Request)
            val user = userRepository.getUserProfile(userId)
            if (user != null && user.name.isNotBlank()) {
                _uiState.value = _uiState.value.copy(user = user)
            } else if (email.isNotBlank()) {
                // Nếu không có, khởi tạo luôn (Seed)
                userRepository.seedUserProfile(userId, email)
            }

            // Sau đó lắng nghe thay đổi (Flow)
            userRepository.getUserProfileFlow(userId).collect { updatedUser ->
                if (updatedUser != null) {
                    _uiState.value = _uiState.value.copy(user = updatedUser)
                }
            }
        }
    }



    fun toggleLike(postId: String) {
        val currentState = _uiState.value
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
            likedPosts = newLikedPosts
        )

        viewModelScope.launch {
            //val post = currentState.posts.find { it.id == postId } ?: return@launch
            //repository.toggleLike(postId, post.likeCount, currentLiked)
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
    }
}
