package com.example.habittracker.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app language selection and locale configuration
 */
@Singleton
class LanguageManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "language_prefs"
        private const val KEY_LANGUAGE = "selected_language"
        private const val DEFAULT_LANGUAGE = "en" // English
    }

    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Get current selected language code
     */
    fun getCurrentLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    /**
     * Set and apply new language
     */
    fun setLanguage(languageCode: String, restart: Boolean = false) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
        applyLanguage(context, languageCode)
    }

    /**
     * Apply language to context
     */
    fun applyLanguage(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            configuration.setLocales(localeList)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
            return context
        }
    }

    /**
     * Get all supported languages
     */
    fun getSupportedLanguages(): List<Language> {
        return listOf(
            Language("en", "English", "🇬🇧"),
            Language("hi", "हिंदी (Hindi)", "🇮🇳"),
            Language("ar", "العربية (Arabic)", "🇸🇦"),
            Language("bn", "বাংলা (Bengali)", "🇧🇩"),
            Language("ja", "日本語 (Japanese)", "🇯🇵")
        )
    }

    /**
     * Get current language object
     */
    fun getCurrentLanguageObject(): Language {
        val currentCode = getCurrentLanguage()
        return getSupportedLanguages().find { it.code == currentCode } 
            ?: Language("en", "English", "🇬🇧")
    }
}

/**
 * Represents a supported language
 */
data class Language(
    val code: String,      // ISO language code (en, hi, ar, etc.)
    val name: String,      // Display name
    val flag: String       // Flag emoji
)
