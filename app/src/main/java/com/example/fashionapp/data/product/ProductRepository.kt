package com.example.fashionapp.data.product

import com.example.fashionapp.model.Product
import com.example.fashionapp.model.ProductVariant
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

object ProductRepository {
    private val db      = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val urlCache = mutableMapOf<String, String>()

    // ── In-Memory Cache ──────────────────────────────────────────────────────
    private val productCache = mutableMapOf<String, Product>()
    private var mostPopularCache: List<Product>? = null

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

    suspend fun getProductById(productId: String): Product? {
        // Trả về từ cache nếu đã xem sản phẩm này trước đó
        productCache[productId]?.let { return it }

        return try {
            val doc = db.collection("products").document(productId).get().await()
            if (!doc.exists()) return null
            val product = parseProduct(doc)
            if (product != null) productCache[productId] = product
            product
        } catch (_: Exception) { null }
    }

    suspend fun getMostPopular(excludeId: String, limit: Int = 8): List<Product> {
        // Cache danh sách phổ biến
        mostPopularCache?.let { list ->
            return list.filter { it.id != excludeId }.take(limit)
        }

        return try {
            val snap = db.collection("products")
                .orderBy("soldCount", Query.Direction.DESCENDING)
                .limit((limit + 1).toLong())
                .get().await()

            val results = snap.documents.mapNotNull { parseProduct(it) }
            mostPopularCache = results
            results.filter { it.id != excludeId }.take(limit)
        } catch (_: Exception) { emptyList() }
    }

    private suspend fun parseProduct(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ): Product? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val imagePaths = (doc.get("images") as? List<String>) ?: emptyList()
            val imageUrls  = imagePaths.map { resolveUrl(it) }

            @Suppress("UNCHECKED_CAST")
            val rawVariants = (doc.get("variants") as? List<*>) ?: emptyList<Any>()
            val variants = rawVariants.mapNotNull { item ->
                (item as? Map<*, *>)?.let { m ->
                    ProductVariant(
                        id              = m["id"]              as? String ?: "",
                        size            = m["size"]            as? String ?: "",
                        color           = m["color"]           as? String ?: "",
                        colorHex        = m["colorHex"]        as? String ?: "#888888",
                        stock           = safeInt(m["stock"]),
                        additionalPrice = safeDouble(m["additionalPrice"]),
                    )
                }
            }

            @Suppress("UNCHECKED_CAST")
            val rawSpecs = (doc.get("specifications") as? Map<String, String>) ?: emptyMap()
            val specs = rawSpecs.toMutableMap()
            if (!specs.containsKey("Xuất xứ")) {
                specs["Xuất xứ"] = "Việt Nam"
            }

            val discount = safeInt(doc.get("discountPercent"))
            val price = safeDouble(doc.get("price"))
            val originalPrice = safeDouble(doc.get("originalPrice"))

            Product(
                id              = doc.id,
                name            = doc.getString("name").orEmpty(),
                price           = price,
                originalPrice   = originalPrice,
                discountPercent = discount,
                imageUrl        = imageUrls.firstOrNull() ?: resolveUrl(doc.getString("imageUrl").orEmpty()),
                imageUrls       = imageUrls,
                rating          = (doc.get("rating") as? Number)?.toFloat() ?: 0f,
                reviewCount     = safeInt(doc.get("reviewCount")),
                soldCount       = safeInt(doc.get("soldCount")),
                isSale          = discount > 0 || originalPrice > price,
                shopId          = doc.getString("shopId").orEmpty(),
                categoryId      = doc.getString("categoryId").orEmpty(),
                description     = doc.getString("description").orEmpty(),
                variants        = variants,
                stock           = safeInt(doc.get("stock")),
                freeShipping    = doc.getBoolean("freeShipping") ?: false,
                tags            = @Suppress("UNCHECKED_CAST") (doc.get("tags") as? List<String>) ?: emptyList(),
                specifications  = specs,
            )
        } catch (_: Exception) { null }
    }

    fun clearCache() {
        productCache.clear()
        mostPopularCache = null
    }
}
