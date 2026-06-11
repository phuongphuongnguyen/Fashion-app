package com.example.fashionapp.ui.app.shopping

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Worker chạy ngầm gửi notification theo dõi đơn hàng.
 *
 * Nhận inputData:
 *  - step (Int): 1 = đóng gói, 2 = đang giao 50%, 3 = giao thành công
 *  - orderId (String): Firestore order document ID
 *  - userId (String): Firebase user ID
 *  - amount (Long): số tiền đơn hàng
 *
 * Step 3 sẽ cập nhật Firestore orders/{orderId} → orderStatus = "Delivered"
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

        val orderState = getOrderState(orderId) ?: return Result.retry()
        if (orderState.isTerminal) {
            Log.d(TAG, "Skip tracking step=$step for terminal order=$orderId status=${orderState.orderStatus}/${orderState.paymentStatus}")
            return Result.success()
        }

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
        saveToFirestore(userId, message, "SHIPPING")

        // ── Step 3: cập nhật Firestore status → "Delivered" ───────────
        if (step == 3) {
            if (!updateOrderStatusToDelivered(orderId)) {
                Log.d(TAG, "Skip delivered update for order=$orderId because status changed")
            }
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
    // Path: orders/{orderId}
    private fun updateOrderStatusToDelivered(orderId: String): Boolean {
        val db = FirebaseFirestore.getInstance()

        return try {
            Tasks.await(
                db.runTransaction { transaction ->
                    val orderRef = db.collection("orders").document(orderId)
                    val snapshot = transaction.get(orderRef)
                    if (!snapshot.exists()) return@runTransaction false

                    val state = OrderState(
                        orderStatus = snapshot.getString("orderStatus").orEmpty(),
                        paymentStatus = snapshot.getString("paymentStatus").orEmpty()
                    )
                    if (state.isTerminal) return@runTransaction false

                    transaction.update(
                        orderRef,
                        mapOf(
                            "orderStatus" to "Delivered",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    true
                }
            ).also { updated ->
                if (updated) Log.d(TAG, "Updated order $orderId to Delivered")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update order $orderId: ${e.message}")
            false
        }
    }

    private fun getOrderState(orderId: String): OrderState? {
        return try {
            val snapshot = Tasks.await(
                FirebaseFirestore.getInstance()
                    .collection("orders")
                    .document(orderId)
                    .get()
            )
            if (!snapshot.exists()) return null
            OrderState(
                orderStatus = snapshot.getString("orderStatus").orEmpty(),
                paymentStatus = snapshot.getString("paymentStatus").orEmpty()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read order $orderId before tracking: ${e.message}")
            null
        }
    }

    private data class OrderState(
        val orderStatus: String,
        val paymentStatus: String
    ) {
        val isTerminal: Boolean
            get() = orderStatus.equals("Cancelled", ignoreCase = true) ||
                orderStatus.equals("Canceled", ignoreCase = true) ||
                paymentStatus.equals("REFUNDED", ignoreCase = true) ||
                paymentStatus.equals("CANCELLED", ignoreCase = true)
    }

    private fun saveToFirestore(userId: String, message: String, type: String) {
        if (userId.isEmpty()) return
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users")
            .document(userId)
            .collection("user_notifications")
            .document()

        val data = hashMapOf(
            "id" to docRef.id,
            "userId" to userId,
            "message" to message,
            "type" to type,
            "isRead" to false,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        docRef.set(data)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully saved tracking notification to Firestore: ${docRef.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save notification: ${e.message}")
            }
    }

    private fun formatAmount(amount: Long): String =
        amount.toString().reversed().chunked(3).joinToString(".").reversed()
}
