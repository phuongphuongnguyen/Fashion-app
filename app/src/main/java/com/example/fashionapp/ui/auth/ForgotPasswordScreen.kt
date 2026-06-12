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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fashionapp.data.auth.AuthRepository
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onCodeSent: (String) -> Unit
) {
    val settings = LocalAppSettings.current
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthBackground)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = settings.t("Password Recovery", "Khôi phục mật khẩu"),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = AuthTextDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = settings.t(
                "Enter email to receive reset password verification code",
                "Nhập email để nhận mã xác nhận đặt lại mật khẩu"
            ),
            color = AuthTextSubtle,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text(settings.t("Email", "Email")) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
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

        if (successMessage.isNotBlank()) {
            Text(
                text = successMessage,
                color = AuthPrimaryBlue,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                errorMessage = ""
                successMessage = ""
                if (email.isBlank()) {
                    errorMessage = settings.t("Please enter your email", "Vui lòng nhập email")
                    return@Button
                }

                scope.launch {
                    isLoading = true
                    val normalizedEmail = email.trim()
                    val result = authRepository.sendPasswordResetCode(normalizedEmail)
                    isLoading = false

                    if (result.isSuccess) {
                        successMessage = result.message
                        onCodeSent(normalizedEmail)
                    } else {
                        errorMessage = result.message
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
                Text(settings.t("Send Code", "Gửi mã"))
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
