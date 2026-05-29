package com.example.fashionapp.data.shop

import com.example.fashionapp.model.Product
import com.example.fashionapp.model.ProductVariant
import com.example.fashionapp.model.Category
import com.example.fashionapp.data.CartItem
import com.example.fashionapp.data.ProductReview
import com.example.fashionapp.data.ReviewOrder
import com.example.fashionapp.data.StorageUrlResolver
import com.example.fashionapp.data.product.toProduct
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object ShopRepository {
    private val db = FirebaseFirestore.getInstance()
    private const val REVIEW_EDIT_WINDOW_MILLIS = 7L * 24 * 60 * 60 * 1000

    // ── In-Memory Cache ──────────────────────────────────────────────────────
    private var cachedProducts: List<Product>? = null
    private var cachedCartItems: List<CartItem>? = null
    private var cachedOrders: List<ReviewOrder>? = null
    private var categoriesCache: List<Category>? = null
    private var newItemsCache: List<Product>? = null
    private var mostPopularCache: List<Product>? = null
    private var forYouCache: List<Product>? = null
    private val shopLogoCache = mutableMapOf<String, String?>()

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
                    val products = snapshot.documents.mapNotNull { it.toProduct() }
                    cachedProducts = products
                    trySend(products)
                }
            }
        awaitClose { listener.remove() }
    }.onStart {
        cachedProducts?.let { emit(it) }
    }

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
                val urls = StorageUrlResolver.resolveAll(previewPaths)
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

    private suspend fun fetchProducts(query: Query): List<Product> {
        return try {
            val snap = query.get().await()
            snap.documents.mapNotNull { it.toProduct() }
        } catch (_: Exception) { emptyList() }
    }

    fun getShopLogoUrlFlow(shopId: String): Flow<String> = callbackFlow {
        if (shopId.isBlank()) {
            trySend("")
            close()
            return@callbackFlow
        }

        val listener = db.collection("shops").document(shopId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(shopLogoCache[shopId] ?: "")
                    return@addSnapshotListener
                }

                launch {
                    val logoRef = snapshot.getString("logoRef").orEmpty()
                    val logoUrl = StorageUrlResolver.resolve(logoRef).takeIf { it.isNotBlank() }
                    shopLogoCache[shopId] = logoUrl
                    trySend(logoUrl.orEmpty())
                }
            }
        awaitClose { listener.remove() }
    }.onStart {
        shopLogoCache[shopId]?.let { emit(it) }
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
                                imageUrl = StorageUrlResolver.resolve(productMap["imageUrl"] as? String ?: ""),
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
                                imageUrl = StorageUrlResolver.resolve(productMap["imageUrl"] as? String ?: "")
                            )
                            ReviewOrder(
                                id = doc.id,
                                product = product,
                                status = doc.getString("status") ?: "Paid",
                                orderDate = doc.getString("orderDate") ?: "",
                                placedAtMillis = (doc.get("placedAtMillis") as? Number)?.toLong() ?: 0L
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

    fun getProductReviewsFlow(userId: String): Flow<Map<String, ProductReview>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyMap())
            close()
            return@callbackFlow
        }

        val listener = db.collectionGroup("reviews")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyMap())
                    return@addSnapshotListener
                }

                val reviews = snapshot.documents.mapNotNull { doc ->
                    val orderId = doc.getString("orderId").orEmpty()
                    if (orderId.isBlank()) {
                        null
                    } else {
                        ProductReview(
                            id = doc.id,
                            orderId = orderId,
                            productId = doc.getString("productId").orEmpty(),
                            userId = doc.getString("userId").orEmpty(),
                            rating = (doc.get("rating") as? Number)?.toInt() ?: 0,
                            comment = doc.getString("comment").orEmpty(),
                            editCount = (doc.get("editCount") as? Number)?.toInt() ?: 0,
                            createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                            updatedAtMillis = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                        )
                    }
                }.associateBy { it.orderId }

                trySend(reviews)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getReviewsForOrders(orders: List<ReviewOrder>): Map<String, ProductReview> {
        return orders.mapNotNull { order ->
            if (order.product.id.isBlank()) return@mapNotNull null

            val review = runCatching {
                val doc = db.collection("products")
                    .document(order.product.id)
                    .collection("reviews")
                    .document(order.id)
                    .get()
                    .await()

                if (!doc.exists()) {
                    null
                } else {
                    ProductReview(
                        id = doc.id,
                        orderId = doc.getString("orderId").orEmpty().ifBlank { order.id },
                        productId = doc.getString("productId").orEmpty().ifBlank { order.product.id },
                        userId = doc.getString("userId").orEmpty(),
                        rating = (doc.get("rating") as? Number)?.toInt() ?: 0,
                        comment = doc.getString("comment").orEmpty(),
                        editCount = (doc.get("editCount") as? Number)?.toInt() ?: 0,
                        createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        updatedAtMillis = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                    )
                }
            }.getOrNull()

            review?.let { it.orderId to it }
        }.toMap()
    }

    suspend fun placeOrder(
        userId: String,
        orders: List<ReviewOrder>,
        paymentMethod: String,
        shippingMethod: String,
        shippingFee: Double,
        shippingAddress: String
    ) {
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
                "orderDate" to order.orderDate,
                "placedAtMillis" to order.placedAtMillis,
                "paymentMethod" to paymentMethod,
                "shippingMethod" to shippingMethod,
                "shippingFee" to shippingFee,
                "shippingAddress" to shippingAddress
            )
            batch.set(ordersRef.document(), data)
        }
        batch.commit().await()
    }

    suspend fun submitProductReview(
        userId: String,
        order: ReviewOrder,
        rating: Int,
        comment: String,
        userName: String,
        userAvatarUrl: String
    ) {
        if (userId.isBlank() || order.product.id.isBlank()) return

        val safeRating = rating.coerceIn(1, 5)
        val productRef = db.collection("products").document(order.product.id)
        val reviewRef = productRef.collection("reviews").document(order.id)

        db.runTransaction { transaction ->
            val productSnapshot = transaction.get(productRef)
            val reviewSnapshot = transaction.get(reviewRef)

            val currentRating = (productSnapshot.get("rating") as? Number)?.toDouble() ?: 0.0
            val currentReviewCount = (productSnapshot.get("reviewCount") as? Number)?.toInt() ?: 0
            val oldRating = (reviewSnapshot.get("rating") as? Number)?.toDouble()
            val currentEditCount = (reviewSnapshot.get("editCount") as? Number)?.toInt() ?: 0
            val now = Timestamp.now()
            val createdAt = reviewSnapshot.getTimestamp("createdAt")

            if (reviewSnapshot.exists()) {
                val createdAtMillis = createdAt?.toDate()?.time ?: 0L
                if (createdAtMillis <= 0L || now.toDate().time - createdAtMillis > REVIEW_EDIT_WINDOW_MILLIS) {
                    throw IllegalStateException("Review edit window expired")
                }
                if (currentEditCount >= 1) {
                    throw IllegalStateException("Review can only be edited once")
                }
            }

            val newReviewCount = if (reviewSnapshot.exists()) {
                currentReviewCount.coerceAtLeast(1)
            } else {
                currentReviewCount + 1
            }

            val newAverage = if (reviewSnapshot.exists() && oldRating != null) {
                ((currentRating * newReviewCount) - oldRating + safeRating) / newReviewCount
            } else {
                ((currentRating * currentReviewCount) + safeRating) / newReviewCount
            }

            val reviewData = hashMapOf<String, Any>(
                "id" to order.id,
                "orderId" to order.id,
                "productId" to order.product.id,
                "userId" to userId,
                "userName" to userName,
                "userAvatarUrl" to userAvatarUrl,
                "rating" to safeRating,
                "comment" to comment.trim(),
                "editCount" to if (reviewSnapshot.exists()) currentEditCount + 1 else 0,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (!reviewSnapshot.exists()) {
                reviewData["createdAt"] = FieldValue.serverTimestamp()
            }

            transaction.set(reviewRef, reviewData, SetOptions.merge())
            transaction.update(
                productRef,
                mapOf(
                    "rating" to newAverage.toFloat(),
                    "reviewCount" to newReviewCount
                )
            )
        }.await()
    }

    fun clearCache() {
        cachedProducts = null
        cachedCartItems = null
        cachedOrders = null
        categoriesCache = null
        newItemsCache = null
        mostPopularCache = null
        forYouCache = null
        shopLogoCache.clear()
    }
}
