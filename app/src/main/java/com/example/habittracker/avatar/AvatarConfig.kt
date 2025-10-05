package it.atraj.habittracker.avatar

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration manager for avatar upload feature
 * 
 * Handles secure storage and initialization of GitHub token
 */
@Singleton
class AvatarConfig @Inject constructor(
    @ApplicationContext private val context: Context,
    private val avatarManager: AvatarManager
) {
    
    companion object {
        private const val TAG = "AvatarConfig"
        private const val PREFS_NAME = "avatar_config"
        private const val KEY_GITHUB_TOKEN = "github_token"
        private const val KEY_UPLOAD_ENABLED = "upload_enabled"
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Initialize avatar upload feature with GitHub token
     * 
     * @param token GitHub Personal Access Token with 'repo' permissions
     */
    fun initialize(token: String) {
        try {
            // Store token securely (in production, use Android Keystore)
            prefs.edit()
                .putString(KEY_GITHUB_TOKEN, token)
                .putBoolean(KEY_UPLOAD_ENABLED, true)
                .apply()
            
            // Initialize uploader
            avatarManager.initializeGitHubUploader(token)
            
            Log.d(TAG, "Avatar upload feature initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize avatar config", e)
        }
    }
    
    /**
     * Check if upload feature is enabled
     */
    fun isUploadEnabled(): Boolean {
        return prefs.getBoolean(KEY_UPLOAD_ENABLED, false)
    }
    
    /**
     * Auto-initialize if token exists
     */
    fun autoInitialize() {
        val token = prefs.getString(KEY_GITHUB_TOKEN, null)
        if (token != null && token.isNotEmpty()) {
            avatarManager.initializeGitHubUploader(token)
            Log.d(TAG, "Avatar upload feature auto-initialized")
        }
    }
    
    /**
     * Disable upload feature and clear token
     */
    fun disable() {
        prefs.edit()
            .remove(KEY_GITHUB_TOKEN)
            .putBoolean(KEY_UPLOAD_ENABLED, false)
            .apply()
        
        Log.d(TAG, "Avatar upload feature disabled")
    }
    
    /**
     * Check if token is configured
     */
    fun hasToken(): Boolean {
        val token = prefs.getString(KEY_GITHUB_TOKEN, null)
        return !token.isNullOrEmpty()
    }
}
