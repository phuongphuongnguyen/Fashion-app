package com.example.fashionapp.data.user

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getUserProfileFlow(userId: String): Flow<com.example.fashionapp.model.User?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val user = com.example.fashionapp.model.User(
                    id = snapshot.id,
                    name = snapshot.getString("name") ?: "",
                    avatarUrl = snapshot.getString("avatarUrl") ?: "",
                    email = snapshot.getString("email") ?: "",
                    followersCount = snapshot.getLong("followersCount")?.toInt() ?: 0,
                    followingCount = snapshot.getLong("followingCount")?.toInt() ?: 0
                )
                trySend(user)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getUserProfile(userId: String): com.example.fashionapp.model.User? {
        if (userId.isBlank()) return null
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            if (!snapshot.exists()) return null
            com.example.fashionapp.model.User(
                id = snapshot.id,
                name = snapshot.getString("name") ?: "",
                avatarUrl = snapshot.getString("avatarUrl") ?: "",
                email = snapshot.getString("email") ?: "",
                followersCount = snapshot.getLong("followersCount")?.toInt() ?: 0,
                followingCount = snapshot.getLong("followingCount")?.toInt() ?: 0
            )
        } catch (_: Exception) { null }
    }

    suspend fun seedUserProfile(userId: String, email: String) {
        if (userId.isBlank()) return
        val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        // Tạo avatar ngẫu nhiên dựa trên UID để mỗi user có 1 ảnh khác nhau nếu chưa có
        val randomSeed = userId.take(5)
        val avatarUrl = "https://api.dicebear.com/7.x/avataaars/svg?seed=$randomSeed"
        
        val data = hashMapOf(
            "name" to name,
            "avatarUrl" to avatarUrl,
            "email" to email,
            "followersCount" to (100..1000).random(),
            "followingCount" to (50..500).random()
        )
        // Dùng set với merge để không ghi đè phoneNumber hay các field khác đã có
        db.collection("users").document(userId).set(data as Map<String, Any>, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    fun getSavedPostIdsFlow(userId: String): Flow<List<String>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("users").document(userId).collection("saved_posts")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val postIds = snapshot.documents.mapNotNull { doc ->
                    doc.id
                }
                trySend(postIds)
            }
        awaitClose { listener.remove() }
    }

    suspend fun savePost(userId: String, postId: String) {
        if (userId.isBlank() || postId.isBlank()) return
        val data = hashMapOf("savedAt" to System.currentTimeMillis())
        db.collection("users").document(userId).collection("saved_posts").document(postId).set(data).await()
    }

    suspend fun unsavePost(userId: String, postId: String) {
        if (userId.isBlank() || postId.isBlank()) return
        db.collection("users").document(userId).collection("saved_posts").document(postId).delete().await()
    }
}
