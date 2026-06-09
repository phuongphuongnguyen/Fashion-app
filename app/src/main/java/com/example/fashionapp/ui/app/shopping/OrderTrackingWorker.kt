package com.example.fashionapp.ui.app.shopping

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Worker chạy ngầm gửi notification theo dõi đơn hàng.
 *
 * Nhận inputData:
 *  - step (Int): 1 = đóng gói, 2 = đang giao 50%, 3 = giao thành công
 *  - orderId (String): MoMo orderId
 *  - userId (String): Firebase user ID
 *  - amount (Long): số tiền đơn hàng
 *
 * Step 3 sẽ cập nhật Firestore users/{userId}/orders/ → status = "Delivered"
 * để đơn chuyển sang tab History.
 */
class OrderTrackingWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val TAG = "OrderTracking"
        private const val CHANNEL_ID = "order_tracking_channel"
        private const val CHANNEL_NAME = "Theo dõi đơn hàng"
    }

    override fun doWork(): Result {
        val step    = inputData.getInt("step", 0)
        val orderId = inputData.getString("orderId") ?: return Result.failure()
        val userId  = inputData.getString("userId") ?: return Result.failure()
        val amount  = inputData.getLong("amount", 0L)

        Log.d(TAG, "doWork step=$step orderId=$orderId userId=$userId")

        // ── Nội dung notification theo step ──────────────────────────────
        val (title, message) = when (step) {
            1 -> "Đơn hàng đã được đóng gói 📦" to
                    "Đơn hàng ₫${formatAmount(amount)} đang được chuẩn bị giao cho đơn vị vận chuyển."
            2 -> "Đơn hàng đã đi được 50% 🚚" to
                    "Đơn hàng ₫${formatAmount(amount)} đang trên đường giao đến bạn."
            3 -> "Đơn hàng đã được giao đến bạn ✅" to
                    "Đơn hàng ₫${formatAmount(amount)} đã giao thành công. Cảm ơn bạn!"
            else -> return Result.failure()
        }

        showNotification(title, message, step, orderId)

        // ── Step 3: cập nhật Firestore status → "Delivered" ───────────
        if (step == 3) {
            updateOrderStatusToDelivered(userId)
        }

        return Result.success()
    }

    // ── Gửi notification ─────────────────────────────────────────────────
    private fun showNotification(title: String, message: String, step: Int, orderId: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        // Tạo channel (idempotent — gọi nhiều lần không sao)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Unique ID = orderId hash + step, tránh ghi đè
        val notificationId = orderId.hashCode() + step
        manager.notify(notificationId, notification)
    }

    // ── Cập nhật Firestore status ────────────────────────────────────────
    // Update tất cả order documents của user có status="Ongoing" → "Delivered"
    // Path: users/{userId}/orders/{docId}
    private fun updateOrderStatusToDelivered(userId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).collection("orders")
            .whereEqualTo("status", "Ongoing")
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.update(doc.reference, "status", "Delivered")
                }
                batch.commit()
                    .addOnSuccessListener {
                        Log.d(TAG, "Updated ${snapshot.size()} orders → Delivered for user $userId")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Batch update failed: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to query orders: ${e.message}")
            }
    }

    private fun formatAmount(amount: Long): String =
        amount.toString().reversed().chunked(3).joinToString(".").reversed()
}
