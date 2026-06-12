package com.example.fashionapp.ui.app.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.feed.FeedRepository
import com.example.fashionapp.data.user.UserRepository
import com.example.fashionapp.data.user.UserSession
import com.example.fashionapp.model.Post
import com.example.fashionapp.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val posts: List<Post> = emptyList(),
    val user: User? = null,
    val isLoading: Boolean = true,
    val isOwnProfile: Boolean = true,
    val isFollowing: Boolean = false,
    val bioError: String? = null
)

// viewedUserId = null → xem trang của chính mình. Khác → xem trang user khác (read-only + Follow).
class ProfileViewModel(
    viewedUserId: String? = null
) : ViewModel() {
    private val feedRepository = FeedRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val myUid = auth.currentUser?.uid.orEmpty()
    private val targetId = viewedUserId?.takeIf { it.isNotBlank() } ?: myUid
    private val isOwn = targetId.isNotBlank() && targetId == myUid

    private val _uiState = MutableStateFlow(ProfileUiState(isOwnProfile = isOwn))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        if (isOwn) observeUserSession()
        observeUserProfile()
        loadUserPosts()
        if (!isOwn) observeFollowing()
    }

    private fun observeUserSession() {
        viewModelScope.launch {
            UserSession.currentUser.collect { user ->
                if (user != null) {
                    _uiState.value = _uiState.value.copy(user = user)
                }
            }
        }
    }

    private fun observeUserProfile() {
        if (targetId.isBlank()) return
        viewModelScope.launch {
            userRepository.getUserProfileFlow(targetId).collect { user ->
                if (user != null) {
                    if (isOwn) UserSession.updateCurrentUser(user)
                    _uiState.value = _uiState.value.copy(user = user)
                }
            }
        }
    }

    private fun loadUserPosts() {
        if (targetId.isBlank()) return
        viewModelScope.launch {
            feedRepository.getPostsByAuthorFlow(targetId).collect { posts ->
                _uiState.value = _uiState.value.copy(
                    posts = posts,
                    isLoading = false
                )
            }
        }
    }

    private fun observeFollowing() {
        viewModelScope.launch {
            userRepository.isFollowingFlow(myUid, targetId).collect { following ->
                _uiState.value = _uiState.value.copy(isFollowing = following)
            }
        }
    }

    fun toggleFollow() {
        if (isOwn || myUid.isBlank() || targetId.isBlank()) return
        val shouldFollow = !_uiState.value.isFollowing
        viewModelScope.launch {
            runCatching { userRepository.setFollowing(myUid, targetId, shouldFollow) }
        }
    }

    fun updateBio(bio: String) {
        if (!isOwn || myUid.isBlank()) return
        viewModelScope.launch {
            runCatching { userRepository.updateUserBio(myUid, bio) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(bioError = "Could not update bio")
                }
        }
    }

    fun consumeBioError() {
        _uiState.value = _uiState.value.copy(bioError = null)
    }
}

class ProfileViewModelFactory(
    private val userId: String?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProfileViewModel(userId) as T
    }
}
