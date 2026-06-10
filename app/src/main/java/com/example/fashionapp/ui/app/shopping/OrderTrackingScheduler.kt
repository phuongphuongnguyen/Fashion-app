package com.example.fashionapp.ui.app.shopping

import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/**
 * Lên lịch 3 notification theo dõi đơn hàng sau khi thanh toán thành công.
 *
 * Timeline:
 *   +30s  → "Đơn hàng đã được đóng gói 📦"
 *   +60s  → "Đơn hàng đã đi được 50% 🚚"
 *   +90s  → "Đơn hàng đã được giao đến bạn ✅" → status → Delivered
 */
object OrderTrackingScheduler {

    private const val TAG = "OrderTrackingScheduler"

    fun scheduleTracking(context: Context, orderId: String, userId: String, amount: Long) {
        val workManager = WorkManager.getInstance(context)

        val steps = listOf(
            Triple(1, 30L, TimeUnit.SECONDS),   // Step 1: đóng gói
            Triple(2, 60L, TimeUnit.SECONDS),   // Step 2: đang giao 50%
            Triple(3, 90L, TimeUnit.SECONDS)    // Step 3: giao thành công
        )

        steps.forEach { (step, delay, unit) ->
            val data = workDataOf(
                "step"    to step,
                "orderId" to orderId,
                "userId"  to userId,
                "amount"  to amount
            )

            val request = OneTimeWorkRequestBuilder<OrderTrackingWorker>()
                .setInitialDelay(delay, unit)
                .setInputData(data)
                .addTag("order_tracking_$orderId")
                .build()

            workManager.enqueue(request)
            Log.d(TAG, "Scheduled step $step with delay ${delay}s for order $orderId")
        }
    }

    fun showPaymentNotification(context: Context, amount: Long) {
        val channelId = "payment_channel"
        val manager   = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                android.app.NotificationChannel(channelId, "Thanh toán", android.app.NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val formattedAmount = amount.toString().reversed().chunked(3).joinToString(".").reversed()
        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Thanh toán thành công 🎉")
            .setContentText("Đơn hàng ₫$formattedAmount đã được thanh toán!")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
