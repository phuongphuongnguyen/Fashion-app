package com.example.fashionapp.data.shop

import com.example.fashionapp.data.product.toProduct
import com.example.fashionapp.model.Product
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

// ── Tổng hợp số liệu của shop (doc shops/{shopId}) ──
data class ShopStats(
    val revenue: Double = 0.0,
    val soldCount: Int = 0,
    val orderCount: Int = 0,
    val productCount: Int = 0,
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val responseRate: Int = 0,
    val responseTime: String = ""
)

// ── Doanh thu 1 ngày (doc .../dailyRevenue/{yyyy-MM-dd}) ──
data class DailyRevenuePoint(
    val date: String = "",          // yyyy-MM-dd (chính là document id)
    val revenue: Double = 0.0,
    val orderCount: Int = 0,
    val soldCount: Int = 0
)

// ── 1 sản phẩm + doanh thu (Product model không có field revenue nên gộp thêm) ──
data class ShopProductStat(
    val product: Product,
    val revenue: Double = 0.0
)

/**
 * Đọc số liệu cho màn Quản lý shop & Phân tích sản phẩm.
 * Tất cả query đều là single-field (không cần composite index).
 */
object ShopDashboardRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun safeDouble(value: Any?): Double = (value as? Number)?.toDouble() ?: 0.0
    private fun safeInt(value: Any?): Int = (value as? Number)?.toInt() ?: 0

    // shops/{shopId} → ShopStats
    suspend fun getShopStats(shopId: String): ShopStats? {
        if (shopId.isBlank()) return null
        return try {
            val doc = db.collection("shops").document(shopId).get().await()
            if (!doc.exists()) return null
            ShopStats(
                revenue = safeDouble(doc.get("revenue")),
                soldCount = safeInt(doc.get("soldCount")),
                orderCount = safeInt(doc.get("orderCount")),
                productCount = safeInt(doc.get("productCount")),
                rating = (doc.get("rating") as? Number)?.toFloat() ?: 0f,
                reviewCount = safeInt(doc.get("reviewCount")),
                responseRate = safeInt(doc.get("responseRate")),
                responseTime = doc.getString("responseTime").orEmpty()
            )
        } catch (_: Exception) {
            null
        }
    }

    // shops/{shopId}/DailyRevenue → các ngày gần nhất (trả về tăng dần để vẽ chart trái→phải)
    suspend fun getShopDailyRevenue(shopId: String, limit: Int = 7): List<DailyRevenuePoint> {
        if (shopId.isBlank()) return emptyList()
        return fetchDailyRevenue(
            db.collection("shops").document(shopId).collection("dailyRevenue"),
            limit
        )
    }

    // products/{productId}/DailyRevenue → các ngày gần nhất
    suspend fun getProductDailyRevenue(productId: String, limit: Int = 7): List<DailyRevenuePoint> {
        if (productId.isBlank()) return emptyList()
        return fetchDailyRevenue(
            db.collection("products").document(productId).collection("dailyRevenue"),
            limit
        )
    }

    private suspend fun fetchDailyRevenue(ref: CollectionReference, limit: Int): List<DailyRevenuePoint> {
        return try {
            // Lấy tất cả rồi sort + cắt ở client → không cần index đặc biệt.
            // (Mỗi shop/product chỉ có tối đa ~vài chục ngày nên đọc hết là ổn.)
            val snap = ref.get().await()
            val result = snap.documents.map { d ->
                DailyRevenuePoint(
                    date = d.id,
                    revenue = safeDouble(d.get("revenue")),
                    orderCount = safeInt(d.get("orderCount")),
                    soldCount = safeInt(d.get("soldCount"))
                )
            }
                .sortedByDescending { it.date }   // mới nhất trước
                .take(limit)                      // lấy `limit` ngày gần nhất
                .sortedBy { it.date }             // rồi sort tăng dần để vẽ chart trái→phải

            android.util.Log.d("ShopDashboardRepo", "Fetched ${result.size} points for path: ${ref.path}")
            result
        } catch (e: Exception) {
            android.util.Log.e("ShopDashboardRepo", "Error fetching daily revenue for path: ${ref.path}", e)
            emptyList()
        }
    }

    // products where shopId == shopId → kèm field revenue của từng doc (sắp doanh thu cao lên đầu)
    suspend fun getProductsForShop(shopId: String): List<ShopProductStat> = coroutineScope {
        if (shopId.isBlank()) return@coroutineScope emptyList()
        try {
            val snap = db.collection("products")
                .whereEqualTo("shopId", shopId)
                .get().await()
            snap.documents.map { doc ->
                async {
                    val product = doc.toProduct() ?: return@async null
                    ShopProductStat(
                        product = product,
                        revenue = safeDouble(doc.get("revenue"))
                    )
                }
            }.awaitAll().filterNotNull()
                .sortedByDescending { it.revenue }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // products/{productId}.revenue (Product model không có field này)
    suspend fun getProductRevenue(productId: String): Double {
        if (productId.isBlank()) return 0.0
        return try {
            val doc = db.collection("products").document(productId).get().await()
            safeDouble(doc.get("revenue"))
        } catch (_: Exception) {
            0.0
        }
    }
}