package com.example.fashionapp.data.shop

import android.util.Log
import com.example.fashionapp.model.Product
import com.example.fashionapp.model.ProductVariant
import com.example.fashionapp.model.Category
import com.example.fashionapp.data.CartItem
import com.example.fashionapp.data.OrderItem
import com.example.fashionapp.data.ProductReview
import com.example.fashionapp.data.ReviewOrder
import com.example.fashionapp.data.StorageUrlResolver
import com.example.fashionapp.data.product.toProduct
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShopRepository {
    private val db = FirebaseFirestore.getInstance()
    private const val TAG = "ShopRepository"
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

    suspend fun getCategories(): List<Category> = coroutineScope {
        categoriesCache?.let { return@coroutineScope it }
        val targetIds = listOf("cat001a", "cat002", "cat003", "cat004", "cat005", "cat006")
        try {
            val snap = db.collection("categories")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), targetIds)
                .get().await()

            val docsMap = snap.documents.associateBy { it.id }
            // Parse + resolve URLs của tất cả category song song
            val result = targetIds.map { id ->
                async {
                    val doc = docsMap[id] ?: return@async null
                    val name = doc.getString("name") ?: return@async null
                    @Suppress("UNCHECKED_CAST")
                    val previewPaths = (doc.get("previewImages") as? List<String>) ?: emptyList()
                    val urls = StorageUrlResolver.resolveAll(previewPaths)
                    Category(id, name, urls)
                }
            }.awaitAll().filterNotNull()
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

    private suspend fun fetchProducts(query: Query): List<Product> = coroutineScope {
        try {
            val snap = query.get().await()
            // Parse + resolve URLs của các product song song
            snap.documents.map { async { it.toProduct() } }
                .awaitAll()
                .filterNotNull()
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

    // Điểm đánh giá của shop (shops/{shopId}.rating), realtime.
    fun getShopRatingFlow(shopId: String): Flow<Float> = callbackFlow {
        if (shopId.isBlank()) {
            trySend(0f)
            close()
            return@callbackFlow
        }
        val listener = db.collection("shops").document(shopId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(0f)
                    return@addSnapshotListener
                }
                trySend((snapshot.get("rating") as? Number)?.toFloat() ?: 0f)
            }
        awaitClose { listener.remove() }
    }

    fun getCartItemsFlow(userId: String): Flow<List<CartItem>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList()); close(); return@callbackFlow
        }
        val listener = db.collection("carts").document(userId).collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Log.e(TAG, "Failed to load cart items for user=$userId", error)
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
                                soldCount = (productMap["soldCount"] as? Number)?.toInt() ?: 0,
                                shopId = productMap["shopId"] as? String ?: doc.getString("shopId").orEmpty()
                            )
                            CartItem(
                                id = doc.id,
                                product = product,
                                color = doc.getString("color").orEmpty(),
                                size = doc.getString("size").orEmpty(),
                                quantity = safeInt(doc.get("quantity"))
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse cart item ${doc.reference.path}", e)
                            null
                        }
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
                "soldCount" to cartItem.product.soldCount,
                "shopId" to cartItem.product.shopId
            ),
            "color" to cartItem.color,
            "size" to cartItem.size,
            "quantity" to cartItem.quantity
        )
        val cartDoc = db.collection("carts").document(userId)
        db.runBatch { batch ->
            batch.set(
                cartDoc,
                mapOf(
                    "userId" to userId,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            batch.set(cartDoc.collection("items").document(cartItem.id), data)
        }.await()
    }

    suspend fun updateCartItemQuantity(userId: String, cartItemId: String, newQuantity: Int) {
        if (userId.isBlank()) return
        if (newQuantity <= 0) {
            db.collection("carts").document(userId).collection("items").document(cartItemId).delete().await()
        } else {
            db.collection("carts").document(userId).collection("items").document(cartItemId).update("quantity", newQuantity).await()
        }
    }

    suspend fun clearCart(userId: String, cartItems: List<CartItem>) {
        if (userId.isBlank()) return
        val batch = db.batch()
        val cartRef = db.collection("carts").document(userId).collection("items")
        cartItems.forEach { item -> batch.delete(cartRef.document(item.id)) }
        batch.commit().await()
    }

    fun getOrdersFlow(userId: String): Flow<List<ReviewOrder>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList()); close(); return@callbackFlow
        }
        val listener = db.collection("orders")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(cachedOrders ?: emptyList()); return@addSnapshotListener
                }
                launch {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val rawItems = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                            val orderItems = rawItems.mapNotNull { itemMap ->
                                @Suppress("UNCHECKED_CAST")
                                val productMap = itemMap["product"] as? Map<String, Any>
                                val variantLabel = itemMap["variantLabel"] as? String ?: ""
                                val variantParts = variantLabel.split("/").map { it.trim() }
                                val product = Product(
                                    id = productMap?.get("id") as? String
                                        ?: itemMap["productId"] as? String
                                        ?: "",
                                    name = productMap?.get("name") as? String
                                        ?: itemMap["productName"] as? String
                                        ?: "",
                                    price = safeDouble(productMap?.get("price") ?: itemMap["price"]),
                                    imageUrl = StorageUrlResolver.resolve(
                                        productMap?.get("imageUrl") as? String
                                            ?: itemMap["productImage"] as? String
                                            ?: ""
                                    ),
                                    shopId = productMap?.get("shopId") as? String
                                        ?: itemMap["shopId"] as? String
                                        ?: ""
                                )
                                if (product.id.isBlank() && product.name.isBlank()) return@mapNotNull null
                                OrderItem(
                                    cartItemId = itemMap["cartItemId"] as? String ?: "",
                                    product = product,
                                    shopId = itemMap["shopId"] as? String ?: product.shopId,
                                    color = itemMap["color"] as? String ?: variantParts.getOrNull(1).orEmpty(),
                                    size = itemMap["size"] as? String ?: variantParts.getOrNull(0).orEmpty(),
                                    quantity = safeInt(itemMap["quantity"]),
                                    lineTotal = safeDouble(itemMap["lineTotal"]).takeIf { it > 0.0 }
                                        ?: product.price * safeInt(itemMap["quantity"]).coerceAtLeast(1)
                                )
                            }
                            val product = orderItems.firstOrNull()?.product ?: run {
                                @Suppress("UNCHECKED_CAST")
                                val productMap = doc.get("product") as? Map<String, Any> ?: return@mapNotNull null
                                Product(
                                    id = productMap["id"] as? String ?: "",
                                    name = productMap["name"] as? String ?: "",
                                    price = safeDouble(productMap["price"]),
                                    imageUrl = StorageUrlResolver.resolve(productMap["imageUrl"] as? String ?: ""),
                                    shopId = productMap["shopId"] as? String ?: ""
                                )
                            }
                            ReviewOrder(
                                id = doc.id,
                                product = product,
                                status = doc.getString("orderStatus") ?: doc.getString("status") ?: "Ongoing",
                                orderDate = doc.getString("orderDate") ?: "",
                                placedAtMillis = (doc.get("placedAtMillis") as? Number)?.toLong()
                                    ?: doc.getTimestamp("createdAt")?.toDate()?.time
                                    ?: 0L,
                                items = orderItems,
                                userId = doc.getString("userId").orEmpty(),
                                paymentMethod = doc.getString("paymentMethod").orEmpty(),
                                paymentStatus = doc.getString("paymentStatus").orEmpty(),
                                shippingMethod = doc.getString("shippingMethod").orEmpty(),
                                shippingFee = safeDouble(doc.get("shippingFee")),
                                shippingAddress = doc.getString("shippingAddress").orEmpty(),
                                totalPrice = safeDouble(doc.get("totalPrice")),
                                momoOrderId = doc.getString("momoOrderId").orEmpty()
                            )
                        } catch (e: Exception) { null }
                    }.sortedByDescending { it.placedAtMillis }
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
        cartItems: List<CartItem>,
        orderDate: String,
        placedAtMillis: Long,
        paymentMethod: String,
        paymentStatus: String,
        shippingMethod: String,
        shippingFee: Double,
        shippingAddress: String,
        momoOrderId: String = ""
    ): String {
        if (userId.isBlank() || cartItems.isEmpty()) return ""

        val orderRef = db.collection("orders").document()
        val subtotal = cartItems.sumOf { it.totalPrice }
        val totalPrice = subtotal + shippingFee
        val isPaid = paymentStatus.equals("PAID", ignoreCase = true)
        val dayId = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(placedAtMillis))
        val data = hashMapOf<String, Any>(
            "userId" to userId,
            "items" to cartItems.map { item ->
                val shopId = item.product.shopId
                hashMapOf(
                    "cartItemId" to item.id,
                    "product" to hashMapOf(
                        "id" to item.product.id,
                        "name" to item.product.name,
                        "price" to item.product.price,
                        "imageUrl" to item.product.imageUrl,
                        "shopId" to shopId
                    ),
                    "productId" to item.product.id,
                    "shopId" to shopId,
                    "color" to item.color,
                    "size" to item.size,
                    "quantity" to item.quantity,
                    "lineTotal" to item.totalPrice
                )
            },
            "subtotal" to subtotal,
            "shippingFee" to shippingFee,
            "totalPrice" to totalPrice,
            "orderStatus" to "Ongoing",
            "orderDate" to orderDate,
            "placedAtMillis" to placedAtMillis,
            "paymentMethod" to paymentMethod,
            "paymentStatus" to paymentStatus,
            "shippingMethod" to shippingMethod,
            "shippingAddress" to shippingAddress,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (momoOrderId.isNotBlank()) {
            data["momoOrderId"] = momoOrderId
        }

        db.runBatch { batch ->
            batch.set(orderRef, data)
            val cartRef = db.collection("carts").document(userId).collection("items")
            cartItems.forEach { item ->
                batch.delete(cartRef.document(item.id))
            }

            if (isPaid) {
                applyRevenueUpdates(batch, cartItems, dayId, totalPrice)
            }
        }.await()
        return orderRef.id
    }

    private fun applyRevenueUpdates(
        batch: com.google.firebase.firestore.WriteBatch,
        cartItems: List<CartItem>,
        dayId: String,
        orderTotal: Double
    ) {
        val productStats = cartItems
            .filter { it.product.id.isNotBlank() }
            .groupBy { it.product.id }
            .mapValues { (_, items) ->
                RevenueStats(
                    revenue = items.sumOf { it.totalPrice },
                    soldCount = items.sumOf { it.quantity },
                    orderCount = 1
                )
            }

        val shopStats = cartItems
            .filter { it.product.shopId.isNotBlank() }
            .groupBy { it.product.shopId }
            .mapValues { (_, items) ->
                RevenueStats(
                    revenue = items.sumOf { it.totalPrice },
                    soldCount = items.sumOf { it.quantity },
                    orderCount = 1
                )
            }

        productStats.forEach { (productId, stats) ->
            val productRef = db.collection("products").document(productId)
            batch.update(
                productRef,
                mapOf(
                    "soldCount" to FieldValue.increment(stats.soldCount.toLong()),
                    "revenue" to FieldValue.increment(stats.revenue)
                )
            )
            batch.set(
                productRef.collection("dailyRevenue").document(dayId),
                dailyRevenueData(stats),
                SetOptions.merge()
            )
        }

        shopStats.forEach { (shopId, stats) ->
            val shopRef = db.collection("shops").document(shopId)
            batch.set(
                shopRef,
                mapOf(
                    "revenue" to FieldValue.increment(stats.revenue),
                    "soldCount" to FieldValue.increment(stats.soldCount.toLong()),
                    "orderCount" to FieldValue.increment(stats.orderCount.toLong()),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            batch.set(
                shopRef.collection("dailyRevenue").document(dayId),
                dailyRevenueData(stats),
                SetOptions.merge()
            )
        }

        batch.set(
            db.collection("analytics_daily").document(dayId),
            mapOf(
                "grossRevenue" to FieldValue.increment(orderTotal),
                "netRevenue" to FieldValue.increment(orderTotal),
                "orderCount" to FieldValue.increment(1L),
                "soldCount" to FieldValue.increment(cartItems.sumOf { it.quantity }.toLong()),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
    }

    private fun dailyRevenueData(stats: RevenueStats): Map<String, Any> {
        return mapOf(
            "revenue" to FieldValue.increment(stats.revenue),
            "soldCount" to FieldValue.increment(stats.soldCount.toLong()),
            "orderCount" to FieldValue.increment(stats.orderCount.toLong()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private data class RevenueStats(
        val revenue: Double,
        val soldCount: Int,
        val orderCount: Int
    )

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
