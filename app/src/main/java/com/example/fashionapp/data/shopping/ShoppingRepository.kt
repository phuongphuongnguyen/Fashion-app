package com.example.fashionapp.data.shopping

import com.example.fashionapp.model.Product
import com.example.fashionapp.data.CartItem
import com.example.fashionapp.data.ReviewOrder
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ShoppingRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getProductsFlow(): Flow<List<Product>> = callbackFlow {
        val listener = db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val products = snapshot.documents.mapNotNull { doc ->
                    try {
                        Product(
                            id = doc.id,
                            name = doc.getString("name").orEmpty(),
                            price = doc.getDouble("price") ?: 0.0,
                            imageUrl = doc.getString("imageUrl").orEmpty(),
                            rating = doc.getDouble("rating")?.toFloat() ?: 0f,
                            soldCount = doc.getLong("soldCount")?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(products)
            }
        awaitClose { listener.remove() }
    }

    fun getCartItemsFlow(userId: String): Flow<List<CartItem>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("users").document(userId).collection("cart")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                // Mapping logic will need to resolve Product reference or store product denormalized.
                // For simplicity, we assume product is stored as a nested map in the cart item.
                val items = snapshot.documents.mapNotNull { doc ->
                    try {
                        val productMap = doc.get("product") as? Map<String, Any> ?: return@mapNotNull null
                        val product = Product(
                            id = productMap["id"] as? String ?: "",
                            name = productMap["name"] as? String ?: "",
                            price = (productMap["price"] as? Number)?.toDouble() ?: 0.0,
                            imageUrl = productMap["imageUrl"] as? String ?: "",
                            rating = (productMap["rating"] as? Number)?.toFloat() ?: 0f,
                            soldCount = (productMap["soldCount"] as? Number)?.toInt() ?: 0
                        )
                        CartItem(
                            id = doc.id,
                            product = product,
                            color = doc.getString("color").orEmpty(),
                            size = doc.getString("size").orEmpty(),
                            quantity = doc.getLong("quantity")?.toInt() ?: 1
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(items)
            }
        awaitClose { listener.remove() }
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
        val listener = db.collection("users").document(userId).collection("orders")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val orders = snapshot.documents.mapNotNull { doc ->
                    try {
                        val productMap = doc.get("product") as? Map<String, Any> ?: return@mapNotNull null
                        val product = Product(
                            id = productMap["id"] as? String ?: "",
                            name = productMap["name"] as? String ?: "",
                            price = (productMap["price"] as? Number)?.toDouble() ?: 0.0,
                            imageUrl = productMap["imageUrl"] as? String ?: ""
                        )
                        ReviewOrder(
                            id = doc.id,
                            product = product,
                            status = doc.getString("status") ?: "Paid",
                            orderDate = doc.getString("orderDate") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(orders)
            }
        awaitClose { listener.remove() }
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
            val newDocRef = ordersRef.document()
            batch.set(newDocRef, data)
        }
        batch.commit().await()
    }
}
