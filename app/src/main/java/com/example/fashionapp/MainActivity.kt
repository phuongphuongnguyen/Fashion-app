package com.example.fashionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.fashionapp.navigation.AppNavigation
import com.example.fashionapp.ui.theme.FashionAppTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FashionAppTheme {
                AppNavigation()
            }
        }
    }
}
