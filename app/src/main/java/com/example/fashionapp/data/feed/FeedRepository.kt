package com.example.fashionapp.data.feed

import com.example.fashionapp.model.Post
import com.example.fashionapp.model.ProductTag
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FeedRepository {
    private val db      = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // ── Cache URL để tránh gọi Storage nhiều lần cho cùng 1 path ──────────
    private val urlCache = mutableMapOf<String, String>()
    private suspend fun resolveUrl(storagePath: String): String {
        if (storagePath.isBlank()) return ""
        urlCache[storagePath]?.let { return it }
        return try {
            val url = storage.reference.child(storagePath).downloadUrl.await().toString()
            urlCache[storagePath] = url
            url
        } catch (_: Exception) {
            ""
        }
    }

    // real-time feed từ Firestore
    fun getPostsFlow(): Flow<List<Post>> = callbackFlow {
        val listener = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // launch {} dùng đúng scope của callbackFlow — không cần import thêm
                launch {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        try {
                            // ── 1. Parse raw fields ────────────────────────────────
                            val authorId = doc.getString("authorId").orEmpty()
                            val caption  = doc.getString("caption").orEmpty()

                            @Suppress("UNCHECKED_CAST")
                            val imagePaths = (doc.get("images") as? List<String>) ?: emptyList()

                            @Suppress("UNCHECKED_CAST")
                            val rawTags = (doc.get("taggedProducts") as? List<*>) ?: emptyList<Any>()

                            val likeCount    = doc.getLong("likeCount")    ?: 0L
                            val commentCount = doc.getLong("commentCount") ?: 0L
                            val shareCount   = doc.getLong("shareCount")   ?: 0L

                            @Suppress("UNCHECKED_CAST")
                            val tags = (doc.get("tags") as? List<String>) ?: emptyList()

                            val createdAt = doc.getTimestamp("createdAt")

                            // ── 2. Lấy thông tin tác giả từ "users" ───────────────
                            var username  = ""
                            var avatarUrl = ""
                            if (authorId.isNotBlank()) {
                                try {
                                    val userDoc = db.collection("users")
                                        .document(authorId)
                                        .get()
                                        .await()
                                    username = userDoc.getString("username")
                                        ?: userDoc.getString("displayName")
                                                ?: ""
                                    val avatarRef = userDoc.getString("avatarRef").orEmpty()
                                    avatarUrl = resolveUrl(avatarRef)
                                } catch (_: Exception) { /* giữ rỗng */ }
                            }

                            // ── 3. Resolve image paths → URLs ─────────────────────
                            val imageUrls = imagePaths.map { resolveUrl(it) }

                            // ── 4. Resolve thumbnails của taggedProducts ───────────
                            val productTags = rawTags.mapNotNull { item ->
                                (item as? Map<*, *>)?.let { map ->
                                    val productId    = map["productId"]    as? String ?: ""
                                    val thumbnailRef = map["thumbnailRef"] as? String ?: ""
                                    val label        = map["label"]        as? String ?: ""
                                    ProductTag(
                                        productId    = productId,
                                        thumbnailUrl = resolveUrl(thumbnailRef),
                                        label        = label,
                                    )
                                }
                            }

                            // ── 5. Build Post ──────────────────────────────────────
                            Post(
                                id             = doc.id,
                                authorId       = authorId,
                                username       = username,
                                avatarUrl      = avatarUrl,
                                caption        = caption,
                                imageUrls      = imageUrls,
                                taggedProducts = productTags,
                                likeCount      = likeCount,
                                commentCount   = commentCount,
                                shareCount     = shareCount,
                                tags           = tags,
                                createdAt      = createdAt,
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                    trySend(posts)
                }
            }

        awaitClose { listener.remove() }
    }


    suspend fun toggleLike(postId: String, currentCount: Long, isLiked: Boolean) {
        val delta = if (isLiked) -1L else 1L
        db.collection("posts").document(postId)
            .update("likeCount", currentCount + delta)
    }
}