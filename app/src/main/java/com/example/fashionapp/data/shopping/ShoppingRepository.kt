package com.example.fashionapp.data.shopping

import com.example.fashionapp.model.Category
import com.example.fashionapp.model.Product
import com.example.fashionapp.model.ProductVariant
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

object ShoppingRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val urlCache = mutableMapOf<String, String>()

    // ── In-Memory Cache ──────────────────────────────────────────────────────
    private var categoriesCache: List<Category>? = null
    private var newItemsCache: List<Product>? = null
    private var mostPopularCache: List<Product>? = null
    private var forYouCache: List<Product>? = null

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

    suspend fun getCategories(): List<Category> {
        categoriesCache?.let { return it }
        val targetIds = listOf("cat001a", "cat002", "cat003", "cat004", "cat005", "cat006")
        return try {
            val snap = db.collection("categories")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), targetIds)
                .get().await()

            val docsMap = snap.documents.associateBy { it.id }
            val result = targetIds.mapNotNull { id ->
                val doc = docsMap[id] ?: return@mapNotNull null
                val name = doc.getString("name") ?: return@mapNotNull null
                @Suppress("UNCHECKED_CAST")
                val previewPaths = (doc.get("previewImages") as? List<String>) ?: emptyList()
                val urls = previewPaths.map { resolveUrl(it) }
                Category(id, name, urls)
            }
            categoriesCache = result
            result
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getNewItems(): List<Product> {
        newItemsCache?.let { return it }
        val result = fetchProducts(
            db.collection("products").orderBy("createdAt", Query.Direction.DESCENDING).limit(8)
        )
        newItemsCache = result
        return result
    }

    suspend fun getMostPopular(): List<Product> {
        mostPopularCache?.let { return it }
        val result = fetchProducts(
            db.collection("products").orderBy("soldCount", Query.Direction.DESCENDING).limit(8)
        )
        mostPopularCache = result
        return result
    }

    suspend fun getForYou(): List<Product> {
        forYouCache?.let { return it }
        val result = fetchProducts(
            db.collection("products").limit(6)
        )
        forYouCache = result
        return result
    }

    fun clearCache() {
        categoriesCache = null
        newItemsCache = null
        mostPopularCache = null
        forYouCache = null
    }

    private suspend fun fetchProducts(query: Query): List<Product> {
        return try {
            val snap = query.get().await()
            snap.documents.mapNotNull { doc ->
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
            }
        } catch (_: Exception) { emptyList() }
    }
}
