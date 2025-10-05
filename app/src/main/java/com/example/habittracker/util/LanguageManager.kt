package it.atraj.habittracker.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
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
        
        // Apply locale change
        setLocale(context, language.code)
        
        // Force activity recreation for immediate effect
        if (context is Activity) {
            context.recreate()
        }
    }
    
    private fun setLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            configuration.setLocales(android.os.LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        }
        
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
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
        setLocale(context, language.code)
    }
    
    fun isRTL(language: Language): Boolean {
        return language == Language.ARABIC
    }
}
