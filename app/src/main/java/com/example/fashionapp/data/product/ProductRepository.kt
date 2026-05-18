package com.example.fashionapp.data.product

import com.example.fashionapp.model.Product
import com.example.fashionapp.model.ProductVariant
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class ProductRepository {
    private val db      = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val urlCache = mutableMapOf<String, String>()

    private suspend fun resolveUrl(path: String): String {
        if (path.isBlank()) return ""
        urlCache[path]?.let { return it }
        return try {
            val url = storage.reference.child(path).downloadUrl.await().toString()
            urlCache[path] = url
            url
        } catch (_: Exception) { "" }
    }

    // ── Lấy 1 product theo ID ─────────────────────────────────────────────
    suspend fun getProductById(productId: String): Product? {
        return try {
            val doc = db.collection("products").document(productId).get().await()
            if (!doc.exists()) return null
            parseProduct(doc)
        } catch (_: Exception) { null }
    }

    // ── Lấy most popular (cho section cuối ProductDetail) ─────────────────
    suspend fun getMostPopular(excludeId: String, limit: Int = 8): List<Product> {
        return try {
            val snap = db.collection("products")
                .orderBy("soldCount", Query.Direction.DESCENDING)
                .limit((limit + 1).toLong())
                .get().await()

            snap.documents
                .filter { it.id != excludeId }
                .take(limit)
                .mapNotNull { parseProduct(it) }
        } catch (_: Exception) { emptyList() }
    }

    // ── Parse Firestore document → Product ────────────────────────────────
    private suspend fun parseProduct(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ): Product? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val imagePaths = (doc.get("images") as? List<String>) ?: emptyList()
            val imageUrls  = imagePaths.map { resolveUrl(it) }

            // Variants
            @Suppress("UNCHECKED_CAST")
            val rawVariants = (doc.get("variants") as? List<*>) ?: emptyList<Any>()
            val variants = rawVariants.mapNotNull { item ->
                (item as? Map<*, *>)?.let { m ->
                    ProductVariant(
                        id             = m["id"]             as? String ?: "",
                        size           = m["size"]           as? String ?: "",
                        color          = m["color"]          as? String ?: "",
                        colorHex       = m["colorHex"]       as? String ?: "#888888",
                        stock          = (m["stock"]         as? Long)?.toInt() ?: 0,
                        additionalPrice= (m["additionalPrice"]as? Double) ?: 0.0,
                    )
                }
            }

            // Specifications — nếu không có field hoặc không có "Xuất xứ" → default VN
            @Suppress("UNCHECKED_CAST")
            val rawSpecs = (doc.get("specifications") as? Map<String, String>) ?: emptyMap()
            val specs = rawSpecs.toMutableMap()
            if (!specs.containsKey("Xuất xứ")) {
                specs["Xuất xứ"] = "Việt Nam"
            }

            val discount = (doc.getLong("discountPercent") ?: 0L).toInt()

            Product(
                id             = doc.id,
                name           = doc.getString("name") ?: "",
                price          = (doc.getLong("price") ?: 0L).toDouble(),
                originalPrice  = (doc.getLong("originalPrice") ?: 0L).toDouble(),
                discountPercent= discount,
                imageUrl       = imageUrls.firstOrNull() ?: "",
                imageUrls      = imageUrls,
                rating         = (doc.getDouble("rating") ?: 0.0).toFloat(),
                reviewCount    = (doc.getLong("reviewCount") ?: 0L).toInt(),
                soldCount      = (doc.getLong("soldCount") ?: 0L).toInt(),
                isSale         = discount > 0,
                shopId         = doc.getString("shopId") ?: "",
                categoryId     = doc.getString("categoryId") ?: "",
                description    = doc.getString("description") ?: "",
                variants       = variants,
                stock          = (doc.getLong("stock") ?: 0L).toInt(),
                freeShipping   = doc.getBoolean("freeShipping") ?: false,
                tags           = @Suppress("UNCHECKED_CAST")
                                 (doc.get("tags") as? List<String>) ?: emptyList(),
                specifications = specs,
            )
        } catch (_: Exception) { null }
    }
}
