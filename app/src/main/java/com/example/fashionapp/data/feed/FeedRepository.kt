package com.example.fashionapp.data.feed

import com.example.fashionapp.model.Post
import com.example.fashionapp.model.ProductTag
import com.example.fashionapp.data.StorageUrlResolver
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class FeedRepository {
    private val db = FirebaseFirestore.getInstance()

    // ── In-Memory Cache ──────────────────────────────────────────────────────
    private var cachedPosts: List<Post> = emptyList()

    fun getPostsFlow(): Flow<List<Post>> = callbackFlow {
        val listener = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(cachedPosts)
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
                            val authorAvt  = StorageUrlResolver.resolve(doc.getString("authorAvt").orEmpty())

                            val imageUrls = imagePaths.map { StorageUrlResolver.resolve(it) }

                            val productTags = rawTags.mapNotNull { item ->
                                (item as? Map<*, *>)?.let { map ->
                                    ProductTag(
                                        productId    = map["productId"]    as? String ?: "",
                                        thumbnailUrl = StorageUrlResolver.resolve(map["thumbnailRef"] as? String ?: ""),
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
                    cachedPosts = posts
                    trySend(posts)
                }
            }

        awaitClose { listener.remove() }
    }.onStart {
        if (cachedPosts.isNotEmpty()) emit(cachedPosts)
    }

    suspend fun toggleLike(postId: String, currentCount: Long, isLiked: Boolean) {
        val delta = if (isLiked) -1L else 1L
        db.collection("posts").document(postId)
            .update("likeCount", currentCount + delta)
    }

    fun clearCache() { cachedPosts = emptyList() }
}
