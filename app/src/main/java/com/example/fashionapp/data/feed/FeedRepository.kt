package com.example.fashionapp.data.feed

import com.example.fashionapp.model.Post
import com.example.fashionapp.model.ProductTag
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FeedRepository {
    private val db = FirebaseFirestore.getInstance()

    // Lắng nghe real-time — tự động cập nhật khi Firestore thay đổi
    fun getPostsFlow(): Flow<List<Post>> = callbackFlow {
        val listener = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val posts = snapshot.documents.mapNotNull { doc ->
                    try {
                        // Parse productTags thủ công vì là nested list
                        val rawTags = doc.get("productTags") as? List<*>
                        val tags = rawTags?.mapNotNull { item ->
                            (item as? Map<*, *>)?.let {
                                ProductTag(
                                    imageUrl = it["imageUrl"] as? String ?: "",
                                    productId = it["productId"] as? String ?: ""
                                )
                            }
                        } ?: emptyList()

                        Post(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            username = doc.getString("username") ?: "",
                            avatarUrl = doc.getString("avatarUrl") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            caption = doc.getString("caption") ?: "",
                            likeCount = doc.getLong("likeCount") ?: 0,
                            createdAt = doc.getTimestamp("createdAt"),
                            productTags = tags
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(posts)
            }

        awaitClose { listener.remove() }
    }

    // Toggle like (optimistic update)
    suspend fun toggleLike(postId: String, currentCount: Long, isLiked: Boolean) {
        val delta = if (isLiked) -1L else 1L
        db.collection("posts").document(postId)
            .update("likeCount", currentCount + delta)
    }
}