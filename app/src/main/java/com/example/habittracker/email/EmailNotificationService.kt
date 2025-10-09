package it.atraj.habittracker.email

import android.util.Log
import it.atraj.habittracker.data.local.Habit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Service to send email notifications via Gmail SMTP.
 * Uses JavaMail API with TLS encryption.
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
            
            // Generate email content
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
        // Use Gmail SMTP SSL (port 465) instead of STARTTLS (port 587)
        // SSL is more reliable on Android than STARTTLS
        val props = Properties().apply {
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "465") // SSL port
            put("mail.smtp.auth", "true")
            
            // Enable SSL
            put("mail.smtp.ssl.enable", "true")
            put("mail.smtp.ssl.trust", "smtp.gmail.com")
            put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
            
            // Socket factory for SSL
            put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            put("mail.smtp.socketFactory.port", "465")
            put("mail.smtp.socketFactory.fallback", "false")
            
            // Disable STARTTLS (we're using SSL instead)
            put("mail.smtp.starttls.enable", "false")
            
            // Timeout settings
            put("mail.smtp.connectiontimeout", "30000") // 30 seconds
            put("mail.smtp.timeout", "30000") // 30 seconds
            put("mail.smtp.writetimeout", "30000") // 30 seconds
            
            // Debug logging
            put("mail.debug", "true")
        }
        
        // Create authenticated session
        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(
                    secureEmailStorage.senderEmail,
                    secureEmailStorage.senderPassword
                )
            }
        })
        
        // Enable debug mode in development
        session.debug = false // Set to true for debugging
        
        // Create message
        val message = MimeMessage(session).apply {
            // Use alias email if configured, otherwise use sender email
            val fromAddress = secureEmailStorage.fromEmail ?: secureEmailStorage.senderEmail
            setFrom(InternetAddress(fromAddress, "Habit Tracker"))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
            setSubject(subject, "UTF-8")
            
            // Set both HTML and plain text content for better compatibility
            setContent(createMultipartContent(htmlBody, textBody))
        }
        
        // Send the message
        Transport.send(message)
    }
    
    /**
     * Create multipart content with both HTML and plain text versions
     */
    private fun createMultipartContent(htmlBody: String, textBody: String): javax.mail.Multipart {
        val multipart = javax.mail.internet.MimeMultipart("alternative")
        
        // Add plain text part (lower priority)
        val textPart = javax.mail.internet.MimeBodyPart().apply {
            setText(textBody, "UTF-8")
        }
        multipart.addBodyPart(textPart)
        
        // Add HTML part (higher priority)
        val htmlPart = javax.mail.internet.MimeBodyPart().apply {
            setContent(htmlBody, "text/html; charset=UTF-8")
        }
        multipart.addBodyPart(htmlPart)
        
        return multipart
    }
    
    /**
     * Create a deep link to open the habit details screen in the app
     */
    private fun createHabitDeepLink(habitId: Long): String {
        // Android App Links format
        // This will open the app directly to the habit details screen
        return "habittracker://habit/$habitId"
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

