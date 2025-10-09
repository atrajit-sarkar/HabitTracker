package it.atraj.habittracker.email

import android.util.Log
import it.atraj.habittracker.data.local.Habit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to send email notifications via Gmail SMTP.
 * Uses custom Android-compatible SMTP client.
 * Note: Email clients don't support Base64 images, so we use emoji fallbacks.
 */
@Singleton
class EmailNotificationService @Inject constructor(
    private val secureEmailStorage: SecureEmailStorage
) {
    
    /**
     * Send a habit reminder email to the user.
     * This runs on IO dispatcher to avoid blocking the main thread.
     * 
     * @param habit The habit to send a reminder for
     * @param userName Optional user's display name for personalization
     * @return Result indicating success or failure
     */
    suspend fun sendHabitReminderEmail(
        habit: Habit,
        userName: String? = null
    ): EmailResult = withContext(Dispatchers.IO) {
        try {
            // Check if email is configured
            if (!secureEmailStorage.isConfigured()) {
                Log.w(TAG, "Email not configured, skipping email notification")
                return@withContext EmailResult.NotConfigured
            }
            
            val recipientEmail = secureEmailStorage.userEmail ?: return@withContext EmailResult.NoRecipient
            
            // Create deep link to habit details
            val deepLink = createHabitDeepLink(habit.id)
            
            // Generate email content with smart emoji fallback
            val subject = EmailTemplate.generateSubject(habit)
            val htmlBody = EmailTemplate.generateHabitReminderEmail(habit, userName, deepLink)
            val textBody = EmailTemplate.generatePlainTextEmail(habit, userName, deepLink)
            
            // Send email
            sendEmail(
                recipientEmail = recipientEmail,
                subject = subject,
                htmlBody = htmlBody,
                textBody = textBody
            )
            
            Log.i(TAG, "Email sent successfully for habit: ${habit.title}")
            EmailResult.Success
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send email for habit: ${habit.title}", e)
            EmailResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Send an email using Gmail SMTP
     */
    private fun sendEmail(
        recipientEmail: String,
        subject: String,
        htmlBody: String,
        textBody: String
    ) {
        // Use custom Android-compatible SMTP client
        val client = AndroidSMTPClient(
            host = "smtp.gmail.com",
            port = 587,
            username = secureEmailStorage.senderEmail,
            password = secureEmailStorage.senderPassword
        )
        
        // Determine from address (use alias if configured)
        val fromAddress = secureEmailStorage.fromEmail ?: secureEmailStorage.senderEmail
        
        // Send the email
        client.sendEmail(
            from = fromAddress,
            fromName = "Habit Tracker",
            to = recipientEmail,
            subject = subject,
            htmlBody = htmlBody,
            textBody = textBody
        )
    }
    
    
    /**
     * Create a deep link to open the habit details screen in the app
     * Using HTTPS URL for better email client compatibility (Gmail blocks custom schemes)
     */
    private fun createHabitDeepLink(habitId: Long): String {
        // Use HTTPS deep link with habittracker subdomain
        // This works better in email clients like Gmail
        return "https://habittracker.atraj.it/habit/$habitId"
    }
    
    companion object {
        private const val TAG = "EmailNotificationService"
    }
}

/**
 * Result of email sending operation
 */
sealed class EmailResult {
    object Success : EmailResult()
    object NotConfigured : EmailResult()
    object NoRecipient : EmailResult()
    data class Error(val message: String) : EmailResult()
}

