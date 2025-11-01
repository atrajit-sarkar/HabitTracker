package it.atraj.habittracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import it.atraj.habittracker.MainActivity
import it.atraj.habittracker.R
import it.atraj.habittracker.data.local.Habit
import it.atraj.habittracker.gemini.GeminiApiService
import it.atraj.habittracker.gemini.GeminiPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Service for sending notifications for bad habit tracking
 * Sends encouragement when user avoids the app, disappointment when they use it
 */
object BadHabitNotificationService {
    
    private const val CHANNEL_ID = "bad_habit_channel"
    private const val CHANNEL_NAME = "Bad Habit Tracking"
    private const val CHANNEL_DESCRIPTION = "Notifications for bad habit tracking progress"
    
    /**
     * Ensure the notification channel exists
     */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Send encouragement notification when user successfully avoids the bad habit
     */
    fun sendEncouragementNotification(context: Context, habit: Habit) {
        ensureChannel(context)
        
        // Generate Gemini message in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geminiMessage = generateEncouragementMessage(habit, context)
                showEncouragementNotification(context, habit, geminiMessage)
            } catch (e: Exception) {
                android.util.Log.e("BadHabitNotif", "Error generating Gemini message", e)
                // Show default message on error
                showEncouragementNotification(context, habit, getDefaultEncouragementMessage(habit))
            }
        }
    }
    
    /**
     * Send disappointment notification when user uses the bad habit app
     */
    fun sendDisappointmentNotification(context: Context, habit: Habit, usageTimeMs: Long) {
        ensureChannel(context)
        
        // Generate Gemini message in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geminiMessage = generateDisappointmentMessage(habit, usageTimeMs, context)
                showDisappointmentNotification(context, habit, geminiMessage, usageTimeMs)
            } catch (e: Exception) {
                android.util.Log.e("BadHabitNotif", "Error generating Gemini message", e)
                // Show default message on error
                showDisappointmentNotification(context, habit, getDefaultDisappointmentMessage(habit), usageTimeMs)
            }
        }
    }
    
    private fun showEncouragementNotification(context: Context, habit: Habit, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openHabitDetails", true)
            putExtra("habitId", habit.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            habit.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Load and resize images
        val allDoneImage = try {
            context.assets.open("all-done.png").use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            android.util.Log.e("BadHabitNotif", "Failed to load all-done.png", e)
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_notification_habit)
        }
        
        // Load habit avatar
        val avatarBitmap = createAvatarBitmap(habit.avatar, context, habit.id)
        
        // Resize for different sizes
        val smallAvatar = android.graphics.Bitmap.createScaledBitmap(avatarBitmap, 48.dpToPx(context), 48.dpToPx(context), true)
        val smallSuccessIcon = android.graphics.Bitmap.createScaledBitmap(allDoneImage, 48.dpToPx(context), 48.dpToPx(context), true)
        val largeSuccessIcon = android.graphics.Bitmap.createScaledBitmap(allDoneImage, 128.dpToPx(context), 128.dpToPx(context), true)
        
        val titleText = "🎉 ${habit.title}"
        
        // Create custom notification layout for collapsed view
        val collapsedView = android.widget.RemoteViews(context.packageName, R.layout.notification_overdue)
        collapsedView.setTextViewText(R.id.notification_title, titleText)
        collapsedView.setTextViewText(R.id.notification_text, message)
        collapsedView.setImageViewBitmap(R.id.notification_avatar, smallAvatar)
        collapsedView.setImageViewBitmap(R.id.notification_overdue_icon, smallSuccessIcon)

        // Create custom notification layout for expanded view
        val expandedView = android.widget.RemoteViews(context.packageName, R.layout.notification_overdue_expanded)
        expandedView.setTextViewText(R.id.notification_title, titleText)
        expandedView.setTextViewText(R.id.notification_text, message)
        expandedView.setImageViewBitmap(R.id.notification_overdue_icon, largeSuccessIcon)
        
        // Build notification with custom layout
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_habit)
            .setColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.holo_green_light))
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(habit.id.toInt() + 10000, notification)
        
        android.util.Log.d("BadHabitNotif", "Sent encouragement notification for ${habit.title}")
    }
    
    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
    
    private fun showDisappointmentNotification(context: Context, habit: Habit, message: String, usageTimeMs: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openHabitDetails", true)
            putExtra("habitId", habit.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            habit.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Load and resize images
        val overdueImage = try {
            context.assets.open("more-overdue.png").use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            android.util.Log.e("BadHabitNotif", "Failed to load more-overdue.png", e)
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_notification_habit)
        }
        
        // Load habit avatar
        val avatarBitmap = createAvatarBitmap(habit.avatar, context, habit.id)
        
        // Resize for different sizes
        val smallAvatar = android.graphics.Bitmap.createScaledBitmap(avatarBitmap, 48.dpToPx(context), 48.dpToPx(context), true)
        val smallOverdueIcon = android.graphics.Bitmap.createScaledBitmap(overdueImage, 48.dpToPx(context), 48.dpToPx(context), true)
        val largeOverdueIcon = android.graphics.Bitmap.createScaledBitmap(overdueImage, 128.dpToPx(context), 128.dpToPx(context), true)
        
        val usageMinutes = TimeUnit.MILLISECONDS.toMinutes(usageTimeMs)
        val usageText = if (usageMinutes > 0) " (${usageMinutes}min used)" else ""
        val titleText = "😔 ${habit.title}$usageText"
        
        // Create custom notification layout for collapsed view
        val collapsedView = android.widget.RemoteViews(context.packageName, R.layout.notification_overdue)
        collapsedView.setTextViewText(R.id.notification_title, titleText)
        collapsedView.setTextViewText(R.id.notification_text, message)
        collapsedView.setImageViewBitmap(R.id.notification_avatar, smallAvatar)
        collapsedView.setImageViewBitmap(R.id.notification_overdue_icon, smallOverdueIcon)

        // Create custom notification layout for expanded view
        val expandedView = android.widget.RemoteViews(context.packageName, R.layout.notification_overdue_expanded)
        expandedView.setTextViewText(R.id.notification_title, titleText)
        expandedView.setTextViewText(R.id.notification_text, message)
        expandedView.setImageViewBitmap(R.id.notification_overdue_icon, largeOverdueIcon)
        
        // Build notification with custom layout
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_habit)
            .setColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.holo_red_light))
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(habit.id.toInt() + 20000, notification)
        
        android.util.Log.d("BadHabitNotif", "Sent disappointment notification for ${habit.title}")
    }
    
    private suspend fun generateEncouragementMessage(habit: Habit, context: Context): String {
        try {
            val geminiPrefs = GeminiPreferences(context)
            val apiKey = geminiPrefs.getApiKey()
            
            if (apiKey.isNullOrBlank()) {
                return getDefaultEncouragementMessage(habit)
            }
            
            val prompt = """
                The user is trying to release a bad habit: "${habit.title}".
                Description: ${habit.description}
                
                They successfully avoided using the tracked app today! Generate a short, encouraging message (max 100 characters) congratulating them and motivating them to continue. Be warm, supportive, and enthusiastic.
            """.trimIndent()
            
            val geminiService = GeminiApiService(apiKey)
            val result = geminiService.generateCustomMessage(prompt)
            return result.getOrNull() ?: getDefaultEncouragementMessage(habit)
        } catch (e: Exception) {
            android.util.Log.e("BadHabitNotif", "Error generating encouragement message", e)
            return getDefaultEncouragementMessage(habit)
        }
    }
    
    private suspend fun generateDisappointmentMessage(habit: Habit, usageTimeMs: Long, context: Context): String {
        try {
            val geminiPrefs = GeminiPreferences(context)
            val apiKey = geminiPrefs.getApiKey()
            
            if (apiKey.isNullOrBlank()) {
                return getDefaultDisappointmentMessage(habit)
            }
            
            val usageMinutes = TimeUnit.MILLISECONDS.toMinutes(usageTimeMs)
            
            val prompt = """
                The user is trying to release a bad habit: "${habit.title}".
                Description: ${habit.description}
                
                They used the tracked app today for $usageMinutes minutes. Generate a short, empathetic message (max 100 characters) expressing disappointment but also encouraging them to try again tomorrow. Be understanding but firm about the goal.
            """.trimIndent()
            
            val geminiService = GeminiApiService(apiKey)
            val result = geminiService.generateCustomMessage(prompt)
            return result.getOrNull() ?: getDefaultDisappointmentMessage(habit)
        } catch (e: Exception) {
            android.util.Log.e("BadHabitNotif", "Error generating disappointment message", e)
            return getDefaultDisappointmentMessage(habit)
        }
    }
    
    private fun getDefaultEncouragementMessage(habit: Habit): String {
        val messages = listOf(
            "Amazing! You're making great progress staying away from ${habit.targetAppName ?: "that app"}!",
            "Keep it up! Another day without ${habit.targetAppName ?: "the app"} - you're doing fantastic!",
            "Proud of you! You resisted the urge today. Tomorrow will be even easier!",
            "Success! You're building a better habit, one day at a time!",
            "Well done! ${habit.targetAppName ?: "That app"} didn't control you today!"
        )
        return messages.random()
    }
    
    private fun getDefaultDisappointmentMessage(habit: Habit): String {
        val messages = listOf(
            "You used ${habit.targetAppName ?: "the app"} today. Don't give up - tomorrow is a fresh start!",
            "A slip today doesn't erase your progress. Get back on track tomorrow!",
            "Everyone stumbles. What matters is getting back up. You've got this!",
            "Tomorrow is another chance to succeed. Don't let one day define you!",
            "You're still in this journey. Refocus and try again tomorrow!"
        )
        return messages.random()
    }
    
    /**
     * Create avatar bitmap for notification
     */
    private fun createAvatarBitmap(avatar: it.atraj.habittracker.data.local.HabitAvatar, context: Context, habitId: Long? = null): android.graphics.Bitmap {
        return when (avatar.type) {
            it.atraj.habittracker.data.local.HabitAvatarType.CUSTOM_IMAGE -> {
                // Try to load from cache
                if (habitId != null) {
                    try {
                        val cacheFile = java.io.File(context.cacheDir, "habit_avatar_$habitId.png")
                        if (cacheFile.exists()) {
                            return BitmapFactory.decodeFile(cacheFile.absolutePath)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("BadHabitNotif", "Failed to load cached avatar", e)
                    }
                }
                // Fallback to default
                BitmapFactory.decodeResource(context.resources, R.drawable.ic_notification_habit)
            }
            it.atraj.habittracker.data.local.HabitAvatarType.EMOJI -> {
                // Create bitmap with emoji text
                createEmojiBitmap(avatar.value, avatar.backgroundColor, context)
            }
            it.atraj.habittracker.data.local.HabitAvatarType.DEFAULT_ICON -> {
                // Use default icon
                BitmapFactory.decodeResource(context.resources, R.drawable.ic_notification_habit)
            }
        }
    }
    
    /**
     * Create bitmap with emoji character
     */
    private fun createEmojiBitmap(emoji: String, backgroundColor: String, context: Context): android.graphics.Bitmap {
        val size = 128
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Draw background
        val bgPaint = android.graphics.Paint().apply {
            color = try {
                android.graphics.Color.parseColor(backgroundColor)
            } catch (e: Exception) {
                android.graphics.Color.parseColor("#6650A4") // Default purple
            }
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)
        
        // Draw emoji
        val textPaint = android.graphics.Paint().apply {
            textSize = size * 0.6f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        
        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(emoji, 0, emoji.length, textBounds)
        val textY = size / 2f - textBounds.exactCenterY()
        
        canvas.drawText(emoji, size / 2f, textY, textPaint)
        
        return bitmap
    }
}

