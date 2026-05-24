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

data class SavedUiState(
    val savedPosts: List<Post> = emptyList(),
    val savedPostIds: Set<String> = emptySet(),
    val isLoading: Boolean = true
)

class SavedViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val feedRepository = FeedRepository()
    
    private val _uiState = MutableStateFlow(SavedUiState())
    val uiState: StateFlow<SavedUiState> = _uiState.asStateFlow()

    init {
        loadSavedPosts()
    }

    private fun currentUserId(): String = "u001"

    private fun loadSavedPosts() {
        val userId = currentUserId()
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
        val userId = currentUserId()
        val currentIds = _uiState.value.savedPostIds
        viewModelScope.launch {
            if (currentIds.contains(postId)) {
                userRepository.unsavePost(userId, postId)
            } else {
                userRepository.savePost(userId, postId)
            }
        }
    }
}
