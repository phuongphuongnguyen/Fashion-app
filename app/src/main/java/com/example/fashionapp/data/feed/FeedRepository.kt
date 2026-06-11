package com.example.fashionapp.data.feed

import android.util.Log
import com.example.fashionapp.model.Comment
import com.example.fashionapp.model.Post
import com.example.fashionapp.model.ProductTag
import com.example.fashionapp.data.StorageUrlResolver
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FeedRepository {
    private companion object {
        const val TAG = "FeedRepository"
    }

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

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
                    val posts = resolvePosts(snapshot.documents)

                    cachedPosts = posts
                    trySend(posts)
                }
            }

        awaitClose { listener.remove() }
    }.onStart {
        if (cachedPosts.isNotEmpty()) emit(cachedPosts)
    }

    fun getPostsByAuthorFlow(authorId: String): Flow<List<Post>> = callbackFlow {
        if (authorId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // Chỉ lọc theo authorId (không orderBy để khỏi cần composite index),
        // sắp xếp mới nhất trước ở client.
        val listener = db.collection("posts")
            .whereEqualTo("authorId", authorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                launch {
                    val posts = resolvePosts(snapshot.documents)
                        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    trySend(posts)
                }
            }

        awaitClose { listener.remove() }
    }

    private suspend fun resolvePosts(documents: List<DocumentSnapshot>): List<Post> = coroutineScope {
        documents.map { doc ->
            async {
                parsePost(doc)
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun parsePost(doc: DocumentSnapshot): Post? = coroutineScope {
        try {
            val authorId = doc.getString("authorId").orEmpty()
            val authorName = doc.getString("authorName").orEmpty()
            val caption = doc.getString("caption").orEmpty()
            val likeCount = doc.getLong("likeCount") ?: 0L
            val createdAt = doc.getTimestamp("createdAt")

            @Suppress("UNCHECKED_CAST")
            val rawComments = (doc.get("comments") as? List<*>) ?: emptyList<Any>()
            val legacyComments = rawComments.mapNotNull { item ->
                (item as? Map<*, *>)?.let { map ->
                    Comment(
                        id = map["id"] as? String ?: "",
                        username = map["username"] as? String ?: "",
                        avatarUrl = map["avatarUrl"] as? String ?: "",
                        text = map["text"] as? String ?: "",
                        createdAt = map["createdAt"] as? com.google.firebase.Timestamp
                    )
                }
            }
            val comments = getPostComments(doc.id, legacyComments)

            @Suppress("UNCHECKED_CAST")
            val imagePaths = (doc.get("images") as? List<String>) ?: emptyList()

            val imageUrlsDeferred = imagePaths.map { path ->
                async { StorageUrlResolver.resolve(path) }
            }

            val logoRef = doc.getString("logoRef").orEmpty()
            val authorAvtRef = doc.getString("authorAvt").orEmpty()
            val avatarRef = logoRef.ifBlank { authorAvtRef }
            val authorAvtDeferred = async {
                val resolvedAvatar = StorageUrlResolver.resolve(avatarRef)
                if (resolvedAvatar.isBlank()) {
                    Log.w(
                        TAG,
                        "Post ${doc.id} has no resolved avatar. authorId=$authorId logoRef='$logoRef' authorAvt='$authorAvtRef'"
                    )
                }
                resolvedAvatar
            }

            @Suppress("UNCHECKED_CAST")
            val rawTags = (doc.get("taggedProducts") as? List<*>) ?: emptyList<Any>()
            val productTagsDeferred = rawTags.map { item ->
                async {
                    (item as? Map<*, *>)?.let { map ->
                        ProductTag(
                            productId = map["productId"] as? String ?: "",
                            thumbnailUrl = StorageUrlResolver.resolve(map["thumbnailRef"] as? String ?: ""),
                            label = map["label"] as? String ?: "",
                        )
                    }
                }
            }

            Post(
                id = doc.id,
                authorId = authorId,
                authorName = authorName,
                authorAvt = authorAvtDeferred.await(),
                caption = caption,
                imageUrls = imageUrlsDeferred.awaitAll().filter { it.isNotBlank() },
                taggedProducts = productTagsDeferred.awaitAll().filterNotNull(),
                likeCount = likeCount,
                createdAt = createdAt,
                commentCount = doc.getLong("commentCount") ?: 0L,
                shareCount = doc.getLong("shareCount") ?: 0L,
                tags = (doc.get("tags") as? List<String>) ?: emptyList(),
                comments = comments
            )
        } catch (_: Exception) {
            null
        }
    }

    fun getLikedPostIdsFlow(userId: String): Flow<Set<String>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }

        val listener = db.collectionGroup("likes")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptySet())
                    return@addSnapshotListener
                }
                val ids = snapshot.documents.mapNotNull { doc ->
                    doc.reference.parent.parent?.id
                }.toSet()
                trySend(ids)
            }

        awaitClose { listener.remove() }
    }

    suspend fun toggleLike(postId: String, userId: String) {
        if (postId.isBlank() || userId.isBlank()) return
        val postRef = db.collection("posts").document(postId)
        val likeRef = postRef.collection("likes").document(userId)

        db.runTransaction { transaction ->
            val likeSnapshot = transaction.get(likeRef)
            if (likeSnapshot.exists()) {
                transaction.delete(likeRef)
                transaction.update(postRef, "likeCount", FieldValue.increment(-1L))
            } else {
                transaction.set(
                    likeRef,
                    mapOf(
                        "userId" to userId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(postRef, "likeCount", FieldValue.increment(1L))
            }
        }.await()
    }

    suspend fun addComment(postId: String, comment: Comment) {
        if (postId.isBlank() || comment.text.isBlank()) return
        val commentRef = db.collection("posts").document(postId)
            .collection("comments")
            .document(comment.id.ifBlank { db.collection("posts").document().id })
        val data = mapOf(
            "id" to commentRef.id,
            "username" to comment.username,
            "avatarUrl" to comment.avatarUrl,
            "text" to comment.text,
            "createdAt" to (comment.createdAt ?: com.google.firebase.Timestamp.now())
        )
        db.runBatch { batch ->
            batch.set(commentRef, data)
            batch.update(
                db.collection("posts").document(postId),
                "commentCount",
                FieldValue.increment(1L)
            )
        }.await()
    }

    private suspend fun getPostComments(postId: String, fallback: List<Comment>): List<Comment> {
        return try {
            val snapshot = db.collection("posts").document(postId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(100)
                .get()
                .await()
            val comments = snapshot.documents.mapNotNull { doc ->
                Comment(
                    id = doc.getString("id").orEmpty().ifBlank { doc.id },
                    username = doc.getString("username").orEmpty(),
                    avatarUrl = doc.getString("avatarUrl").orEmpty(),
                    text = doc.getString("text").orEmpty(),
                    createdAt = doc.getTimestamp("createdAt")
                ).takeIf { it.text.isNotBlank() }
            }
            comments.ifEmpty { fallback }
        } catch (_: Exception) {
            fallback
        }
    }

//    suspend fun addComment(postId: String, comment: Comment) {
//        db.collection("posts").document(postId)
//            .collection("comments").document(comment.id).set(comment).await()
//        db.collection("posts").document(postId)
//            .update("commentCount", FieldValue.increment(1)).await()
//    }

    suspend fun createPost(
        authorId: String,
        caption: String,
        imageUris: List<Uri>,
        fallbackAuthorName: String = "",
        fallbackAuthorAvt: String = ""
    ) {
        if (authorId.isBlank()) error("Missing author")
        if (caption.isBlank() && imageUris.isEmpty()) error("Post needs caption or image")

        val postRef = db.collection("posts").document()
        val author = resolveAuthorPostInfo(
            authorId = authorId,
            fallbackAuthorName = fallbackAuthorName,
            fallbackAuthorAvt = fallbackAuthorAvt
        )
        val imagePaths = imageUris.mapIndexed { index, uri ->
            val path = "posts/${postRef.id}/image_$index.jpg"
            storage.reference.child(path).putFile(uri).await()
            path
        }

        val data = mutableMapOf<String, Any>(
            "authorId" to authorId,
            "authorName" to author.name,
            "authorAvt" to author.avatarRef,
            "caption" to caption.trim(),
            "images" to imagePaths,
            "taggedProducts" to emptyList<Map<String, Any>>(),
            "likeCount" to 0L,
            "commentCount" to 0L,
            "shareCount" to 0L,
            "tags" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        if (author.logoRef.isNotBlank()) {
            data["logoRef"] = author.logoRef
        }

        postRef.set(data).await()
    }

    private suspend fun resolveAuthorPostInfo(
        authorId: String,
        fallbackAuthorName: String,
        fallbackAuthorAvt: String
    ): PostAuthorInfo {
        val shopDoc = runCatching {
            db.collection("shops").document(authorId).get().await()
        }.getOrNull()
        if (shopDoc?.exists() == true) {
            return PostAuthorInfo(
                name = shopDoc.getString("name").orEmpty().ifBlank { fallbackAuthorName.ifBlank { "Shop" } },
                avatarRef = fallbackAuthorAvt,
                logoRef = shopDoc.getString("logoRef").orEmpty()
            )
        }

        val userDoc = runCatching {
            db.collection("users").document(authorId).get().await()
        }.getOrNull()
        return PostAuthorInfo(
            name = userDoc?.getString("name").orEmpty().ifBlank { fallbackAuthorName.ifBlank { "User" } },
            avatarRef = userDoc?.getString("avatarRef").orEmpty().ifBlank { fallbackAuthorAvt },
            logoRef = ""
        )
    }

    private data class PostAuthorInfo(
        val name: String,
        val avatarRef: String,
        val logoRef: String
    )

    fun clearCache() {
        cachedPosts = emptyList()
    }
}
