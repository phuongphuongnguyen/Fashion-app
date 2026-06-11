package com.example.fashionapp.ui.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fashionapp.data.NotificationModel
import com.example.fashionapp.data.notification.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private val repository = NotificationRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _notifications = MutableStateFlow<List<NotificationModel>>(emptyList())
    val notifications: StateFlow<List<NotificationModel>> = _notifications.asStateFlow()

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.getNotificationsFlow(userId).collect { list ->
                _notifications.value = list
            }
        }
    }

    fun markAsRead(id: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.markAsRead(userId, id)
        }
    }
}
