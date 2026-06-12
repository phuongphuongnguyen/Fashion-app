package com.example.fashionapp.data.onboarding

import android.content.Context

class OnboardingPreferences(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isOnboardingCompleted(uid: String): Boolean {
        if (uid.isEmpty()) return true
        return prefs.getBoolean(KEY_PREFIX + uid, false)
    }

    fun markOnboardingCompleted(uid: String) {
        if (uid.isEmpty()) return
        prefs.edit().putBoolean(KEY_PREFIX + uid, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "fashion_app_onboarding"
        private const val KEY_PREFIX = "onboarding_done_"
    }
}
