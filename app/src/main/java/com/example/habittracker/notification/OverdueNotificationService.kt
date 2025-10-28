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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import it.atraj.habittracker.MainActivity
import it.atraj.habittracker.R
import it.atraj.habittracker.data.local.Habit
import it.atraj.habittracker.gemini.GeminiApiService
import it.atraj.habittracker.gemini.GeminiPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * Service for showing overdue habit notifications with Gemini-generated messages
 * Uses BigPictureStyle for large notification images
 */
object OverdueNotificationService {
    
    private const val TAG = "OverdueNotificationService"
    private const val CHANNEL_ID_PREFIX = "overdue_habit_channel_"
    private const val NOTIFICATION_ID_PREFIX = 100000 // Use high IDs to avoid conflicts with regular reminders
    
    // Map overdue hours to drawable resources
    private val overdueImageMap = mapOf(
        2 to R.drawable.overdue_2hour,
        3 to R.drawable.overdue_3hour,
        4 to R.drawable.overdue_4hour,
        5 to R.drawable.overdue_5hour,
        6 to R.drawable.overdue_6hour
    )
    
    /**
     * Show overdue notification for a habit with Gemini-generated message
     */
    suspend fun showOverdueNotification(
        context: Context,
        habit: Habit,
        overdueHours: Int,
        userName: String?
    ) = withContext(Dispatchers.IO) {
        try {
            // Ensure channel exists
            val channelId = ensureOverdueChannel(context, habit)
            
            // Get or generate personalized message (pass description for aggressive 6+ hour messages)
            val message = generateOverdueMessage(
                context = context,
                habitTitle = habit.title,
                overdueHours = overdueHours,
                userName = userName,
                habitDescription = habit.description
            )
            
            // Get the appropriate image for overdue duration (always use 6hour image for 6+)
            val imageResId = getOverdueImageResource(overdueHours)
            val bigPicture = BitmapFactory.decodeResource(context.resources, imageResId)
            
            // Create notification
            val notificationId = getNotificationId(habit.id, overdueHours)
            val notification = buildOverdueNotification(
                context = context,
                channelId = channelId,
                habit = habit,
                message = message,
                bigPicture = bigPicture,
                overdueHours = overdueHours
            )
            
            // Show notification
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(notificationId, notification.build())
                Log.d(TAG, "Overdue notification shown for habit: ${habit.title}, overdue: ${overdueHours}h")
            } else {
                Log.w(TAG, "Notifications are disabled by user")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing overdue notification", e)
        }
    }
    
    /**
     * Generate personalized overdue message using Gemini API or fallback
     * Uses aggressive messaging for 6+ hours overdue
     */
    private suspend fun generateOverdueMessage(
        context: Context,
        habitTitle: String,
        overdueHours: Int,
        userName: String?,
        habitDescription: String = ""
    ): String = withContext(Dispatchers.IO) {
        try {
            val geminiPrefs = GeminiPreferences(context)
            val apiKey = geminiPrefs.getApiKey()
            
            if (!apiKey.isNullOrBlank() && !userName.isNullOrBlank()) {
                // Generate personalized message with Gemini
                val geminiService = GeminiApiService(apiKey)
                
                // Use aggressive messaging for 6+ hours overdue
                val result = if (overdueHours >= 6) {
                    geminiService.generateAggressiveMotivationalMessage(
                        userName = userName,
                        habitTitle = habitTitle,
                        habitDescription = habitDescription,
                        hoursOverdue = overdueHours
                    )
                } else {
                    // Regular encouraging message for 2-5 hours
                    val prompt = """
                        Generate a personalized reminder message for $userName who has not completed their habit "$habitTitle".
                        This habit is now $overdueHours hour(s) overdue.
                        
                        Requirements:
                        - Address $userName by name
                        - Mention the specific habit: "$habitTitle"
                        - Be encouraging but firm - remind them it's $overdueHours hours late
                        - Keep it short (1-2 sentences maximum)
                        - Be supportive and motivating, not harsh
                        - Don't use emojis
                        - Generate ONLY the message text, nothing else
                    """.trimIndent()
                    geminiService.generateCustomMessage(prompt)
                }
                
                if (result.isSuccess) {
                    val generatedMessage = result.getOrNull()
                    if (!generatedMessage.isNullOrBlank()) {
                        Log.d(TAG, "Generated personalized message with Gemini (aggressive: ${overdueHours >= 6})")
                        return@withContext generatedMessage
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating Gemini message, using fallback", e)
        }
        
        // Fallback message (also more aggressive for 6+ hours)
        val userPrefix = if (!userName.isNullOrBlank()) "$userName, " else ""
        return@withContext if (overdueHours >= 6) {
            "${userPrefix}URGENT: \"${habitTitle}\" is $overdueHours hour(s) overdue! Stop procrastinating and take action NOW!"
        } else {
            "${userPrefix}Your habit \"${habitTitle}\" is $overdueHours hour(s) overdue. Complete it now to stay on track!"
        }
    }
    
    /**
     * Build overdue notification with BigPictureStyle
     */
    private fun buildOverdueNotification(
        context: Context,
        channelId: String,
        habit: Habit,
        message: String,
        bigPicture: Bitmap,
        overdueHours: Int
    ): NotificationCompat.Builder {
        
        // Intent to open habit details
        val contentIntent = PendingIntent.getActivity(
            context,
            getNotificationId(habit.id, overdueHours),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("habitId", habit.id)
                putExtra("openHabitDetails", true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification with BigPictureStyle
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_habit)
            .setColor(ContextCompat.getColor(context, R.color.purple_500))
            .setContentTitle("⚠️ ${habit.title} - ${overdueHours}h Overdue")
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bigPicture)
                    .bigLargeIcon(null as Bitmap?) // Hide large icon when expanded
                    .setBigContentTitle("⚠️ ${habit.title} - ${overdueHours}h Overdue")
                    .setSummaryText(message)
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
        
        // Add "Mark as Done" action button
        val markDoneAction = NotificationCompat.Action.Builder(
            R.drawable.ic_check_24,
            context.getString(R.string.mark_as_completed),
            createMarkDoneActionPendingIntent(context, habit.id, overdueHours)
        ).build()
        
        builder.addAction(markDoneAction)
        
        // Enable heads-up notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setFullScreenIntent(contentIntent, false)
        }
        
        return builder
    }
    
    /**
     * Create PendingIntent for "Mark as Done" action
     */
    private fun createMarkDoneActionPendingIntent(
        context: Context,
        habitId: Long,
        overdueHours: Int
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "COMPLETE_OVERDUE_HABIT"
            putExtra("habitId", habitId)
            putExtra("overdueHours", overdueHours)
        }
        return PendingIntent.getBroadcast(
            context,
            "overdue_complete_${habitId}_${overdueHours}h".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Get the appropriate drawable resource for overdue duration
     * For 6+ hours, use the 6hourplus image for more aggressive visual
     */
    private fun getOverdueImageResource(overdueHours: Int): Int {
        return if (overdueHours >= 6) {
            R.drawable.overdue_6hourplus
        } else {
            overdueImageMap[overdueHours] ?: R.drawable.overdue_6hour
        }
    }
    
    /**
     * Get unique notification ID for habit and overdue duration
     */
    private fun getNotificationId(habitId: Long, overdueHours: Int): Int {
        return NOTIFICATION_ID_PREFIX + (habitId.toInt() * 10) + overdueHours
    }
    
    /**
     * Ensure notification channel exists for overdue notifications
     */
    private fun ensureOverdueChannel(context: Context, habit: Habit): String {
        val channelId = "${CHANNEL_ID_PREFIX}${habit.id}"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
                ?: return channelId
            
            // Create channel if it doesn't exist
            if (manager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    "Overdue: ${habit.title}",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Overdue notifications for ${habit.title}"
                    enableLights(true)
                    lightColor = Color.RED
                    enableVibration(true)
                    setShowBadge(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    
                    // Use habit's notification sound if available
                    val soundUri = it.atraj.habittracker.data.local.NotificationSound.getActualUri(
                        context,
                        habit.getNotificationSound()
                    )
                    if (soundUri != null) {
                        val audioAttributes = android.media.AudioAttributes.Builder()
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .build()
                        setSound(soundUri, audioAttributes)
                    }
                }
                manager.createNotificationChannel(channel)
                Log.d(TAG, "Created overdue channel: $channelId")
            }
        }
        
        return channelId
    }
    
    /**
     * Dismiss overdue notification
     */
    fun dismissOverdueNotification(context: Context, habitId: Long, overdueHours: Int) {
        val notificationId = getNotificationId(habitId, overdueHours)
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
        Log.d(TAG, "Dismissed overdue notification for habit: $habitId, overdue: ${overdueHours}h")
    }
    
    /**
     * Dismiss all overdue notifications for a habit (including recurring ones)
     */
    fun dismissAllOverdueNotifications(context: Context, habitId: Long) {
        val notificationManager = NotificationManagerCompat.from(context)
        
        // Dismiss initial notifications (2-6 hours)
        for (hours in 2..6) {
            val notificationId = getNotificationId(habitId, hours)
            notificationManager.cancel(notificationId)
        }
        
        // Dismiss recurring notifications (8-48 hours, every 2 hours)
        for (hours in 8..48 step 2) {
            val notificationId = getNotificationId(habitId, hours)
            notificationManager.cancel(notificationId)
        }
        
        Log.d(TAG, "Dismissed all overdue notifications (including recurring) for habit: $habitId")
    }
    
    /**
     * Delete overdue notification channel for a habit
     */
    fun deleteOverdueChannel(context: Context, habitId: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "${CHANNEL_ID_PREFIX}${habitId}"
            val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
                ?: return
            
            manager.deleteNotificationChannel(channelId)
            Log.d(TAG, "Deleted overdue channel: $channelId")
        }
    }
}
