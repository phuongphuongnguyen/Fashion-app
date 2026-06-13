package com.example.fashionapp.data.search

import com.example.fashionapp.model.Product
import com.example.fashionapp.model.User
import com.example.fashionapp.data.product.toProduct
import com.example.fashionapp.data.StorageUrlResolver
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.text.Normalizer

data class SubCategory(
    val id: String,
    val name: String,
    val previewImageUrl: String = "",
    val parentId: String = "",
)

object SearchRepository {
    private val db = FirebaseFirestore.getInstance()

    // memory cache
    private var cachedProducts: List<Product>? = null
    private var cachedSubCategories: List<SubCategory>? = null
    private var cachedUsers: List<User>? = null

    // Truy vấn dữ liệu toàn bộ sản phẩm từ Firestore và lưu vào cache để giảm số lần đọc DB
    suspend fun getAllProducts(): List<Product> = coroutineScope {
        cachedProducts?.let { return@coroutineScope it }

        try {
            val snap = db.collection("products")
                .limit(200)
                .get().await()

            // Parse + resolve URLs song song trên toàn bộ tài liệu
            val products = snap.documents.map { async { it.toProduct() } }
                .awaitAll()
                .filterNotNull()

            cachedProducts = products
            products
        } catch (_: Exception) { emptyList() }
    }

    // Tìm kiếm từ khoá và lọc
    suspend fun search(query: String, categoryId: String? = null): List<Product> {
        val all = getAllProducts()
        val tokens = normalize(query).split(WHITESPACE).filter { it.isNotBlank() }

        return all.filter { product ->
            val matchCategory = categoryId.isNullOrBlank() ||
                product.categoryId == categoryId ||
                product.categoryId.startsWith(categoryId)

            // Mọi token đều phải xuất hiện trong tên/tag/desc/màu (AND logic, đã bỏ dấu)
            val haystack = buildSearchHaystack(product)
            val matchQuery = tokens.isEmpty() || tokens.all { haystack.contains(it) }

            matchCategory && matchQuery
        }
    }

    // truy vấn user
    suspend fun getAllUsers(): List<User> = coroutineScope {
        cachedUsers?.let { return@coroutineScope it }

        try {
            val snap = db.collection("users")
                .limit(200)
                .get().await()

            val users = snap.documents.map { doc ->
                async {
                    // Tên hiển thị có thể nằm ở 'name', 'displayName' hoặc 'username' tuỳ doc
                    val username = doc.getString("username").orEmpty()
                    val name = (username.takeIf { it.isNotBlank() }
                        ?: doc.getString("name")
                        ?: doc.getString("displayName"))
                        .orEmpty()
                    if (name.isBlank()) return@async null
                    User(
                        id = doc.id,
                        name = name,
                        avatarRef = doc.getString("avatarRef").orEmpty(),
                        avatarUrl = StorageUrlResolver.resolve(
                            doc.getString("avatarRef").orEmpty()
                        ),
                        email = doc.getString("email").orEmpty(),
                        followersCount = (doc.getLong("followersCount") ?: doc.getLong("followerCount"))?.toInt() ?: 0,
                        followingCount = doc.getLong("followingCount")?.toInt() ?: 0,
                        username = username,
                    )
                }
            }.awaitAll().filterNotNull()

            cachedUsers = users
            users
        } catch (_: Exception) { emptyList() }
    }

    // Tìm kiếm tài khoản người dùng theo tên sau khi chuẩn hóa chuỗi không dấu
    suspend fun searchUsers(query: String): List<User> {
        val tokens = normalize(query).split(WHITESPACE).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()

        val all = getAllUsers()
        return all.filter { user ->
            val haystack = normalize(user.name)
            tokens.all { haystack.contains(it) }
        }
    }

    private fun buildSearchHaystack(product: Product): String {
        val parts = buildList {
            add(product.name)
            add(product.description)
            addAll(product.tags)
            product.variants.forEach { add(it.color) }
        }
        return normalize(parts.joinToString(" "))
    }

    // Bỏ dấu tiếng Việt + lowercase
    private fun normalize(input: String): String {
        if (input.isBlank()) return ""
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        return decomposed
            .replace(COMBINING_MARKS, "")
            .replace('đ', 'd').replace('Đ', 'd')
            .lowercase()
            .trim()
    }

    private val WHITESPACE = "\\s+".toRegex()
    private val COMBINING_MARKS = "\\p{Mn}+".toRegex()

    // Xóa sạch bộ nhớ đệm
    fun clearCache() {
        cachedProducts = null
        cachedSubCategories = null
        cachedUsers = null
    }

    // Lấy danh sách các danh mục con phổ biến và tải trước các URL hình ảnh đại diện
    suspend fun getSubCategories(): List<SubCategory> {
        cachedSubCategories?.let { return it }
        val targetIds = listOf("cat001a", "cat001b", "cat001c", "cat002", "cat003", "cat004", "cat005", "cat006", "cat007")
        return try {
            val snap = db.collection("categories")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), targetIds)
                .get().await()

            val docsMap = snap.documents.associateBy { it.id }
            val result = targetIds.mapNotNull { id ->
                val doc = docsMap[id] ?: return@mapNotNull null
                val name = doc.getString("name") ?: return@mapNotNull null
                @Suppress("UNCHECKED_CAST")
                val previews = (doc.get("previewImages") as? List<String>) ?: emptyList()
                val url = StorageUrlResolver.resolve(previews.firstOrNull() ?: "")

                SubCategory(id = id, name = name, previewImageUrl = url, parentId = doc.getString("parentId") ?: "")
            }
            cachedSubCategories = result
            result
        } catch (_: Exception) { emptyList() }
    }
}
