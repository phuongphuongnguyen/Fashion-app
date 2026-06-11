package com.example.fashionapp.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody


data class AuthResult(
    val isSuccess: Boolean,
    val message: String
)

interface AuthBackend {
    suspend fun register(
        email: String,
        password: String,
        phoneNumber: String
    ): AuthResult

    suspend fun login(email: String, password: String): AuthResult

    suspend fun sendPasswordResetCode(email: String): AuthResult

    suspend fun verifyPasswordResetCode(email: String, code: String): AuthResult

    suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String
    ): AuthResult
}

class FirebaseAuthBackend(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthBackend {
    override suspend fun register(
        email: String,
        password: String,
        phoneNumber: String
    ): AuthResult {
        return try {
            val normalizedEmail = email.trim()
            val authResult = auth.createUserWithEmailAndPassword(normalizedEmail, password).await()
            val uid = authResult.user?.uid
                ?: return AuthResult(false, "Khong tao duoc tai khoan, thu lai sau")

            firestore.collection("users")
                .document(uid)
                .set(
                    mapOf(
                        "email" to normalizedEmail,
                        "phoneNumber" to phoneNumber.trim()
                    )
                )
                .await()

            AuthResult(true, "Tao tai khoan thanh cong")
        } catch (e: FirebaseAuthException) {
            AuthResult(false, mapFirebaseAuthError(e.errorCode))
        } catch (_: Exception) {
            AuthResult(false, "Khong the ket noi den server, vui long thu lai")
        }
    }

    override suspend fun login(email: String, password: String): AuthResult {
        return try {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            AuthResult(true, "Dang nhap thanh cong")
        } catch (e: FirebaseAuthException) {
            AuthResult(false, mapFirebaseAuthError(e.errorCode))
        } catch (_: Exception) {
            AuthResult(false, "Khong the ket noi den server, vui long thu lai")
        }
    }

    override suspend fun sendPasswordResetCode(email: String): AuthResult {
        return try {
            val normalizedEmail = email.trim()

            val usersQuery = firestore.collection("users")
                .whereEqualTo("email", normalizedEmail)
                .get()
                .await()
            if (usersQuery.isEmpty) {
                return AuthResult(false, "Tai khoan khong ton tai")
            }

            val otp = (1000..9999).random().toString()
            val expiresAt = System.currentTimeMillis() + 60_000L

            firestore.collection("password_reset_otps")
                .document(normalizedEmail)
                .set(
                    mapOf(
                        "email" to normalizedEmail,
                        "otp" to otp,
                        "expiresAtMillis" to expiresAt,
                        "attemptCount" to 0,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            // Gọi API Brevo để gửi email trực tiếp chứa OTP
            val client = OkHttpClient()
            val apiKey = "" // Cần được thay thế bằng API Key của bạn
            val senderEmail = "" // Cần được thay thế bằng email đăng ký Brevo của bạn

            val jsonBody = """
                {
                  "sender": {
                    "name": "Fashion App Support",
                    "email": "$senderEmail"
                  },
                  "to": [
                    {
                      "email": "$normalizedEmail"
                    }
                  ],
                  "subject": "Fashion App - Mã OTP đặt lại mật khẩu",
                  "htmlContent": "<html><body><h3>Mã xác nhận OTP của bạn là: <b style='color:#007BFF;font-size:24px;'>$otp</b></h3><p>Mã này có hiệu lực trong vòng 60 giây.</p></body></html>"
                }
            """.trimIndent()

            val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://api.brevo.com/v3/smtp/email")
                .addHeader("api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            if (!response.isSuccessful) {
                return AuthResult(false, "Gui email that bai. Vui long kiem tra cau hinh API.")
            }

            AuthResult(true, "Da gui OTP 4 so ve email. Vui long nhap trong 60 giay.")
        } catch (e: FirebaseAuthException) {
            AuthResult(false, mapFirebaseAuthError(e.errorCode))
        } catch (e: Exception) {
            e.printStackTrace()
            AuthResult(false, "Loi: ${e.localizedMessage ?: e.message}")
        }
    }

    override suspend fun verifyPasswordResetCode(email: String, code: String): AuthResult {
        return try {
            val normalizedEmail = email.trim()
            val doc = firestore.collection("password_reset_otps")
                .document(normalizedEmail)
                .get()
                .await()
            if (!doc.exists()) {
                return AuthResult(false, "OTP het han hoac chua duoc gui")
            }

            val savedOtp = doc.getString("otp").orEmpty()
            val expiresAt = doc.getLong("expiresAtMillis") ?: 0L
            val attemptCount = (doc.getLong("attemptCount") ?: 0L).toInt()

            if (System.currentTimeMillis() > expiresAt) {
                firestore.collection("password_reset_otps").document(normalizedEmail).delete().await()
                return AuthResult(false, "OTP da het han")
            }

            if (attemptCount >= 5) {
                firestore.collection("password_reset_otps").document(normalizedEmail).delete().await()
                return AuthResult(false, "Ban da nhap sai qua nhieu lan")
            }

            if (savedOtp != code.trim()) {
                firestore.collection("password_reset_otps")
                    .document(normalizedEmail)
                    .update("attemptCount", attemptCount + 1)
                    .await()
                return AuthResult(false, "OTP khong dung")
            }

            AuthResult(true, "Xac nhan OTP thanh cong")
        } catch (_: Exception) {
            AuthResult(false, "Khong the ket noi den server, vui long thu lai")
        }
    }

    override suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String
    ): AuthResult {
        return try {
            val normalizedEmail = email.trim()
            val docRef = firestore.collection("password_reset_otps").document(normalizedEmail)
            val doc = docRef.get().await()
            if (!doc.exists()) {
                return AuthResult(false, "OTP het han hoac chua duoc gui")
            }

            val savedOtp = doc.getString("otp").orEmpty()
            val expiresAt = doc.getLong("expiresAtMillis") ?: 0L
            if (System.currentTimeMillis() > expiresAt) {
                docRef.delete().await()
                return AuthResult(false, "OTP da het han")
            }
            if (savedOtp != code.trim()) {
                return AuthResult(false, "OTP khong hop le")
            }

            // Xử lý đổi mật khẩu giả lập ở phía client bằng cách lưu vào Firestore users
            val usersQuery = firestore.collection("users")
                .whereEqualTo("email", normalizedEmail)
                .get()
                .await()

            if (!usersQuery.isEmpty) {
                val userDoc = usersQuery.documents.first()
                firestore.collection("users")
                    .document(userDoc.id)
                    .update("password", newPassword)
                    .await()
            }

            // Xóa mã OTP sau khi sử dụng thành công
            docRef.delete().await()

            AuthResult(true, "Dat lai mat khau thanh cong")
        } catch (e: Exception) {
            e.printStackTrace()
            AuthResult(false, "Loi: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun mapFirebaseAuthError(errorCode: String): String {
        return when (errorCode) {
            "ERROR_INVALID_EMAIL" -> "Email khong hop le"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email da ton tai"
            "ERROR_WEAK_PASSWORD" -> "Mat khau qua yeu, hay dat tu 6 ky tu tro len"
            "ERROR_USER_NOT_FOUND" -> "Tai khoan khong ton tai"
            "ERROR_WRONG_PASSWORD" -> "Mat khau khong dung"
            "ERROR_INVALID_CREDENTIAL" -> "Thong tin dang nhap khong hop le"
            else -> "Yeu cau that bai, vui long thu lai"
        }
    }
}

class AuthRepository(private val backend: AuthBackend) {
    suspend fun register(
        email: String,
        password: String,
        phoneNumber: String
    ): AuthResult = backend.register(email, password, phoneNumber)

    suspend fun login(email: String, password: String): AuthResult =
        backend.login(email, password)

    suspend fun sendPasswordResetCode(email: String): AuthResult =
        backend.sendPasswordResetCode(email)

    suspend fun verifyPasswordResetCode(email: String, code: String): AuthResult =
        backend.verifyPasswordResetCode(email, code)

    suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String
    ): AuthResult = backend.resetPassword(email, code, newPassword)
}
