package com.example.fashionapp.ui.app.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.feed.FeedRepository
import com.example.fashionapp.data.user.UserRepository
import com.example.fashionapp.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.firstOrNull

data class SavedUiState(
    val savedPosts: List<Post> = emptyList(),
    val savedPostIds: Set<String> = emptySet(),
    val isLoading: Boolean = true
)

class SavedViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val feedRepository = FeedRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _uiState = MutableStateFlow(SavedUiState())
    val uiState: StateFlow<SavedUiState> = _uiState.asStateFlow()

    init {
        loadSavedPosts()
    }

    private fun currentUserId(): String? = auth.currentUser?.uid

    private fun loadSavedPosts() {
        val userId = currentUserId()
        if (userId == null) {
            _uiState.value = SavedUiState(isLoading = false)
            return
        }

        viewModelScope.launch {
            combine(
                userRepository.getSavedPostIdsFlow(userId),
                feedRepository.getPostsFlow()
            ) { savedIds, allPosts ->
                val savedIdSet = savedIds.toSet()
                val savedPostsList = allPosts.filter { it.id in savedIdSet }
                SavedUiState(
                    savedPosts = savedPostsList,
                    savedPostIds = savedIdSet,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleSave(postId: String) {
        val userId = currentUserId() ?: return
        val currentIds = _uiState.value.savedPostIds
        viewModelScope.launch {
            val willSave = !currentIds.contains(postId)
            if (currentIds.contains(postId)) {
                userRepository.unsavePost(userId, postId)
            } else {
                userRepository.savePost(userId, postId)
            }

            if (willSave) {
                val post = _uiState.value.savedPosts.find { it.id == postId }
                    ?: feedRepository.getPostsFlow().firstOrNull()?.find { it.id == postId }

                if (post != null && post.authorId.isNotBlank() && post.authorId != userId) {
                    val userA = userRepository.getUserProfile(userId)
                    val senderName = userA?.username?.takeIf { it.isNotBlank() }
                        ?: userA?.name
                        ?: "Ai đó"
                    val notificationRepo = com.example.fashionapp.data.notification.NotificationRepository()
                    notificationRepo.addNotification(
                        userId = post.authorId,
                        message = "$senderName đã lưu bài viết của bạn.",
                        type = "SAVE"
                    )
                }
            }
        }
    }
}
