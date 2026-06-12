
package com.example.fashionapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val brand: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val star: Color,
    val success: Color,
    val danger: Color,
)

val LightAppColors = AppColors(
    brand = BluePrimary, card = LightCard,
    textPrimary = LightTextPrimary, textSecondary = LightTextSecondary,
    star = StarYellow, success = GreenSuccess, danger = RedDanger,
)

val DarkAppColors = AppColors(
    brand = BluePrimary, card = DarkCard,
    textPrimary = DarkTextPrimary, textSecondary = DarkTextSecondary,
    star = StarYellow, success = GreenSuccess, danger = RedDanger,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

// Object để gọi gọn: AppTheme.colors.textSecondary
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current
}
