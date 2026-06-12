package com.example.fashionapp.data.user

import com.example.fashionapp.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserSession {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun updateCurrentUser(user: User?) {
        _currentUser.value = user
    }

    fun clear() {
        _currentUser.value = null
    }
}
