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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fashionapp.data.auth.AuthRepository
import com.example.fashionapp.data.user.UserRepository
import com.example.fashionapp.data.user.UserSession
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val userRepository = remember { UserRepository() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
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
            text = "Login",
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            color = AuthTextDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Good to see you back",
            color = AuthTextSubtle,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) {
                    Icons.Filled.Visibility
                } else {
                    Icons.Filled.VisibilityOff
                }
                val description = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        )

//        Spacer(modifier = Modifier.height(16.dp))

//        Text(
//            text = "Forgot password?",
//            color = AuthPrimaryBlue,
//            modifier = Modifier.clickable { onForgotPasswordClick() }
//        )

        Spacer(modifier = Modifier.height(12.dp))

        if (errorMessage.isNotBlank()) {
            Text(
                text = errorMessage,
                color = androidx.compose.ui.graphics.Color.Red,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                errorMessage = ""
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Vui long nhap email va mat khau"
                    return@Button
                }
                scope.launch {
                    isLoading = true
                    val result = authRepository.login(
                        email = email.trim(),
                        password = password
                    )
                    if (result.isSuccess) {
                        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            val user = userRepository.getUserProfile(uid)
                            UserSession.updateCurrentUser(user)
                        }
                        isLoading = false
                        onLoginSuccess()
                    } else {
                        isLoading = false
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
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.height(18.dp)
                )
            } else {
                Text("Next")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Forgot password?",
            color = AuthPrimaryBlue,
            modifier = Modifier.clickable { onForgotPasswordClick() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Create account",
            color = AuthPrimaryBlue,
            modifier = Modifier.clickable { onCreateAccountClick() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Cancel",
            color = AuthTextSubtle,
            modifier = Modifier.clickable { onBack() }
        )
    }
}
