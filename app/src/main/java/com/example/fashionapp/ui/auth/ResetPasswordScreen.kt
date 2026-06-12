package com.example.fashionapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.example.fashionapp.data.auth.AuthRepository
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import kotlinx.coroutines.launch

// Màn hình thiết lập mật khẩu mới cho tài khoản sau khi người dùng xác thực thành công mã OTP khôi phục
@Composable
fun ResetPasswordScreen(
    authRepository: AuthRepository,
    email: String,
    verifiedCode: String,
    onBack: () -> Unit,
    onPasswordResetSuccess: () -> Unit
) {
    val settings = LocalAppSettings.current
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthBackground)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = settings.t("Setup New Password", "Thiết lập mật khẩu mới"),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = AuthTextDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = settings.t("Set new password for ", "Đặt mật khẩu mới cho ") + email,
            color = AuthTextSubtle,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            placeholder = { Text(settings.t("New Password", "Mật khẩu mới")) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (newPasswordVisible) {
                    Icons.Filled.Visibility
                } else {
                    Icons.Filled.VisibilityOff
                }
                val description = if (newPasswordVisible) settings.t("Hide password", "Ẩn mật khẩu") else settings.t("Show password", "Hiện mật khẩu")

                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder = { Text(settings.t("Repeat Password", "Nhập lại mật khẩu")) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (confirmPasswordVisible) {
                    Icons.Filled.Visibility
                } else {
                    Icons.Filled.VisibilityOff
                }
                val description = if (confirmPasswordVisible) settings.t("Hide password", "Ẩn mật khẩu") else settings.t("Show password", "Hiện mật khẩu")

                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotBlank()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                errorMessage = ""

                when {
                    newPassword.length < 6 -> errorMessage = settings.t("New password must be at least 6 characters", "Mật khẩu mới tối thiểu 6 ký tự")
                    newPassword != confirmPassword -> errorMessage = settings.t("Passwords do not match", "Mật khẩu nhập lại không khớp")
                    else -> {
                        scope.launch {
                            isLoading = true
                            val result = authRepository.resetPassword(
                                email = email,
                                code = verifiedCode,
                                newPassword = newPassword
                            )
                            isLoading = false

                            if (result.isSuccess) {
                                onPasswordResetSuccess()
                            } else {
                                errorMessage = result.message
                            }
                        }
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AuthPrimaryBlue)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.height(18.dp)
                )
            } else {
                Text(settings.t("Save", "Lưu"))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = settings.t("Cancel", "Hủy"),
            color = AuthTextSubtle,
            modifier = Modifier.clickable { onBack() }
        )
    }
}
