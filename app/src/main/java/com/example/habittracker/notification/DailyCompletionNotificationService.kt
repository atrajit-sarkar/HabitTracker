package it.atraj.habittracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import it.atraj.habittracker.MainActivity
import it.atraj.habittracker.R
import it.atraj.habittracker.gemini.GeminiApiService
import it.atraj.habittracker.gemini.GeminiPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for showing daily completion notification at 11:50 PM
 * Congratulates user with Gemini-generated message when all habits are completed
 */
object DailyCompletionNotificationService {
    
    private const val TAG = "DailyCompletionNotification"
    private const val CHANNEL_ID = "daily_completion_channel"
    private const val NOTIFICATION_ID = 999999 // Unique ID for daily completion
    
    /**
     * Show daily completion notification with personalized message
     */
    suspend fun showCompletionNotification(
        context: Context,
        userName: String?,
        completedCount: Int
    ) = withContext(Dispatchers.IO) {
        try {
            // Ensure channel exists
            ensureCompletionChannel(context)
            
            // Generate personalized congratulatory message
            val message = generateCompletionMessage(context, userName, completedCount)
            
            // Load the "done" image
            val doneImage = BitmapFactory.decodeResource(context.resources, R.drawable.done)
            
            // Build and show notification
            val notification = buildCompletionNotification(
                context = context,
                message = message,
                bigPicture = doneImage,
                completedCount = completedCount
            )
            
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(NOTIFICATION_ID, notification.build())
                Log.d(TAG, "Daily completion notification shown: $completedCount habits completed")
            } else {
                Log.w(TAG, "Notifications are disabled by user")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing completion notification", e)
        }
    }
    
    /**
     * Generate personalized completion message using Gemini API or fallback
     */
    private suspend fun generateCompletionMessage(
        context: Context,
        userName: String?,
        completedCount: Int
    ): String = withContext(Dispatchers.IO) {
        try {
            val geminiPrefs = GeminiPreferences(context)
            val apiKey = geminiPrefs.getApiKey()
            
            if (!apiKey.isNullOrBlank() && !userName.isNullOrBlank()) {
                // Generate personalized message with Gemini
                val geminiService = GeminiApiService(apiKey)
                val result = geminiService.generateCompletionGoodnightMessage(userName, completedCount)
                
                if (result.isSuccess) {
                    val generatedMessage = result.getOrNull()
                    if (!generatedMessage.isNullOrBlank()) {
                        Log.d(TAG, "Generated personalized completion message with Gemini")
                        return@withContext generatedMessage
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating Gemini message, using fallback", e)
        }
        
        // Fallback message
        val userPrefix = if (!userName.isNullOrBlank()) "$userName, " else ""
        return@withContext "${userPrefix}Congratulations! You've completed all $completedCount habits today. Great work! Rest well and come back tomorrow to continue your amazing streak. Good night!"
    }
    
    /**
     * Build daily completion notification with BigPictureStyle
     */
    private fun buildCompletionNotification(
        context: Context,
        message: String,
        bigPicture: Bitmap,
        completedCount: Int
    ): NotificationCompat.Builder {
        
        // Intent to open app
        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification with BigPictureStyle
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_habit)
            .setColor(ContextCompat.getColor(context, R.color.purple_500))
            .setContentTitle("🎉 All Habits Completed!")
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bigPicture)
                    .bigLargeIcon(null as Bitmap?)
                    .setBigContentTitle("🎉 Perfect Day - $completedCount Habits Done!")
                    .setSummaryText(message)
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
    }
    
    /**
     * Ensure notification channel exists for daily completion
     */
    private fun ensureCompletionChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
                ?: return
            
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Daily Completion",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Congratulations when you complete all daily habits"
                    enableLights(true)
                    lightColor = Color.GREEN
                    enableVibration(true)
                    setShowBadge(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    
                    // Use a pleasant sound
                    setSound(
                        android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                        android.media.AudioAttributes.Builder()
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                    )
                }
                manager.createNotificationChannel(channel)
                Log.d(TAG, "Created daily completion channel")
            }
        }
    }
    
    /**
     * Dismiss daily completion notification
     */
    fun dismissCompletionNotification(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID)
        Log.d(TAG, "Dismissed daily completion notification")
    }
}
