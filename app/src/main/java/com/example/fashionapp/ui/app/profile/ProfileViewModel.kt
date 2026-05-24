package com.example.fashionapp.ui.app.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.feed.FeedRepository
import com.example.fashionapp.model.Post
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
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
    private val auth = FirebaseAuth.getInstance()
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserPosts()
    }

    private fun loadUserPosts() {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            userRepository.getUserProfileFlow(userId).collect { user ->
                _uiState.value = _uiState.value.copy(user = user)
                if (user?.name.isNullOrBlank()) {
                    seedUserProfile(userId)
                }
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

    private fun seedUserProfile(userId: String) {
        val db = FirebaseFirestore.getInstance()
        val userEmail = auth.currentUser?.email ?: "User"
        val authorName = userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        val docRef = db.collection("users").document(userId)
        val data = hashMapOf(
            "name" to authorName,
            "avatarUrl" to "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
            "email" to userEmail,
            "followersCount" to 834,
            "followingCount" to 162
        )
        docRef.update(data as Map<String, Any>)
    }
}
