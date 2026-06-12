
package com.example.fashionapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val brand: Color,
    val card: Color,
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val selectionBg: Color,
    val star: Color,
    val success: Color,
    val danger: Color,
)

val LightAppColors = AppColors(
    brand = BluePrimary, card = LightCard,
    background = Color(0xFFF7F8FA), surface = Color.White,
    textPrimary = LightTextPrimary, textSecondary = LightTextSecondary,
    divider = Color(0xFFEEEEEE), selectionBg = Color(0xFFEEF2FF),
    star = StarYellow, success = GreenSuccess, danger = RedDanger,
)

val DarkAppColors = AppColors(
    brand = BluePrimary, card = DarkCard,
    background = Color(0xFF121212), surface = Color(0xFF1E1E1E),
    textPrimary = DarkTextPrimary, textSecondary = DarkTextSecondary,
    divider = Color(0xFF2C2C2C), selectionBg = Color(0xFF1D2D50),
    star = StarYellow, success = GreenSuccess, danger = RedDanger,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

// Object để gọi gọn: AppTheme.colors.textSecondary
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current
}
