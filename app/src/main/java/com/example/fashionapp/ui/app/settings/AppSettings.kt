package com.example.fashionapp.ui.app.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// ── Supported Languages ──
enum class AppLanguage(val code: String, val username: String, val nativeName: String, val flag: String) {
    ENGLISH("en", "English", "English", "🇬🇧"),
    VIETNAMESE("vi", "Vietnamese", "Tiếng Việt", "🇻🇳"),
    FRENCH("fr", "French", "Français", "🇫🇷"),
    JAPANESE("ja", "Japanese", "日本語", "🇯🇵"),
    KOREAN("ko", "Korean", "한국어", "🇰🇷"),
    CHINESE("zh", "Chinese", "中文", "🇨🇳")
}

// ── Supported Currencies ──
enum class AppCurrency(val code: String, val symbol: String, val username: String) {
    USD("USD", "$", "US Dollar"),
    VND("VND", "₫", "Vietnamese Dong"),
    EUR("EUR", "€", "Euro"),
    JPY("JPY", "¥", "Japanese Yen"),
    KRW("KRW", "₩", "Korean Won"),
    GBP("GBP", "£", "British Pound")
}

// ── Size Systems ──
enum class SizeSystem(val label: String) {
    UK("UK"),
    US("US"),
    EU("EU"),
    ASIAN("Asian")
}

// ── Countries ──
val countryList = listOf(
    "Vietnam", "United States", "United Kingdom", "France", "Japan",
    "South Korea", "China", "Germany", "Australia", "Canada"
)

// ── Settings ViewModel ──
class AppSettingsViewModel(private val prefs: SharedPreferences) : ViewModel() {

    var language by mutableStateOf(
        AppLanguage.values().find { it.code == prefs.getString("language", "en") }
            ?: AppLanguage.ENGLISH
    )
        private set

    var isDarkMode by mutableStateOf(prefs.getBoolean("dark_mode", false))
        private set

    var currency by mutableStateOf(
        AppCurrency.values().find { it.code == prefs.getString("currency", "USD") }
            ?: AppCurrency.USD
    )
        private set

    var sizeSystem by mutableStateOf(
        SizeSystem.values().find { it.label == prefs.getString("size_system", "UK") }
            ?: SizeSystem.UK
    )
        private set

    var country by mutableStateOf(prefs.getString("country", "Vietnam") ?: "Vietnam")
        private set

    var notificationsEnabled by mutableStateOf(prefs.getBoolean("notifications", true))
        private set

    var orderUpdatesEnabled by mutableStateOf(prefs.getBoolean("order_updates", true))
        private set

    var promotionsEnabled by mutableStateOf(prefs.getBoolean("promotions", false))
        private set

    fun updateLanguage(lang: AppLanguage) {
        language = lang
        prefs.edit().putString("language", lang.code).apply()
    }

    fun updateDarkMode(enabled: Boolean) {
        isDarkMode = enabled
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun updateCurrency(cur: AppCurrency) {
        currency = cur
        prefs.edit().putString("currency", cur.code).apply()
    }

    fun updateSizeSystem(size: SizeSystem) {
        sizeSystem = size
        prefs.edit().putString("size_system", size.label).apply()
    }

    fun updateCountry(c: String) {
        country = c
        prefs.edit().putString("country", c).apply()
    }

    fun updateNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
        prefs.edit().putBoolean("notifications", enabled).apply()
    }

    fun updateOrderUpdates(enabled: Boolean) {
        orderUpdatesEnabled = enabled
        prefs.edit().putBoolean("order_updates", enabled).apply()
    }

    fun updatePromotions(enabled: Boolean) {
        promotionsEnabled = enabled
        prefs.edit().putBoolean("promotions", enabled).apply()
    }

    // Translate helper
    fun t(en: String, vi: String): String = if (language == AppLanguage.VIETNAMESE) vi else en
}

class AppSettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        @Suppress("UNCHECKED_CAST")
        return AppSettingsViewModel(prefs) as T
    }
}

// ── Composition Local ──
val LocalAppSettings = compositionLocalOf<AppSettingsViewModel> {
    error("No AppSettings provided")
}
