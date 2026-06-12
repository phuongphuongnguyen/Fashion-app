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
        val avatarRef = snapshot.getString("avatarRef").orEmpty()
        return User(
            id = snapshot.id,
            name = name,
            avatarRef = avatarRef,
            avatarUrl = StorageUrlResolver.resolve(avatarRef),
            email = snapshot.getString("email") ?: "",
            followersCount = (snapshot.getLong("followersCount") ?: snapshot.getLong("followerCount"))?.toInt() ?: 0,
            followingCount = snapshot.getLong("followingCount")?.toInt() ?: 0,
            role = snapshot.getString("role").orEmpty(),
            shopId = snapshot.getString("shopId").orEmpty(),
            username = snapshot.getString("username").orEmpty(),
            phoneNumber = (snapshot.getString("phoneNumber") ?: snapshot.getString("phone")).orEmpty(),
            address = (snapshot.getString("address")
                ?: snapshot.getString("shippingAddress")
                ?: snapshot.getString("location")).orEmpty(),
            bio = (snapshot.getString("bio") ?: snapshot.getString("description")).orEmpty()
        )
    }

    // Lấy dữ liệu hồ sơ cá nhân của người dùng theo luồng dữ liệu thời gian thực (Flow)
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

    // Lấy thông tin chi tiết hồ sơ cá nhân của người dùng một lần duy nhất từ Firestore
    suspend fun getUserProfile(userId: String): User? {
        if (userId.isBlank()) return null
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            if (!snapshot.exists()) return null
            userFromSnapshot(snapshot)
        } catch (_: Exception) { null }
    }

    // Cập nhật thông tin giới thiệu bản thân (bio) của người dùng vào Firestore
    suspend fun updateUserBio(userId: String, bio: String) {
        if (userId.isBlank()) return
        db.collection("users").document(userId)
            .update(
                mapOf(
                    "bio" to bio.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
    }

    // Tìm kiếm thông tin người dùng dựa trên ID của cửa hàng (shopId) mà họ sở hữu
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
    // Kiểm tra trạng thái theo dõi thời gian thực giữa hai người dùng (Flow)
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

    // Thiết lập hoặc hủy trạng thái theo dõi và cập nhật số lượng người theo dõi giữa hai tài khoản
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

    // Khởi tạo thông tin hồ sơ mặc định và ảnh đại diện ngẫu nhiên khi người dùng đăng ký mới
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

    // Lấy danh sách ID các bài viết đã lưu của người dùng theo thời gian thực (Flow)
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

    // Lưu bài viết yêu thích của người dùng vào bộ sưu tập cá nhân
    suspend fun savePost(userId: String, postId: String) {
        if (userId.isBlank() || postId.isBlank()) return
        val data = hashMapOf("savedAt" to System.currentTimeMillis())
        db.collection("users").document(userId).collection("saved_posts").document(postId).set(data).await()
    }

    // Bỏ lưu bài viết yêu thích của người dùng khỏi bộ sưu tập cá nhân
    suspend fun unsavePost(userId: String, postId: String) {
        if (userId.isBlank() || postId.isBlank()) return
        db.collection("users").document(userId).collection("saved_posts").document(postId).delete().await()
    }
}
