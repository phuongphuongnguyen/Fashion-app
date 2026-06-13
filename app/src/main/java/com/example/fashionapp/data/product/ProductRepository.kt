package com.example.fashionapp.data.product

import com.example.fashionapp.model.Product
import com.example.fashionapp.model.ProductVariant
import com.example.fashionapp.data.StorageUrlResolver
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import kotlinx.coroutines.tasks.await

object ProductRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // memory cache
    private val productCache = mutableMapOf<String, Product>()
    private var mostPopularCache: List<Product>? = null

    private fun safeDouble(value: Any?): Double = (value as? Number)?.toDouble() ?: 0.0
    private fun safeInt(value: Any?): Int = (value as? Number)?.toInt() ?: 0

    suspend fun getProductById(productId: String): Product? {
        productCache[productId]?.let { return it }

        return try {
            val doc = db.collection("products").document(productId).get().await()
            if (!doc.exists()) return null
            val product = doc.toProduct()
            if (product != null) productCache[productId] = product
            product
        } catch (_: Exception) { null }
    }

    suspend fun getMostPopular(excludeId: String, limit: Int = 8): List<Product> {
        mostPopularCache?.let { list ->
            return list.filter { it.id != excludeId }.take(limit)
        }

        return try {
            val snap = db.collection("products")
                .orderBy("soldCount", Query.Direction.DESCENDING)
                .limit((limit + 1).toLong())
                .get().await()

            val results = snap.documents.mapNotNull { it.toProduct() }
            mostPopularCache = results
            results.filter { it.id != excludeId }.take(limit)
        } catch (_: Exception) { emptyList() }
    }

    suspend fun parseProduct(doc: DocumentSnapshot): Product? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val imagePaths = (doc.get("images") as? List<String>) ?: emptyList()
            val imageUrls  = StorageUrlResolver.resolveAll(imagePaths)

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
                imageUrl        = imageUrls.firstOrNull()
                    ?: StorageUrlResolver.resolve(doc.getString("imageUrl").orEmpty()).takeIf { it.isNotBlank() }
                    ?: "",
                imageUrls       = imageUrls,
                rating          = ((doc.get("rating") as? Number)?.toDouble() ?: 0.0).let {
                    Math.round(it * 10) / 10.0
                }.toFloat(),
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
                createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time
                    ?: (doc.get("createdAtMillis") as? Number)?.toLong()
                    ?: (doc.get("updatedAtMillis") as? Number)?.toLong()
                    ?: 0L,
                specifications  = specs,
            )
        } catch (_: Exception) { null }
    }

    suspend fun createProduct(
        shopId: String,
        name: String,
        description: String,
        price: Double,
        stock: Int,
        categoryId: String,
        imageUris: List<Uri>,
        variants: List<ProductVariant>
    ) {
        if (shopId.isBlank()) error("Missing shopId")
        if (name.isBlank()) error("Missing product name")

        val productRef = db.collection("products").document()
        
        // Upload images to Storage
        val imagePaths = imageUris.mapIndexed { index, uri ->
            val path = "products/${productRef.id}/image_$index.jpg"
            storage.reference.child(path).putFile(uri).await()
            path
        }

        val data = mutableMapOf<String, Any>(
            "shopId" to shopId,
            "name" to name.trim(),
            "description" to description.trim(),
            "price" to price,
            "originalPrice" to price,
            "discountPercent" to 0,
            "stock" to stock,
            "categoryId" to categoryId,
            "images" to imagePaths,
            "imageUrl" to (imagePaths.firstOrNull() ?: ""),
            "rating" to 0.0,
            "reviewCount" to 0,
            "soldCount" to 0,
            "revenue" to 0.0,
            "freeShipping" to true,
            "isActive" to true,
            "tags" to emptyList<String>(),
            "specifications" to mapOf(
                "Xuất xứ" to "Việt Nam",
                "Chất liệu" to "Cotton"
            ),
            "variants" to variants.map { v ->
                mapOf(
                    "id" to v.id.ifBlank { "v_${System.currentTimeMillis()}_${v.size}" },
                    "size" to v.size,
                    "color" to v.color,
                    "colorHex" to v.colorHex,
                    "stock" to v.stock,
                    "additionalPrice" to v.additionalPrice
                )
            },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        productRef.set(data).await()
    }

    suspend fun updateProductPrice(productId: String, newPrice: Double) {
        db.collection("products").document(productId).update(
            "price", newPrice,
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
        productCache.remove(productId)
    }

    suspend fun updateProductStock(productId: String, variants: List<ProductVariant>) {
        val totalStock = variants.sumOf { it.stock }
        val variantData = variants.map { v ->
            mapOf(
                "id" to v.id,
                "size" to v.size,
                "color" to v.color,
                "colorHex" to v.colorHex,
                "stock" to v.stock,
                "additionalPrice" to v.additionalPrice
            )
        }
        db.collection("products").document(productId).update(
            "stock", totalStock,
            "variants", variantData,
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
        productCache.remove(productId)
    }

    fun clearCache() {
        productCache.clear()
        mostPopularCache = null
    }
}

suspend fun DocumentSnapshot.toProduct(): Product? = ProductRepository.parseProduct(this)
