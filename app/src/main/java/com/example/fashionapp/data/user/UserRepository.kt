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
                if (error != null || snapshot == null || !snapshot.exists()) {
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
