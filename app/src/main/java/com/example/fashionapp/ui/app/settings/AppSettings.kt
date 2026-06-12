package com.example.fashionapp.ui.app.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth

// ── Supported Languages ──
enum class AppLanguage(val code: String, val displayName: String, val nativeName: String, val flag: String) {
    ENGLISH("en", "English", "English", "🇬🇧"),
    VIETNAMESE("vi", "Vietnamese", "Tiếng Việt", "🇻🇳"),
    FRENCH("fr", "French", "Français", "🇫🇷"),
    JAPANESE("ja", "Japanese", "日本語", "🇯🇵"),
    KOREAN("ko", "Korean", "한국어", "🇰🇷"),
    CHINESE("zh", "Chinese", "中文", "🇨🇳")
}

// ── Supported Currencies ──
enum class AppCurrency(val code: String, val symbol: String, val displayName: String) {
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
    private val auth = FirebaseAuth.getInstance()

    var language by mutableStateOf(AppLanguage.ENGLISH)
        private set

    var isDarkMode by mutableStateOf(false)
        private set

    var currency by mutableStateOf(AppCurrency.USD)
        private set

    var sizeSystem by mutableStateOf(SizeSystem.UK)
        private set

    var country by mutableStateOf("Vietnam")
        private set

    var notificationsEnabled by mutableStateOf(true)
        private set

    var systemNotificationsEnabled by mutableStateOf(true)
        private set

    var orderUpdatesEnabled by mutableStateOf(true)
        private set

    var socialInteractionsEnabled by mutableStateOf(true)
        private set

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val uid = firebaseAuth.currentUser?.uid.orEmpty()
        loadUserSettings(uid)
    }

    init {
        auth.addAuthStateListener(authStateListener)
        loadUserSettings(auth.currentUser?.uid.orEmpty())
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authStateListener)
        super.onCleared()
    }

    private fun loadUserSettings(userId: String) {
        val prefix = if (userId.isBlank()) "" else "${userId}_"

        language = AppLanguage.values().find { it.code == prefs.getString("${prefix}language", prefs.getString("language", "en")) }
            ?: AppLanguage.ENGLISH

        isDarkMode = prefs.getBoolean("${prefix}dark_mode", prefs.getBoolean("dark_mode", false))

        currency = AppCurrency.values().find { it.code == prefs.getString("${prefix}currency", prefs.getString("currency", "USD")) }
            ?: AppCurrency.USD

        sizeSystem = SizeSystem.values().find { it.label == prefs.getString("${prefix}size_system", prefs.getString("size_system", "UK")) }
            ?: SizeSystem.UK

        country = prefs.getString("${prefix}country", prefs.getString("country", "Vietnam")) ?: "Vietnam"

        notificationsEnabled = prefs.getBoolean("${prefix}notifications", prefs.getBoolean("notifications", true))

        systemNotificationsEnabled = prefs.getBoolean("${prefix}system_notifications", prefs.getBoolean("system_notifications", true))

        orderUpdatesEnabled = prefs.getBoolean("${prefix}order_updates", prefs.getBoolean("order_updates", true))

        socialInteractionsEnabled = prefs.getBoolean("${prefix}social_interactions", prefs.getBoolean("social_interactions", true))
    }

    private fun getPrefix(): String {
        val uid = auth.currentUser?.uid.orEmpty()
        return if (uid.isBlank()) "" else "${uid}_"
    }

    fun updateLanguage(lang: AppLanguage) {
        language = lang
        prefs.edit().putString("${getPrefix()}language", lang.code).apply()
    }

    fun updateDarkMode(enabled: Boolean) {
        isDarkMode = enabled
        prefs.edit().putBoolean("${getPrefix()}dark_mode", enabled).apply()
    }

    fun updateCurrency(cur: AppCurrency) {
        currency = cur
        prefs.edit().putString("${getPrefix()}currency", cur.code).apply()
    }

    fun updateSizeSystem(size: SizeSystem) {
        sizeSystem = size
        prefs.edit().putString("${getPrefix()}size_system", size.label).apply()
    }

    fun updateCountry(c: String) {
        country = c
        prefs.edit().putString("${getPrefix()}country", c).apply()
    }

    fun updateNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
        prefs.edit().putBoolean("${getPrefix()}notifications", enabled).apply()
    }

    fun updateSystemNotifications(enabled: Boolean) {
        systemNotificationsEnabled = enabled
        prefs.edit().putBoolean("${getPrefix()}system_notifications", enabled).apply()
    }

    fun updateOrderUpdates(enabled: Boolean) {
        orderUpdatesEnabled = enabled
        prefs.edit().putBoolean("${getPrefix()}order_updates", enabled).apply()
    }

    fun updateSocialInteractions(enabled: Boolean) {
        socialInteractionsEnabled = enabled
        prefs.edit().putBoolean("${getPrefix()}social_interactions", enabled).apply()
    }

    // Translate helper
    fun t(
        en: String,
        vi: String = en,
        fr: String = en,
        ja: String = en,
        ko: String = en,
        zh: String = en
    ): String {
        return when (language) {
            AppLanguage.VIETNAMESE -> vi
            AppLanguage.FRENCH -> fr
            AppLanguage.JAPANESE -> ja
            AppLanguage.KOREAN -> ko
            AppLanguage.CHINESE -> zh
            else -> en
        }
    }
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
