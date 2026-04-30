package com.example.fashionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.fashionapp.navigation.AppNavigation
import com.example.fashionapp.ui.theme.FashionAppTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable

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

@Preview(showBackground = true)
@Composable
fun PreviewApp() {
    FashionAppTheme {
        AppNavigation()
    }
}
