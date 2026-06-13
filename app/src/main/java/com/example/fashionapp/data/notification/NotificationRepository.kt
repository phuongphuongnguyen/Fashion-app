package com.example.fashionapp.data.notification

import com.example.fashionapp.data.NotificationModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()

    // 1. Lưu thông báo mới vào Firestore dưới dạng subcollection
    suspend fun addNotification(userId: String, message: String, type: String): String {
        if (userId.isBlank()) return ""
        val docRef = db.collection("users")
            .document(userId)
            .collection("user_notifications")
            .document()

        val notification = NotificationModel(
            id = docRef.id,
            userId = userId,
            message = message,
            type = type,
            isRead = false,
            createdAt = Timestamp.now()
        )
        return try {
            docRef.set(notification).await()
            docRef.id
        } catch (e: Exception) {
            ""
        }
    }

    // 2. Lấy luồng dữ liệu (Flow) realtime danh sách thông báo theo userId
    fun getNotificationsFlow(userId: String): Flow<List<NotificationModel>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users")
            .document(userId)
            .collection("user_notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(NotificationModel::class.java)?.copy(id = doc.id)
                    }
                    trySend(list)
                }
            }

        awaitClose { listener.remove() }
    }

    // 3. Đánh dấu thông báo đã đọc
    suspend fun markAsRead(userId: String, notificationId: String) {
        if (userId.isBlank() || notificationId.isBlank()) return
        try {
            db.collection("users")
                .document(userId)
                .collection("user_notifications")
                .document(notificationId)
                .update("isRead", true)
                .await()
        } catch (_: Exception) {}
    }
}
