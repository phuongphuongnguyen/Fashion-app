package com.example.fashionapp.data.search

import com.example.fashionapp.model.Product
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

    // ── In-Memory Cache ──────────────────────────────────────────────────────
    private var cachedProducts: List<Product>? = null
    private var cachedSubCategories: List<SubCategory>? = null

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

    private fun buildSearchHaystack(product: Product): String {
        val parts = buildList {
            add(product.name)
            add(product.description)
            addAll(product.tags)
            product.variants.forEach { add(it.color) }
        }
        return normalize(parts.joinToString(" "))
    }

    // Bỏ dấu tiếng Việt + lowercase. Đảm bảo "ao so mi" match được "Áo sơ mi".
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
                val url = StorageUrlResolver.resolve(previews.firstOrNull() ?: "")

                SubCategory(id = id, name = name, previewImageUrl = url, parentId = doc.getString("parentId") ?: "")
            }
            cachedSubCategories = result
            result
        } catch (_: Exception) { emptyList() }
    }
}
