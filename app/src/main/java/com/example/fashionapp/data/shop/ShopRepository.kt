package com.example.fashionapp.data.shop

import com.example.fashionapp.model.Product
import com.example.fashionapp.model.ProductVariant
import com.example.fashionapp.data.CartItem
import com.example.fashionapp.data.ReviewOrder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object ShopRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val urlCache = mutableMapOf<String, String>()

    // ── In-Memory Cache ──────────────────────────────────────────────────────
    private var cachedProducts: List<Product>? = null
    private var cachedCartItems: List<CartItem>? = null
    private var cachedOrders: List<ReviewOrder>? = null

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

    fun getProductsFlow(): Flow<List<Product>> = callbackFlow {
        val listener = db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(cachedProducts ?: emptyList())
                    return@addSnapshotListener
                }

                launch {
                    val products = snapshot.documents.mapNotNull { doc ->
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
                        } catch (e: Exception) { null }
                    }
                    cachedProducts = products
                    trySend(products)
                }
            }
        awaitClose { listener.remove() }
    }.onStart {
        cachedProducts?.let { emit(it) }
    }

    fun getCartItemsFlow(userId: String): Flow<List<CartItem>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList()); close(); return@callbackFlow
        }
        val listener = db.collection("users").document(userId).collection("cart")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(cachedCartItems ?: emptyList()); return@addSnapshotListener
                }
                launch {
                    val items = snapshot.documents.mapNotNull { doc ->
                        try {
                            val productMap = doc.get("product") as? Map<String, Any> ?: return@mapNotNull null
                            val product = Product(
                                id = productMap["id"] as? String ?: "",
                                name = productMap["name"] as? String ?: "",
                                price = safeDouble(productMap["price"]),
                                imageUrl = resolveUrl(productMap["imageUrl"] as? String ?: ""),
                                rating = (productMap["rating"] as? Number)?.toFloat() ?: 0f,
                                soldCount = (productMap["soldCount"] as? Number)?.toInt() ?: 0
                            )
                            CartItem(
                                id = doc.id,
                                product = product,
                                color = doc.getString("color").orEmpty(),
                                size = doc.getString("size").orEmpty(),
                                quantity = safeInt(doc.get("quantity"))
                            )
                        } catch (e: Exception) { null }
                    }
                    cachedCartItems = items
                    trySend(items)
                }
            }
        awaitClose { listener.remove() }
    }.onStart {
        cachedCartItems?.let { emit(it) }
    }

    suspend fun addToCart(userId: String, cartItem: CartItem) {
        if (userId.isBlank()) return
        val data = hashMapOf(
            "product" to hashMapOf(
                "id" to cartItem.product.id,
                "name" to cartItem.product.name,
                "price" to cartItem.product.price,
                "imageUrl" to cartItem.product.imageUrl,
                "rating" to cartItem.product.rating,
                "soldCount" to cartItem.product.soldCount
            ),
            "color" to cartItem.color,
            "size" to cartItem.size,
            "quantity" to cartItem.quantity
        )
        db.collection("users").document(userId).collection("cart").document(cartItem.id).set(data).await()
    }

    suspend fun updateCartItemQuantity(userId: String, cartItemId: String, newQuantity: Int) {
        if (userId.isBlank()) return
        if (newQuantity <= 0) {
            db.collection("users").document(userId).collection("cart").document(cartItemId).delete().await()
        } else {
            db.collection("users").document(userId).collection("cart").document(cartItemId).update("quantity", newQuantity).await()
        }
    }

    suspend fun clearCart(userId: String, cartItems: List<CartItem>) {
        if (userId.isBlank()) return
        val batch = db.batch()
        val cartRef = db.collection("users").document(userId).collection("cart")
        cartItems.forEach { item -> batch.delete(cartRef.document(item.id)) }
        batch.commit().await()
    }

    fun getOrdersFlow(userId: String): Flow<List<ReviewOrder>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList()); close(); return@callbackFlow
        }
        val listener = db.collection("users").document(userId).collection("orders")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(cachedOrders ?: emptyList()); return@addSnapshotListener
                }
                launch {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        try {
                            val productMap = doc.get("product") as? Map<String, Any> ?: return@mapNotNull null
                            val product = Product(
                                id = productMap["id"] as? String ?: "",
                                name = productMap["name"] as? String ?: "",
                                price = safeDouble(productMap["price"]),
                                imageUrl = resolveUrl(productMap["imageUrl"] as? String ?: "")
                            )
                            ReviewOrder(
                                id = doc.id,
                                product = product,
                                status = doc.getString("status") ?: "Paid",
                                orderDate = doc.getString("orderDate") ?: ""
                            )
                        } catch (e: Exception) { null }
                    }
                    cachedOrders = orders
                    trySend(orders)
                }
            }
        awaitClose { listener.remove() }
    }.onStart {
        cachedOrders?.let { emit(it) }
    }

    suspend fun placeOrder(userId: String, orders: List<ReviewOrder>) {
        if (userId.isBlank()) return
        val batch = db.batch()
        val ordersRef = db.collection("users").document(userId).collection("orders")
        orders.forEach { order ->
            val data = hashMapOf(
                "product" to hashMapOf(
                    "id" to order.product.id,
                    "name" to order.product.name,
                    "price" to order.product.price,
                    "imageUrl" to order.product.imageUrl
                ),
                "status" to order.status,
                "orderDate" to order.orderDate
            )
            batch.set(ordersRef.document(), data)
        }
        batch.commit().await()
    }

    fun clearCache() {
        cachedProducts = null
        cachedCartItems = null
        cachedOrders = null
    }
}
