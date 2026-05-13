package com.example.fashionapp.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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

            val methods = auth.fetchSignInMethodsForEmail(normalizedEmail).await().signInMethods
            if (methods.isNullOrEmpty()) {
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

            firestore.collection("mail")
                .add(
                    mapOf(
                        "to" to normalizedEmail,
                        "message" to mapOf(
                            "subject" to "Fashion App OTP",
                            "text" to "Ma OTP cua ban la: $otp. Ma co hieu luc trong 60 giay."
                        )
                    )
                )
                .await()

            AuthResult(true, "Da gui OTP 4 so ve email. Vui long nhap trong 60 giay.")
        } catch (e: FirebaseAuthException) {
            AuthResult(false, mapFirebaseAuthError(e.errorCode))
        } catch (_: Exception) {
            AuthResult(false, "Khong the ket noi den server, vui long thu lai")
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

            // Firebase client SDK cannot reset another account's password by email+OTP.
            // Keep user on secure flow until backend is available.
            AuthResult(false, "Da xac thuc OTP. Tam thoi chua doi duoc mat khau neu khong co backend.")
        } catch (_: Exception) {
            AuthResult(false, "Khong the ket noi den server, vui long thu lai")
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
