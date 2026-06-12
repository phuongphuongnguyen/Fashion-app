package com.example.fashionapp.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.fashionapp.data.NotificationModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object NotificationListenerHelper {
    private var listenerRegistration: ListenerRegistration? = null
    private var lastUserId: String? = null
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var listenerStartedAt: Timestamp? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val uid = firebaseAuth.currentUser?.uid.orEmpty()
        if (uid != lastUserId) {
            stopListening()
            if (uid.isNotBlank()) {
                startListening(null, uid)
            }
            lastUserId = uid
        }
    }

    private var appContext: Context? = null

    // Khởi tạo Helper lắng nghe thông báo và đăng ký bộ lắng nghe sự thay đổi trạng thái đăng nhập Firebase Auth
    fun init(context: Context) {
        appContext = context.applicationContext
        auth.addAuthStateListener(authStateListener)
        val uid = auth.currentUser?.uid.orEmpty()
        if (uid.isNotBlank()) {
            startListening(appContext, uid)
            lastUserId = uid
        }
    }

    // Bắt đầu lắng nghe các thông báo mới từ Firestore của người dùng hiện tại theo thời gian thực (realtime snapshot)
    private fun startListening(context: Context?, userId: String) {
        stopListening()
        val targetContext = context ?: appContext ?: return
        listenerStartedAt = Timestamp.now()

        listenerRegistration = db.collection("users")
            .document(userId)
            .collection("user_notifications")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                for (change in snapshot.documentChanges) {
                    if (change.type == DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val notification = doc.toObject(NotificationModel::class.java)
                        
                        val createdAt = notification.createdAt
                        val startedAt = listenerStartedAt
                        
                        // Only show notifications created after the listener started
                        if (createdAt != null && startedAt != null && createdAt.seconds >= startedAt.seconds) {
                            if (!notification.isRead) {
                                triggerSystemNotificationIfNeeded(targetContext, userId, notification)
                            }
                        }
                    }
                }
            }
    }

    // Ngừng lắng nghe thông báo từ Firestore và giải phóng đăng ký bộ lắng nghe để tránh rò rỉ bộ nhớ (memory leak)
    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    // Kiểm tra cấu hình cài đặt thông báo của người dùng và hiển thị thông báo hệ thống nếu được phép
    private fun triggerSystemNotificationIfNeeded(context: Context, userId: String, notification: NotificationModel) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val prefix = "${userId}_"
        
        val isMasterEnabled = prefs.getBoolean("${prefix}notifications", prefs.getBoolean("notifications", true))
        val isSystemEnabled = prefs.getBoolean("${prefix}system_notifications", prefs.getBoolean("system_notifications", true))
        
        if (!isMasterEnabled || !isSystemEnabled) return

        val isAllowed = when (notification.type) {
            "PAYMENT", "SHIPPING", "CANCELLED", "REFUND" -> prefs.getBoolean("${prefix}order_updates", prefs.getBoolean("order_updates", true))
            "LIKE", "COMMENT", "SAVE" -> prefs.getBoolean("${prefix}social_interactions", prefs.getBoolean("social_interactions", true))
            else -> true
        }

        if (!isAllowed) return

        // Show system notification
        val channelId = "social_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelName = "Tương tác mạng xã hội"
        manager.createNotificationChannel(
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        )

        val title = when (notification.type) {
            "PAYMENT" -> "Thanh toán thành công 🎉"
            "SHIPPING" -> "Theo dõi đơn hàng 🚚"
            "CANCELLED" -> "Hủy đơn hàng thành công 💸"
            "REFUND" -> "Hoàn tiền thành công 💸"
            "LIKE" -> "Yêu thích bài viết ❤️"
            "COMMENT" -> "Bình luận bài viết 💬"
            "SAVE" -> "Lưu bài viết 💾"
            else -> "Thông báo 🔔"
        }

        val systemNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationId = notification.id.hashCode()
        manager.notify(notificationId, systemNotification)
    }
}
