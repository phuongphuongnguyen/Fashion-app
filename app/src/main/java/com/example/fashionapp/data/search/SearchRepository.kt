package com.example.fashionapp.data.search

import com.example.fashionapp.model.Product
import com.example.fashionapp.model.ProductVariant
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

data class SubCategory(
    val id: String,
    val name: String,
    val previewImageUrl: String = "",
    val parentId: String = "",
)

class SearchRepository {
    private val db      = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val urlCache = mutableMapOf<String, String>()

    // ── Cache toàn bộ products — chỉ fetch Firestore 1 lần ───────────────
    private var cachedProducts: List<Product>? = null

    private suspend fun resolveUrl(path: String): String {
        if (path.isBlank()) return ""
        urlCache[path]?.let { return it }
        return try {
            val url = storage.reference.child(path).downloadUrl.await().toString()
            urlCache[path] = url
            url
        } catch (_: Exception) { "" }
    }

    // ── Lấy toàn bộ products (có cache) ──────────────────────────────────
    suspend fun getAllProducts(): List<Product> {
        // Nếu đã cache rồi thì trả về luôn, không gọi Firestore nữa
        cachedProducts?.let { return it }

        return try {
            val snap = db.collection("products")
                .whereEqualTo("isActive", true)
                .limit(200) // đủ cho toàn bộ catalog
                .get().await()

            val products = snap.documents.mapNotNull { doc ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    val imagePaths = (doc.get("images") as? List<String>) ?: emptyList()
                    val firstUrl   = resolveUrl(imagePaths.firstOrNull() ?: "")
                    val discount   = (doc.getLong("discountPercent") ?: 0L).toInt()

                    @Suppress("UNCHECKED_CAST")
                    val rawVariants = (doc.get("variants") as? List<*>) ?: emptyList<Any>()
                    val variants = rawVariants.mapNotNull { item ->
                        (item as? Map<*, *>)?.let { m ->
                            ProductVariant(
                                id       = m["id"]       as? String ?: "",
                                size     = m["size"]     as? String ?: "",
                                color    = m["color"]    as? String ?: "",
                                colorHex = m["colorHex"] as? String ?: "#888888",
                                stock    = (m["stock"]   as? Long)?.toInt() ?: 0,
                            )
                        }
                    }

                    Product(
                        id              = doc.id,
                        name            = doc.getString("name") ?: "",
                        price           = (doc.getLong("price") ?: 0L).toDouble(),
                        originalPrice   = (doc.getLong("originalPrice") ?: 0L).toDouble(),
                        discountPercent = discount,
                        imageUrl        = firstUrl,
                        rating          = (doc.getDouble("rating") ?: 0.0).toFloat(),
                        reviewCount     = (doc.getLong("reviewCount") ?: 0L).toInt(),
                        soldCount       = (doc.getLong("soldCount") ?: 0L).toInt(),
                        isSale          = discount > 0,
                        description     = doc.getString("description") ?: "",
                        categoryId      = doc.getString("categoryId") ?: "",
                        variants        = variants,
                        freeShipping    = doc.getBoolean("freeShipping") ?: false,
                        tags            = @Suppress("UNCHECKED_CAST")
                                         (doc.get("tags") as? List<String>) ?: emptyList(),
                    )
                } catch (_: Exception) { null }
            }

            // Lưu vào cache
            cachedProducts = products
            products
        } catch (_: Exception) { emptyList() }
    }

    // ── Search thuần client-side — không gọi Firestore ───────────────────
    suspend fun search(query: String, categoryId: String? = null): List<Product> {
        val all = getAllProducts() // lấy từ cache, không fetch lại
        val q   = query.trim().lowercase()

        return all.filter { product ->
            // Filter category
            val matchCategory = categoryId.isNullOrBlank()
                || product.categoryId == categoryId

            // Filter query — tìm trong name, tags, description
            val matchQuery = q.isEmpty()
                || product.name.lowercase().contains(q)
                || product.tags.any { it.lowercase().contains(q) }
                || product.description.lowercase().contains(q)
                || matchVariantColor(product, q) // tìm theo màu sắc

            matchCategory && matchQuery
        }
    }

    // Tìm theo màu trong variants (vd: gõ "đen" → ra product có variant màu đen)
    private fun matchVariantColor(product: Product, query: String): Boolean {
        return product.variants.any { it.color.lowercase().contains(query) }
    }

    // ── Xóa cache (gọi khi cần refresh data) ─────────────────────────────
    fun clearCache() { cachedProducts = null }

    // ── SubCategories ─────────────────────────────────────────────────────
    suspend fun getSubCategories(): List<SubCategory> {
        val targetIds = listOf(
            "cat001a", "cat001b", "cat001c", "cat002",
            "cat003", "cat004", "cat005", "cat006", "cat007"
        )
        return try {
            val snap = db.collection("categories")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), targetIds)
                .get().await()

            val docsMap = snap.documents.associateBy { it.id }

            targetIds.mapNotNull { id ->
                val doc = docsMap[id] ?: return@mapNotNull null
                val name = doc.getString("name") ?: return@mapNotNull null
                @Suppress("UNCHECKED_CAST")
                val previews = (doc.get("previewImages") as? List<String>) ?: emptyList()
                val url = resolveUrl(previews.firstOrNull() ?: "")

                SubCategory(
                    id              = id,
                    name            = name,
                    previewImageUrl = url,
                    parentId        = doc.getString("parentId") ?: ""
                )
            }
        } catch (_: Exception) { emptyList() }
    }
}
