package com.example.fashionapp.data.user

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private suspend fun resolveAvatarUrl(avatarUrl: String): String {
        val cleanAvatarUrl = avatarUrl.trim()
        if (cleanAvatarUrl.isBlank()) return ""
        if (cleanAvatarUrl.startsWith("http://") || cleanAvatarUrl.startsWith("https://")) {
            return cleanAvatarUrl
        }
        return try {
            val reference = if (cleanAvatarUrl.startsWith("gs://")) {
                storage.getReferenceFromUrl(cleanAvatarUrl)
            } else {
                storage.reference.child(cleanAvatarUrl.trimStart('/'))
            }
            reference.downloadUrl.await().toString()
        } catch (_: Exception) {
            cleanAvatarUrl
        }
    }

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

                launch {
                    val user = com.example.fashionapp.model.User(
                        id = snapshot.id,
                        username = snapshot.firstString("username", "name", "fullName"),
                        avatarUrl = resolveAvatarUrl(snapshot.getString("avatarUrl").orEmpty()),
                        email = snapshot.getString("email") ?: "",
                        followersCount = snapshot.firstLong("followersCount", "followerCount").toInt(),
                        followingCount = snapshot.firstLong("followingCount", "following").toInt()
                    )
                    trySend(user)
                }
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

    suspend fun seedUserProfile(userId: String, data: Map<String, Any>) {
        if (userId.isBlank()) return
        db.collection("users")
            .document(userId)
            .set(data, SetOptions.merge())
            .await()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.firstString(vararg fields: String): String {
        return fields.firstNotNullOfOrNull { field -> getString(field)?.takeIf { it.isNotBlank() } }
            .orEmpty()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.firstLong(vararg fields: String): Long {
        return fields.firstNotNullOfOrNull { field -> (get(field) as? Number)?.toLong() } ?: 0L
    }
}
