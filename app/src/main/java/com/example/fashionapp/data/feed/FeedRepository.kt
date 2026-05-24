package com.example.fashionapp.data.feed

import com.example.fashionapp.model.Post
import com.example.fashionapp.model.ProductTag
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FeedRepository {
    private val db      = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val urlCache = mutableMapOf<String, String>()

    // ── In-Memory Cache ──────────────────────────────────────────────────────
    private var cachedPosts: List<Post> = emptyList()

    private suspend fun resolveUrl(storagePath: String): String {
        if (storagePath.isBlank()) return ""
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) return storagePath
        urlCache[storagePath]?.let { return it }
        return try {
            val url = storage.reference.child(storagePath).downloadUrl.await().toString()
            urlCache[storagePath] = url
            url
        } catch (_: Exception) { storagePath }
    }

    fun getPostsFlow(): Flow<List<Post>> = callbackFlow {
        val listener = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(cachedPosts) // Trả về cache nếu lỗi
                    return@addSnapshotListener
                }

                launch {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        try {
                            val authorId    = doc.getString("authorId").orEmpty()
                            val caption     = doc.getString("caption").orEmpty()
                            val likeCount   = doc.getLong("likeCount")    ?: 0L
                            val commentCount= doc.getLong("commentCount") ?: 0L
                            val shareCount  = doc.getLong("shareCount")   ?: 0L
                            val createdAt   = doc.getTimestamp("createdAt")

                            @Suppress("UNCHECKED_CAST")
                            val imagePaths = (doc.get("images") as? List<String>) ?: emptyList()

                            @Suppress("UNCHECKED_CAST")
                            val tags = (doc.get("tags") as? List<String>) ?: emptyList()

                            @Suppress("UNCHECKED_CAST")
                            val rawTags = (doc.get("taggedProducts") as? List<*>) ?: emptyList<Any>()

                            val authorName = doc.getString("authorName").orEmpty()
                            val authorAvt  = resolveUrl(doc.getString("authorAvt").orEmpty())

                            val imageUrls = imagePaths.map { resolveUrl(it) }

                            val productTags = rawTags.mapNotNull { item ->
                                (item as? Map<*, *>)?.let { map ->
                                    ProductTag(
                                        productId    = map["productId"]    as? String ?: "",
                                        thumbnailUrl = resolveUrl(map["thumbnailRef"] as? String ?: ""),
                                        label        = map["label"]        as? String ?: "",
                                    )
                                }
                            }

                            Post(
                                id             = doc.id,
                                authorId       = authorId,
                                authorName     = authorName,
                                authorAvt      = authorAvt,
                                caption        = caption,
                                imageUrls      = imageUrls,
                                taggedProducts = productTags,
                                likeCount      = likeCount,
                                commentCount   = commentCount,
                                shareCount     = shareCount,
                                tags           = tags,
                                createdAt      = createdAt,
                            )
                        } catch (_: Exception) { null }
                    }
                    cachedPosts = posts // Cập nhật cache
                    trySend(posts)
                }
            }

        awaitClose { listener.remove() }
    }.onStart {
        if (cachedPosts.isNotEmpty()) emit(cachedPosts) // Phát ngay dữ liệu cũ nếu có
    }

    suspend fun toggleLike(postId: String, currentCount: Long, isLiked: Boolean) {
        val delta = if (isLiked) -1L else 1L
        db.collection("posts").document(postId)
            .update("likeCount", currentCount + delta)
    }

    fun clearCache() { cachedPosts = emptyList() }
}
