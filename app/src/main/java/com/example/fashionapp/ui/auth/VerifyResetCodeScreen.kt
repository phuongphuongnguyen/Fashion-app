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
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VerifyResetCodeScreen(
    authRepository: AuthRepository,
    email: String,
    onBack: () -> Unit,
    onOtpVerified: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var infoMessage by remember { mutableStateOf("") }
    var remainingSeconds by remember { mutableStateOf(60) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthBackground)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Verify OTP",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = AuthTextDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Nhap OTP 4 so da gui ve $email",
            color = AuthTextSubtle,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { code = it.filter(Char::isDigit).take(4) },
            placeholder = { Text("OTP code") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotBlank()) {
            Text(text = errorMessage, color = Color.Red, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (infoMessage.isNotBlank()) {
            Text(text = infoMessage, color = AuthPrimaryBlue, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                errorMessage = ""
                infoMessage = ""
                if (code.length != 4) {
                    errorMessage = "Vui long nhap OTP 4 so"
                    return@Button
                }
                scope.launch {
                    isLoading = true
                    val result = authRepository.verifyPasswordResetCode(email, code)
                    isLoading = false
                    if (result.isSuccess) {
                        onOtpVerified(code)
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
                CircularProgressIndicator(color = Color.White, modifier = Modifier.height(18.dp))
            } else {
                Text("Verify")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (remainingSeconds > 0) {
            Text(
                text = "Gui lai code sau ${remainingSeconds}s",
                color = AuthTextSubtle,
                fontSize = 12.sp
            )
        } else {
            Text(
                text = if (isResending) "Dang gui lai code..." else "Khong nhan duoc code? Gui lai",
                color = if (isResending) AuthTextSubtle else AuthPrimaryBlue,
                fontSize = 12.sp,
                modifier = Modifier.clickable(enabled = !isResending) {
                    scope.launch {
                        isResending = true
                        errorMessage = ""
                        val result = authRepository.sendPasswordResetCode(email)
                        isResending = false
                        if (result.isSuccess) {
                            infoMessage = result.message
                            remainingSeconds = 60
                        } else {
                            errorMessage = result.message
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Cancel",
            color = AuthTextSubtle,
            modifier = Modifier.clickable { onBack() }
        )
    }
}
