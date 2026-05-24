package com.example.fashionapp.ui.app.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.feed.FeedRepository
import com.example.fashionapp.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.fashionapp.data.user.UserRepository
import com.example.fashionapp.model.User

data class ProfileUiState(
    val posts: List<Post> = emptyList(),
    val user: User? = null,
    val isLoading: Boolean = true
)

class ProfileViewModel : ViewModel() {
    private val feedRepository = FeedRepository()
    private val userRepository = UserRepository()
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserPosts()
    }

    private fun currentUserId(): String = "u001"

    private fun loadUserPosts() {
        val userId = currentUserId()
        
        viewModelScope.launch {
            userRepository.getUserProfileFlow(userId).collect { user ->
                _uiState.value = _uiState.value.copy(user = user)
            }
        }
        
        viewModelScope.launch {
            feedRepository.getPostsFlow().collect { allPosts ->
                val userPosts = allPosts.filter { it.authorId == userId }
                _uiState.value = _uiState.value.copy(
                    posts = userPosts,
                    isLoading = false
                )
            }
        }
    }

}
