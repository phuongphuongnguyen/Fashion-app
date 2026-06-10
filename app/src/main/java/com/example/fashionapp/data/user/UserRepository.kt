package com.example.fashionapp.data.user

import com.example.fashionapp.data.StorageUrlResolver
import com.example.fashionapp.model.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    // Map 1 document users/{id} -> User, đọc field theo nhiều tên có thể có (data seed lệch field).
    private suspend fun userFromSnapshot(snapshot: DocumentSnapshot): User {
        val name = (snapshot.getString("name")
            ?: snapshot.getString("displayName")
            ?: snapshot.getString("username"))
            .orEmpty()
        return User(
            id = snapshot.id,
            name = name,
            avatarUrl = StorageUrlResolver.resolve(
                snapshot.getString("avatarRef").orEmpty()
            ),
            email = snapshot.getString("email") ?: "",
            followersCount = (snapshot.getLong("followersCount") ?: snapshot.getLong("followerCount"))?.toInt() ?: 0,
            followingCount = snapshot.getLong("followingCount")?.toInt() ?: 0,
            role = snapshot.getString("role").orEmpty(),
            shopId = snapshot.getString("shopId").orEmpty(),
            username = snapshot.getString("username").orEmpty(),
            phoneNumber = (snapshot.getString("phoneNumber") ?: snapshot.getString("phone")).orEmpty(),
            address = (snapshot.getString("address")
                ?: snapshot.getString("shippingAddress")
                ?: snapshot.getString("location")).orEmpty()
        )
    }

    fun getUserProfileFlow(userId: String): Flow<User?> = callbackFlow {
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

                launch {
                    trySend(userFromSnapshot(snapshot))
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getUserProfile(userId: String): User? {
        if (userId.isBlank()) return null
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            if (!snapshot.exists()) return null
            userFromSnapshot(snapshot)
        } catch (_: Exception) { null }
    }

    // Tìm user là chủ của shop theo shopId (users/{uid} có field shopId == shopId).
    suspend fun findUserByShopId(shopId: String): User? {
        if (shopId.isBlank()) return null
        return try {
            val snap = db.collection("users")
                .whereEqualTo("shopId", shopId)
                .limit(1)
                .get().await()
            snap.documents.firstOrNull()?.let { userFromSnapshot(it) }
        } catch (_: Exception) { null }
    }

    // ── Follow ──────────────────────────────────────────────────────────────
    // currentUid có đang theo dõi targetUid không (realtime).
    fun isFollowingFlow(currentUid: String, targetUid: String): Flow<Boolean> = callbackFlow {
        if (currentUid.isBlank() || targetUid.isBlank() || currentUid == targetUid) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val ref = db.collection("users").document(currentUid)
            .collection("following").document(targetUid)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            trySend(snapshot?.exists() == true)
        }
        awaitClose { listener.remove() }
    }

    // Bật/tắt theo dõi. Ghi 2 chiều (following của mình + followers của target) và
    // cập nhật followerCount/followingCount bằng transaction để không lệch số.
    suspend fun setFollowing(currentUid: String, targetUid: String, follow: Boolean) {
        if (currentUid.isBlank() || targetUid.isBlank() || currentUid == targetUid) return

        val meRef = db.collection("users").document(currentUid)
        val targetRef = db.collection("users").document(targetUid)
        val followingRef = meRef.collection("following").document(targetUid)
        val followerRef = targetRef.collection("followers").document(currentUid)

        db.runTransaction { tx ->
            val alreadyFollowing = tx.get(followingRef).exists()
            when {
                follow && !alreadyFollowing -> {
                    val data = hashMapOf("createdAt" to FieldValue.serverTimestamp())
                    tx.set(followingRef, data)
                    tx.set(followerRef, data)
                    tx.update(meRef, "followingCount", FieldValue.increment(1))
                    tx.update(targetRef, "followerCount", FieldValue.increment(1))
                }
                !follow && alreadyFollowing -> {
                    tx.delete(followingRef)
                    tx.delete(followerRef)
                    tx.update(meRef, "followingCount", FieldValue.increment(-1))
                    tx.update(targetRef, "followerCount", FieldValue.increment(-1))
                }
            }
            null
        }.await()
    }

    suspend fun seedUserProfile(userId: String, email: String) {
        if (userId.isBlank()) return
        val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        // Tạo avatar ngẫu nhiên dựa trên UID để mỗi user có 1 ảnh khác nhau nếu chưa có
        val randomSeed = userId.take(5)
        val avatarUrl = "https://api.dicebear.com/7.x/avataaars/svg?seed=$randomSeed"
        
        val data = hashMapOf(
            "name" to name,
            "avatarRef" to avatarUrl,
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
