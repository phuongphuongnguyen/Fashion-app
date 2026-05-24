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

object SearchRepository {
    private val db      = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val urlCache = mutableMapOf<String, String>()

    // ── In-Memory Cache ──────────────────────────────────────────────────────
    private var cachedProducts: List<Product>? = null
    private var cachedSubCategories: List<SubCategory>? = null

    private suspend fun resolveUrl(path: String): String {
        if (path.isBlank()) return ""
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        urlCache[path]?.let { return it }
        return try {
            val url = storage.reference.child(path).downloadUrl.await().toString()
            urlCache[path] = url
            url
        } catch (_: Exception) { path }
    }

    private fun safeDouble(value: Any?): Double = (value as? Number)?.toDouble() ?: 0.0
    private fun safeInt(value: Any?): Int = (value as? Number)?.toInt() ?: 0

    suspend fun getAllProducts(): List<Product> {
        cachedProducts?.let { return it }

        return try {
            val snap = db.collection("products")
                .limit(200)
                .get().await()

            val products = snap.documents.mapNotNull { doc ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    val imagePaths = (doc.get("images") as? List<String>) ?: emptyList()
                    val imageUrls = imagePaths.map { resolveUrl(it) }
                    
                    @Suppress("UNCHECKED_CAST")
                    val rawVariants = (doc.get("variants") as? List<*>) ?: emptyList<Any>()
                    val variants = rawVariants.mapNotNull { item ->
                        (item as? Map<*, *>)?.let { m ->
                            ProductVariant(
                                id = m["id"] as? String ?: "",
                                size = m["size"] as? String ?: "",
                                color = m["color"] as? String ?: "",
                                colorHex = m["colorHex"] as? String ?: "#888888",
                                stock = safeInt(m["stock"]),
                                additionalPrice = safeDouble(m["additionalPrice"])
                            )
                        }
                    }

                    @Suppress("UNCHECKED_CAST")
                    val specs = (doc.get("specifications") as? Map<String, String>) ?: emptyMap()
                    val discount = safeInt(doc.get("discountPercent"))
                    val price = safeDouble(doc.get("price"))
                    val originalPrice = safeDouble(doc.get("originalPrice"))

                    Product(
                        id = doc.id,
                        name = doc.getString("name").orEmpty(),
                        price = price,
                        originalPrice = originalPrice,
                        discountPercent = discount,
                        imageUrl = imageUrls.firstOrNull() ?: resolveUrl(doc.getString("imageUrl").orEmpty()),
                        imageUrls = imageUrls,
                        rating = (doc.get("rating") as? Number)?.toFloat() ?: 0f,
                        reviewCount = safeInt(doc.get("reviewCount")),
                        soldCount = safeInt(doc.get("soldCount")),
                        isSale = discount > 0 || originalPrice > price,
                        shopId = doc.getString("shopId").orEmpty(),
                        categoryId = doc.getString("categoryId").orEmpty(),
                        description = doc.getString("description").orEmpty(),
                        variants = variants,
                        stock = safeInt(doc.get("stock")),
                        freeShipping = doc.getBoolean("freeShipping") ?: false,
                        tags = @Suppress("UNCHECKED_CAST") (doc.get("tags") as? List<String>) ?: emptyList(),
                        specifications = specs
                    )
                } catch (_: Exception) { null }
            }

            cachedProducts = products
            products
        } catch (_: Exception) { emptyList() }
    }

    suspend fun search(query: String, categoryId: String? = null): List<Product> {
        val all = getAllProducts()
        val q   = query.trim().lowercase()

        return all.filter { product ->
            val matchCategory = categoryId.isNullOrBlank() || product.categoryId == categoryId
            val matchQuery = q.isEmpty()
                || product.name.lowercase().contains(q)
                || product.tags.any { it.lowercase().contains(q) }
                || product.description.lowercase().contains(q)
                || product.variants.any { it.color.lowercase().contains(q) }

            matchCategory && matchQuery
        }
    }

    fun clearCache() {
        cachedProducts = null
        cachedSubCategories = null
    }

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
                val url = resolveUrl(previews.firstOrNull() ?: "")

                SubCategory(id = id, name = name, previewImageUrl = url, parentId = doc.getString("parentId") ?: "")
            }
            cachedSubCategories = result
            result
        } catch (_: Exception) { emptyList() }
    }
}
