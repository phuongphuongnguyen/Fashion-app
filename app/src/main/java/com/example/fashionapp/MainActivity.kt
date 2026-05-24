package com.example.fashionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import com.example.fashionapp.navigation.AppNavigation
import com.example.fashionapp.navigation.Screen
import com.example.fashionapp.ui.app.settings.AppSettingsViewModel
import com.example.fashionapp.ui.app.settings.AppSettingsViewModelFactory
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.theme.FashionAppTheme

class MainActivity : ComponentActivity(), ImageLoaderFactory {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: AppSettingsViewModel = viewModel(
                factory = AppSettingsViewModelFactory(applicationContext)
            )
            CompositionLocalProvider(LocalAppSettings provides settingsViewModel) {
                FashionAppTheme(darkTheme = settingsViewModel.isDarkMode) {
                    AppNavigation(startDestination = Screen.Login.route)
                }
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }
}
