package com.example.habittracker.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LanguageManager {
    private const val PREF_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    
    // Supported languages
    enum class Language(val code: String, val displayName: String, val nativeName: String, val flagEmoji: String) {
        ENGLISH("en", "English", "English", "🇬🇧"),
        ARABIC("ar", "Arabic", "العربية", "🇸🇦")
    }
    
    fun setLanguage(context: Context, language: Language) {
        // Save preference
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        
        // Apply language using AppCompatDelegate for instant change
        val localeList = LocaleListCompat.forLanguageTags(language.code)
        AppCompatDelegate.setApplicationLocales(localeList)
        
        // Force activity recreation for immediate effect
        if (context is Activity) {
            context.recreate()
        }
    }
    
    fun getCurrentLanguage(context: Context): Language {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val languageCode = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        return Language.values().find { it.code == languageCode } ?: Language.ENGLISH
    }
    
    fun getAvailableLanguages(): List<Language> {
        return Language.values().toList()
    }
    
    fun applyLanguage(context: Context) {
        val language = getCurrentLanguage(context)
        val localeList = LocaleListCompat.forLanguageTags(language.code)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
    
    fun isRTL(language: Language): Boolean {
        return language == Language.ARABIC
    }
}
