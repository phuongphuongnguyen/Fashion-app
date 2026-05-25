package com.example.fashionapp.data.search

import com.example.fashionapp.model.Product
import com.example.fashionapp.data.product.toProduct
import com.example.fashionapp.data.StorageUrlResolver
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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

    suspend fun getAllProducts(): List<Product> {
        cachedProducts?.let { return it }

        return try {
            val snap = db.collection("products")
                .limit(200)
                .get().await()

            val products = snap.documents.mapNotNull { it.toProduct() }

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
                val url = StorageUrlResolver.resolve(previews.firstOrNull() ?: "")

                SubCategory(id = id, name = name, previewImageUrl = url, parentId = doc.getString("parentId") ?: "")
            }
            cachedSubCategories = result
            result
        } catch (_: Exception) { emptyList() }
    }
}
