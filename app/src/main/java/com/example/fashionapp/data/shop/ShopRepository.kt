package com.example.fashionapp.data.shop

import com.example.fashionapp.model.Category
import com.example.fashionapp.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

// ── Repository for Shop ───────────────────────────────────────
class ShopRepository {
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

    // ── Categories từ Firestore ───────────────────────────────────────────────
    suspend fun getCategories(): List<Category> {
        val targetIds = listOf(
            "cat001a", // áo thun
            "cat002",  // quần
            "cat003",  // váy đầm
            "cat004",  // giày dép
            "cat005",  // túi xách
            "cat006"   // phụ kiện
        )
        return try {
            val snap = db.collection("categories")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), targetIds)
                .get()
                .await()

            val docsMap = snap.documents.associateBy { it.id }

            // Duyệt theo đúng thứ tự targetIds để hiển thị đúng
            targetIds.mapNotNull { id ->
                val doc = docsMap[id] ?: return@mapNotNull null
                val name = doc.getString("name") ?: return@mapNotNull null
                @Suppress("UNCHECKED_CAST")
                val previewPaths = (doc.get("previewImages") as? List<String>) ?: emptyList()

                val urls = previewPaths.map { path -> resolveUrl(path) }
                Category(id, name, urls)
            }
        } catch (_: Exception) { emptyList() }
    }

    // ── Products mới nhất ─────────────────────────────────────────────────────
    suspend fun getNewItems(): List<Product> = fetchProducts(
        db.collection("products")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(8)
    )

    // ── Bán chạy nhất ────────────────────────────────────────────────────────
    suspend fun getMostPopular(): List<Product> = fetchProducts(
        db.collection("products")
            .orderBy("soldCount", Query.Direction.DESCENDING)
            .limit(8)
    )

    // ── Just For You (tất cả, xáo trộn phía client) ──────────────────────────
    suspend fun getForYou(): List<Product> = fetchProducts(
        db.collection("products").limit(6)
    )

    // ── Helper chung ──────────────────────────────────────────────────────────
    private suspend fun fetchProducts(query: Query): List<Product> {
        return try {
            val snap = query.get().await()
            snap.documents.mapNotNull { doc ->
                @Suppress("UNCHECKED_CAST")
                val images     = (doc.get("images") as? List<String>) ?: emptyList()
                val firstPath  = images.firstOrNull() ?: ""
                val discount   = doc.getLong("discountPercent") ?: 0L

                Product(
                    id        = doc.id,
                    name      = doc.getString("name") ?: "",
                    price     = (doc.getLong("price") ?: 0L).toDouble(),
                    imageUrl  = resolveUrl(firstPath),
                    rating    = (doc.getDouble("rating") ?: 0.0).toFloat(),
                    soldCount = (doc.getLong("soldCount") ?: 0L).toInt(),
                    isSale    = discount > 0
                )
            }
        } catch (_: Exception) { emptyList() }
    }
}