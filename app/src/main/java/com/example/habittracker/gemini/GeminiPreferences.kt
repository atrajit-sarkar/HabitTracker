package it.atraj.habittracker.gemini

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Log

/**
 * Secure storage for Gemini API key using EncryptedSharedPreferences
 */
class GeminiPreferences(context: Context) {
    
    companion object {
        private const val TAG = "GeminiPreferences"
        private const val PREFS_NAME = "gemini_secure_prefs"
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_ENABLED = "gemini_enabled"
    }
    
    private val sharedPreferences: SharedPreferences by lazy {
        try {
            // Create or retrieve the master key for encryption
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            // Create encrypted shared preferences
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating encrypted preferences, falling back to regular preferences", e)
            // Fallback to regular shared preferences if encryption fails
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    /**
     * Save Gemini API key
     */
    fun saveApiKey(apiKey: String) {
        try {
            sharedPreferences.edit()
                .putString(KEY_API_KEY, apiKey)
                .apply()
            Log.d(TAG, "API key saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving API key", e)
        }
    }
    
    /**
     * Get stored Gemini API key
     */
    fun getApiKey(): String? {
        return try {
            sharedPreferences.getString(KEY_API_KEY, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving API key", e)
            null
        }
    }
    
    /**
     * Check if API key is configured
     */
    fun isApiKeyConfigured(): Boolean {
        val key = getApiKey()
        return !key.isNullOrBlank()
    }
    
    /**
     * Delete stored API key
     */
    fun clearApiKey() {
        try {
            sharedPreferences.edit()
                .remove(KEY_API_KEY)
                .apply()
            Log.d(TAG, "API key cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing API key", e)
        }
    }
    
    /**
     * Enable/disable Gemini features
     */
    fun setGeminiEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
    
    /**
     * Check if Gemini features are enabled
     */
    fun isGeminiEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_ENABLED, true)
    }
}
