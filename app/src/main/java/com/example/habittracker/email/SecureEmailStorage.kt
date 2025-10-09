package it.atraj.habittracker.email

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Securely stores email configuration using Android's EncryptedSharedPreferences.
 * This ensures that email credentials are encrypted at rest and not accessible
 * to other apps or through device backup.
 */
@Singleton
class SecureEmailStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * User's email address where notifications should be sent
     */
    var userEmail: String?
        get() = encryptedPrefs.getString(KEY_USER_EMAIL, null)
        set(value) = encryptedPrefs.edit().putString(KEY_USER_EMAIL, value).apply()

    /**
     * Whether email notifications are enabled
     */
    var emailNotificationsEnabled: Boolean
        get() = encryptedPrefs.getBoolean(KEY_EMAIL_ENABLED, false)
        set(value) = encryptedPrefs.edit().putBoolean(KEY_EMAIL_ENABLED, value).apply()

    /**
     * SMTP server configuration (Gmail by default)
     */
    var smtpHost: String
        get() = encryptedPrefs.getString(KEY_SMTP_HOST, DEFAULT_SMTP_HOST) ?: DEFAULT_SMTP_HOST
        set(value) = encryptedPrefs.edit().putString(KEY_SMTP_HOST, value).apply()

    var smtpPort: Int
        get() = encryptedPrefs.getInt(KEY_SMTP_PORT, DEFAULT_SMTP_PORT)
        set(value) = encryptedPrefs.edit().putInt(KEY_SMTP_PORT, value).apply()

    /**
     * Sender email credentials (from BuildConfig for security)
     * This is the actual Gmail account used for SMTP authentication
     */
    val senderEmail: String
        get() = it.atraj.habittracker.BuildConfig.SMTP_AUTH_EMAIL

    val senderPassword: String
        get() = it.atraj.habittracker.BuildConfig.EMAIL_APP_PASSWORD
    
    /**
     * From email address (can be an alias)
     * This is the email address that will appear in the "From" field
     */
    val fromEmail: String?
        get() = it.atraj.habittracker.BuildConfig.EMAIL_FROM_ADDRESS.takeIf { it.isNotBlank() }

    /**
     * Check if email configuration is complete
     */
    fun isConfigured(): Boolean {
        return !userEmail.isNullOrBlank() && 
               emailNotificationsEnabled && 
               senderEmail.isNotBlank() && 
               senderPassword.isNotBlank()
    }

    /**
     * Clear all email settings
     */
    fun clearSettings() {
        encryptedPrefs.edit()
            .remove(KEY_USER_EMAIL)
            .remove(KEY_EMAIL_ENABLED)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "secure_email_prefs"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_EMAIL_ENABLED = "email_enabled"
        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
        
        // Gmail SMTP configuration
        // Using port 465 (SSL) instead of 587 (STARTTLS) for better Android compatibility
        private const val DEFAULT_SMTP_HOST = "smtp.gmail.com"
        private const val DEFAULT_SMTP_PORT = 465
    }
}

