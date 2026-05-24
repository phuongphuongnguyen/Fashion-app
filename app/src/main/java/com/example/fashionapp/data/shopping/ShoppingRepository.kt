package com.example.fashionapp.data.shopping

import com.example.fashionapp.model.Product
import com.example.fashionapp.data.CartItem
import com.example.fashionapp.data.ReviewOrder
import com.example.fashionapp.data.ShopProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

class ShoppingRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val urlCache = mutableMapOf<String, String>()

    private suspend fun resolveUrl(storagePath: String): String {
        if (storagePath.isBlank()) return ""
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) {
            return storagePath
        }
        urlCache[storagePath]?.let { return it }
        return try {
            val url = storage.reference.child(storagePath).downloadUrl.await().toString()
            urlCache[storagePath] = url
            url
        } catch (_: Exception) {
            storagePath
        }
    }

    fun getProductsFlow(): Flow<List<Product>> = callbackFlow {
        val listener = db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                launch {
                    val products = snapshot.documents.mapNotNull { doc ->
                        try {
                            mapProductDocument(doc)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    trySend(products)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getShopProfileFlow(shopId: String): Flow<ShopProfile?> = callbackFlow {
        if (shopId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = db.collection("shops").document(shopId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }

                launch {
                    trySend(
                        ShopProfile(
                            id = snapshot.id,
                            ownerUserId = snapshot.getString("userId").orEmpty(),
                            name = snapshot.getString("name").orEmpty(),
                            logoUrl = resolveUrl(snapshot.getString("logoRef").orEmpty()),
                            followerCount = snapshot.long("followerCount").toInt(),
                            productCount = snapshot.long("productCount").toInt(),
                            rating = snapshot.number("rating").toFloat(),
                            location = snapshot.getString("location").orEmpty(),
                            description = snapshot.getString("description").orEmpty()
                        )
                    )
                }
            }
        awaitClose { listener.remove() }
    }

    fun getCartItemsFlow(userId: String): Flow<List<CartItem>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("carts")
            .document(userId)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                launch {
                    val items = snapshot.documents.flatMap { doc ->
                        mapCartDocument(doc)
                    }
                    trySend(items)
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun addToCart(userId: String, cartItem: CartItem) {
        if (userId.isBlank()) return
        val data = hashMapOf(
            "productId" to cartItem.product.id,
            "name" to cartItem.product.name,
            "imageUrl" to cartItem.product.imageUrl,
            "price" to cartItem.product.price,
            "color" to cartItem.color,
            "size" to cartItem.size,
            "quantity" to cartItem.quantity,
            "shopId" to cartItem.product.shopId,
            "variantLabel" to listOf(cartItem.size, cartItem.color)
                .filter { it.isNotBlank() }
                .joinToString(" / ")
        )
        db.collection("carts")
            .document(userId)
            .collection("items")
            .document(cartItem.id)
            .set(data)
            .await()
    }

    suspend fun updateCartItemQuantity(userId: String, cartItemId: String, newQuantity: Int) {
        if (userId.isBlank()) return
        if (cartItemId.contains("__item_")) return
        val itemRef = db.collection("carts")
            .document(userId)
            .collection("items")
            .document(cartItemId)
        if (newQuantity <= 0) {
            itemRef.delete().await()
        } else {
            itemRef.update("quantity", newQuantity).await()
        }
    }

    suspend fun clearCart(userId: String, cartItems: List<CartItem>) {
        if (userId.isBlank()) return
        val batch = db.batch()
        val cartRef = db.collection("carts")
            .document(userId)
            .collection("items")
        cartItems.forEach { item ->
            batch.delete(cartRef.document(item.id))
        }
        batch.commit().await()
    }

    fun getOrdersFlow(userId: String): Flow<List<ReviewOrder>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("orders")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                launch {
                    val orders = snapshot.documents.flatMap { doc ->
                        mapOrderDocument(doc)
                    }
                    trySend(orders)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun placeOrder(userId: String, orders: List<ReviewOrder>) {
        if (userId.isBlank()) return
        if (orders.isEmpty()) return
        val items = orders.map { order ->
            hashMapOf(
                "productId" to order.product.id,
                "productName" to order.product.name,
                "productImage" to order.product.imageUrl,
                "quantity" to 1,
                "price" to order.product.price,
                "variantLabel" to ""
            )
        }
        val subtotal = orders.sumOf { it.product.price }
        val data = hashMapOf(
            "userId" to userId,
            "items" to items,
            "status" to "PENDING",
            "paymentMethod" to "COD",
            "paymentStatus" to "UNPAID",
            "subtotal" to subtotal,
            "shippingFee" to 0,
            "discount" to 0,
            "totalPrice" to subtotal,
            "voucherId" to null,
            "createdAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )
        db.collection("orders").document().set(data).await()
    }

    private suspend fun mapCartDocument(doc: DocumentSnapshot): List<CartItem> {
        val items = doc.get("items") as? List<*>
        if (!items.isNullOrEmpty()) {
            return items.mapIndexedNotNull { index, raw ->
                mapCartItemMap(
                    id = "${doc.id}__item_$index",
                    data = raw as? Map<*, *> ?: return@mapIndexedNotNull null
                )
            }
        }

        val productMap = doc.get("product") as? Map<*, *>
        return listOfNotNull(
            if (productMap != null) {
                val data = productMap.toMutableMap()
                data["quantity"] = doc.long("quantity")
                data["color"] = doc.getString("color").orEmpty()
                data["size"] = doc.getString("size").orEmpty()
                mapCartItemMap(doc.id, data)
            } else {
                mapCartItemMap(
                    id = doc.id,
                    data = mapOf(
                        "productId" to doc.getString("productId").orEmpty(),
                        "productName" to doc.getString("productName").orEmpty(),
                        "productImage" to doc.getString("productImage").orEmpty(),
                        "price" to doc.number("price"),
                        "quantity" to doc.long("quantity"),
                        "variantLabel" to doc.getString("variantLabel").orEmpty(),
                        "color" to doc.getString("color").orEmpty(),
                        "size" to doc.getString("size").orEmpty()
                    )
                )
            }
        )
    }

    private suspend fun mapCartItemMap(id: String, data: Map<*, *>): CartItem? {
        val productId = data.string("productId").ifBlank { data.string("id") }
        val firebaseProduct = productId.takeIf { it.isNotBlank() }?.let { fetchProduct(it) }
        val productName = data.string("productName").ifBlank {
            data.string("name").ifBlank {
                firebaseProduct?.name.orEmpty()
            }
        }
        val imageRef = data.string("productImage").ifBlank {
            data.string("image").ifBlank {
                data.string("imageUrl").ifBlank {
                    firebaseProduct?.imageUrl.orEmpty()
                }
            }
        }
        val variant = data.string("variantLabel")
        val parts = variant.split("/").map { it.trim() }
        return CartItem(
            id = id,
            product = Product(
                id = productId,
                name = productName,
                price = data.number("price").takeIf { it > 0.0 }
                    ?: data.number("unitPrice").takeIf { it > 0.0 }
                    ?: firebaseProduct?.price
                    ?: 0.0,
                imageUrl = resolveUrl(imageRef),
                rating = data.number("rating").takeIf { it > 0.0 }?.toFloat()
                    ?: firebaseProduct?.rating
                    ?: 0f,
                soldCount = data.long("soldCount").takeIf { it > 0L }?.toInt()
                    ?: firebaseProduct?.soldCount
                    ?: 0,
                shopId = firebaseProduct?.shopId.orEmpty()
            ),
            color = data.string("color").ifBlank { parts.getOrNull(1).orEmpty() },
            size = data.string("size").ifBlank { parts.firstOrNull().orEmpty() },
            quantity = data.long("quantity").toInt().coerceAtLeast(1)
        )
    }

    private suspend fun fetchProduct(productId: String): Product? {
        return try {
            val doc = db.collection("products").document(productId).get().await()
            if (doc.exists()) mapProductDocument(doc) else null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun mapProductDocument(doc: DocumentSnapshot): Product {
        val imageRef = doc.getStringList("images").firstOrNull()
            ?: doc.getString("imageUrl").orEmpty()
        return Product(
            id = doc.id,
            name = doc.getString("name").orEmpty(),
            price = doc.number("price"),
            imageUrl = resolveUrl(imageRef),
            rating = doc.number("rating").toFloat(),
            soldCount = doc.long("soldCount").toInt(),
            shopId = doc.getString("shopId").orEmpty()
        )
    }

    private suspend fun mapOrderDocument(doc: DocumentSnapshot): List<ReviewOrder> {
        val orderDate = doc.getTimestamp("createdAt")?.formatDate()
            ?: doc.getString("orderDate").orEmpty()
        val status = doc.getString("status") ?: "Paid"
        val items = doc.get("items") as? List<*>
        if (!items.isNullOrEmpty()) {
            return items.mapIndexedNotNull { index, raw ->
                val item = raw as? Map<*, *> ?: return@mapIndexedNotNull null
                ReviewOrder(
                    id = "${doc.id}__item_$index",
                    product = Product(
                        id = item.string("productId"),
                        name = item.string("productName"),
                        price = item.number("price"),
                        imageUrl = resolveUrl(item.string("productImage"))
                    ),
                    status = status,
                    orderDate = orderDate
                )
            }
        }

        val productMap = doc.get("product") as? Map<*, *> ?: return emptyList()
        return listOf(
            ReviewOrder(
                id = doc.id,
                product = Product(
                    id = productMap.string("id"),
                    name = productMap.string("name"),
                    price = productMap.number("price"),
                    imageUrl = resolveUrl(productMap.string("imageUrl"))
                ),
                status = status,
                orderDate = orderDate
            )
        )
    }

    private fun DocumentSnapshot.getStringList(field: String): List<String> {
        return (get(field) as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    }

    private fun DocumentSnapshot.number(field: String): Double {
        return (get(field) as? Number)?.toDouble() ?: 0.0
    }

    private fun DocumentSnapshot.long(field: String): Long {
        return (get(field) as? Number)?.toLong() ?: 0L
    }

    private fun Map<*, *>.string(field: String): String {
        return this[field] as? String ?: ""
    }

    private fun Map<*, *>.number(field: String): Double {
        return (this[field] as? Number)?.toDouble() ?: 0.0
    }

    private fun Map<*, *>.long(field: String): Long {
        return (this[field] as? Number)?.toLong() ?: 0L
    }

    private fun Timestamp.formatDate(): String {
        return SimpleDateFormat("MMMM yyyy", Locale.US).format(toDate())
    }
}
