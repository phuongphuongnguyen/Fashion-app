package com.example.fashionapp.ui.app.chatbot

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirestoreContextBuilder {

    private val db  = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // ── Fetch tất cả data liên quan → tạo context string ─────────────────────
    // Truy vấn toàn bộ dữ liệu từ Firestore (Sản phẩm, Danh mục, Vouchers, Đơn hàng, Shops) để xây dựng ngữ cảnh (Context) cho Chatbot
    suspend fun buildContext(): String {
        val sb = StringBuilder()

        // 1. Products
        try {
            val products = db.collection("products")
                .whereEqualTo("isActive", true)
                .limit(30)
                .get().await()
            sb.appendLine("=== DANH SÁCH SẢN PHẨM ===")
            products.documents.forEach { doc ->
                val name     = doc.getString("name") ?: return@forEach
                val price    = doc.getLong("price") ?: 0L
                val origPrice= doc.getLong("originalPrice") ?: 0L
                val discount = doc.getLong("discountPercent") ?: 0L
                val desc     = doc.getString("description") ?: ""
                val category = doc.getString("categoryId") ?: ""
                val shipping = doc.getBoolean("freeShipping") ?: false
                val rating   = doc.getDouble("rating") ?: 0.0
                val sold     = doc.getLong("soldCount") ?: 0L
                sb.appendLine("- Sản phẩm: $name | Giá: ₫${formatPrice(price)} | Giảm: $discount% | Giá gốc: ₫${formatPrice(origPrice)} | Danh mục: $category | Miễn phí ship: $shipping | Rating: $rating | Đã bán: $sold | Mô tả: $desc")
            }
            sb.appendLine()
        } catch (_: Exception) {}

        // 2. Categories
        try {
            val categories = db.collection("categories").get().await()
            sb.appendLine("=== DANH MỤC SẢN PHẨM ===")
            categories.documents.forEach { doc ->
                val name     = doc.getString("name") ?: return@forEach
                val parentId = doc.getString("parentId")
                if (parentId == null) {
                    sb.appendLine("- Danh mục chính: $name (ID: ${doc.id})")
                } else {
                    sb.appendLine("- Danh mục con: $name (thuộc: $parentId)")
                }
            }
            sb.appendLine()
        } catch (_: Exception) {}

        // 3. Vouchers
        try {
            val vouchers = db.collection("vouchers").get().await()
            sb.appendLine("=== MÃ GIẢM GIÁ HIỆN CÓ ===")
            vouchers.documents.forEach { doc ->
                val code      = doc.getString("code") ?: return@forEach
                val desc      = doc.getString("description") ?: ""
                val type      = doc.getString("discountType") ?: ""
                val value     = doc.getLong("discountValue") ?: 0L
                val minOrder  = doc.getLong("minOrderValue") ?: 0L
                val limit     = doc.getLong("usageLimit") ?: 0L
                val used      = doc.getLong("usedCount") ?: 0L
                val remaining = limit - used
                sb.appendLine("- Mã: $code | $desc | Loại: $type | Giảm: ₫${formatPrice(value)} | Đơn tối thiểu: ₫${formatPrice(minOrder)} | Còn lại: $remaining lượt")
            }
            sb.appendLine()
        } catch (_: Exception) {}

        // 4. Đơn hàng của user hiện tại
        if (uid.isNotEmpty()) {
            try {
                val orders = db.collection("orders")
                    .whereEqualTo("userId", uid)
                    .limit(10)
                    .get().await()
                sb.appendLine("=== ĐƠN HÀNG CỦA NGƯỜI DÙNG ===")
                orders.documents.forEach { doc ->
                    val total         = doc.getLong("totalPrice") ?: 0L
                    val paymentStatus = doc.getString("paymentStatus") ?: ""
                    val status        = doc.getString("status") ?: ""
                    val method        = doc.getString("paymentMethod") ?: ""
                    val tracking      = doc.getString("trackingNumber") ?: "Chưa có"
                    val orderId       = doc.id.takeLast(6)
                    sb.appendLine("- Đơn #$orderId | Tổng: ₫${formatPrice(total)} | Thanh toán: $paymentStatus | Trạng thái: $status | Phương thức: $method | Mã vận đơn: $tracking")
                }
                sb.appendLine()
            } catch (_: Exception) {}
        }

        // 5. Shops
        try {
            val shops = db.collection("shops").limit(10).get().await()
            sb.appendLine("=== DANH SÁCH SHOP ===")
            shops.documents.forEach { doc ->
                val name     = doc.getString("name") ?: return@forEach
                val desc     = doc.getString("description") ?: ""
                val rating   = doc.getDouble("rating") ?: 0.0
                val followers= doc.getLong("followerCount") ?: 0L
                sb.appendLine("- Shop: $name | Rating: $rating | Followers: $followers | $desc")
            }
            sb.appendLine()
        } catch (_: Exception) {}

        return sb.toString()
    }

    // Định dạng số tiền kiểu Long sang chuỗi ký tự hiển thị phân cách phần nghìn tiền tệ (ví dụ: 100.000)
    private fun formatPrice(price: Long): String =
        price.toString().reversed().chunked(3).joinToString(".").reversed()
}
