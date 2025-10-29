package it.atraj.habittracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import androidx.core.graphics.toColorInt
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import it.atraj.habittracker.MainActivity
import it.atraj.habittracker.R
import it.atraj.habittracker.data.local.Habit
import it.atraj.habittracker.data.local.HabitAvatar
import it.atraj.habittracker.data.local.HabitAvatarType
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
     * Build overdue notification with large overdue image on the left and text on the right (like image is speaking)
     */
    private fun buildOverdueNotification(
        context: Context,
        channelId: String,
        habit: Habit,
        message: String,
        bigPicture: Bitmap,
        overdueHours: Int
    ): NotificationCompat.Builder {
        
        // Intent to open habit details - same as regular notifications
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
        
        // Create habit avatar for the collapsed state
        val avatarBitmap = createAvatarBitmap(habit.avatar, context, habit.id)
        
        // Create composite image: overdue image on left + message text on right (like image is speaking)
        val compositeImage = createCompositeNotificationImage(context, bigPicture, message, habit.title, overdueHours)
        
        // Build notification with BigPictureStyle showing composite image
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_habit)
            .setLargeIcon(avatarBitmap) // Show habit avatar on right side when collapsed
            .setColor(ContextCompat.getColor(context, R.color.purple_500))
            .setContentTitle("⚠️ ${habit.title} - ${overdueHours}h Overdue")
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(compositeImage) // Composite image: overdue image on left, text on right
                    .bigLargeIcon(avatarBitmap) // Keep avatar on right when expanded
                    .setBigContentTitle("⚠️ ${habit.title} - ${overdueHours}h Overdue")
                    .setSummaryText(message) // Full message text appears below the image when expanded
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true) // Allow swipe to dismiss
            .setOngoing(false) // Not an ongoing notification
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for visibility
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setNumber(1) // Show badge number
            .setTicker("⚠️ ${habit.title} - ${overdueHours}h Overdue") // Legacy ticker text for older Android versions
        
        // For Android versions below O, set sound and vibration directly on notification
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val soundUri = it.atraj.habittracker.data.local.NotificationSound.getActualUri(
                context,
                habit.getNotificationSound()
            )
            if (soundUri != null) {
                builder.setSound(soundUri)
                Log.d(TAG, "Set sound on overdue notification (pre-O): $soundUri")
            } else {
                builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
            }
            
            // Add custom vibration pattern
            val vibrationPattern = longArrayOf(0, 250, 250, 250)
            builder.setVibrate(vibrationPattern)
        }
        
        // Add action buttons
        val markDoneAction = NotificationCompat.Action.Builder(
            R.drawable.ic_check_24,
            context.getString(R.string.mark_as_completed),
            createMarkDoneActionPendingIntent(context, habit.id, overdueHours)
        ).build()
        
        val dismissAction = NotificationCompat.Action.Builder(
            R.drawable.ic_close_24,
            context.getString(R.string.dismiss),
            createDismissActionPendingIntent(context, habit.id, overdueHours)
        ).build()
        
        builder
            .addAction(markDoneAction)
            .addAction(dismissAction)
        
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
     * Create PendingIntent for "Dismiss" action
     */
    private fun createDismissActionPendingIntent(
        context: Context,
        habitId: Long,
        overdueHours: Int
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "DISMISS_OVERDUE_HABIT"
            putExtra("habitId", habitId)
            putExtra("overdueHours", overdueHours)
        }
        return PendingIntent.getBroadcast(
            context,
            "overdue_dismiss_${habitId}_${overdueHours}h".hashCode(),
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
     * Ensure default overdue channel exists (for backward compatibility)
     * Call this from MainActivity onCreate
     */
    fun ensureDefaultOverdueChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "overdue_habit_default"
            val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
                ?: return
            val existing = manager.getNotificationChannel(channelId)
            if (existing == null) {
                val channel = NotificationChannel(
                    channelId,
                    "Overdue Habit Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for overdue habits"
                    enableLights(true)
                    lightColor = Color.RED
                    enableVibration(true)
                    setShowBadge(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    setBypassDnd(false)
                }
                manager.createNotificationChannel(channel)
                Log.d(TAG, "Created default overdue channel")
            }
        }
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
    
    /**
     * Create composite image with overdue picture on left and message text on right
     * This makes it look like the image is "speaking" the message
     * Shows as much text as possible, full text available in notification's setSummaryText
     */
    private fun createCompositeNotificationImage(
        context: Context,
        overdueImage: Bitmap,
        message: String,
        habitTitle: String,
        overdueHours: Int
    ): Bitmap {
        // Notification big picture dimensions (wider and taller for more text)
        val density = context.resources.displayMetrics.density
        val canvasWidth = (480 * density).toInt() // Increased width
        val canvasHeight = (280 * density).toInt() // Increased height for more text lines
        
        // Create canvas
        val compositeBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(compositeBitmap)
        
        // Background color
        canvas.drawColor(Color.parseColor("#1E1E2E")) // Dark background
        
        // Scale and draw overdue image on the LEFT side (slightly smaller to give more space for text)
        val imageSize = (canvasHeight * 0.85f).toInt() // 85% of height
        val scaledOverdueImage = Bitmap.createScaledBitmap(overdueImage, imageSize, imageSize, true)
        val imageLeft = (15 * density) // Padding from left
        val imageTop = (canvasHeight - imageSize) / 2f // Center vertically
        canvas.drawBitmap(scaledOverdueImage, imageLeft, imageTop, null)
        
        // Draw text on the RIGHT side (more space for text)
        val textStartX = imageLeft + imageSize + (15 * density) // Start after image + padding
        val textWidth = canvasWidth - textStartX - (15 * density) // Available width for text
        
        // Title paint (slightly smaller)
        val titlePaint = Paint().apply {
            color = Color.parseColor("#FFD700") // Gold color for warning
            textSize = 20 * density
            isAntiAlias = true
            isFakeBoldText = true
        }
        
        // Message paint (optimized size)
        val messagePaint = Paint().apply {
            color = Color.WHITE
            textSize = 16 * density
            isAntiAlias = true
        }
        
        // Draw title (compact)
        val title = "⚠️ $habitTitle"
        var currentY = canvasHeight * 0.18f
        canvas.drawText(title, textStartX, currentY, titlePaint)
        
        // Draw overdue hours (compact)
        currentY += 25 * density
        val overdueText = "$overdueHours hrs overdue"
        val overdueTextPaint = Paint().apply {
            color = Color.parseColor("#FF6B6B") // Red color
            textSize = 14 * density
            isAntiAlias = true
        }
        canvas.drawText(overdueText, textStartX, currentY, overdueTextPaint)
        
        // Draw message with word wrap (more lines, tighter spacing)
        currentY += 28 * density
        val words = message.split(" ")
        var line = ""
        var linesDrawn = 0
        val maxLines = 8 // Allow up to 8 lines of text
        val lineHeight = 20 * density
        
        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            val testWidth = messagePaint.measureText(testLine)
            
            if (testWidth > textWidth && line.isNotEmpty()) {
                // Draw current line and start new one
                canvas.drawText(line, textStartX, currentY, messagePaint)
                currentY += lineHeight
                linesDrawn++
                line = word
                
                // Stop if we've drawn max lines or run out of space
                if (linesDrawn >= maxLines || currentY > canvasHeight * 0.92f) {
                    // Add ellipsis if text was truncated
                    if (words.indexOf(word) < words.size - 1) {
                        line += "..."
                    }
                    break
                }
            } else {
                line = testLine
            }
        }
        
        // Draw remaining text if we have space
        if (line.isNotEmpty() && currentY < canvasHeight * 0.92f) {
            canvas.drawText(line, textStartX, currentY, messagePaint)
        }
        
        return compositeBitmap
    }
    
    /**
     * Create avatar bitmap for notification large icon
     * Same as HabitReminderService implementation
     */
    private fun createAvatarBitmap(avatar: HabitAvatar, context: Context, habitId: Long? = null): Bitmap {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw background circle
        val backgroundPaint = Paint().apply {
            color = avatar.backgroundColor.toColorInt()
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, backgroundPaint)
        
        when (avatar.type) {
            HabitAvatarType.EMOJI -> {
                // Draw emoji
                val textPaint = Paint().apply {
                    textSize = size * 0.6f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                val textY = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(avatar.value, size / 2f, textY, textPaint)
            }
            HabitAvatarType.DEFAULT_ICON -> {
                // Draw a simple icon
                val iconPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                    strokeWidth = 8f
                    style = Paint.Style.STROKE
                }
                val centerX = size / 2f
                val centerY = size / 2f
                val radius = size * 0.2f
                canvas.drawCircle(centerX, centerY, radius, iconPaint)
            }
            HabitAvatarType.CUSTOM_IMAGE -> {
                // For custom images, try to load from cache
                try {
                    val imageLoader = coil.ImageLoader.Builder(context)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build()
                    
                    val cacheKey = "habit_avatar_$habitId"
                    
                    // Only load from cache - disable network
                    val drawable = imageLoader.diskCache?.get(cacheKey)?.use { snapshot ->
                        android.graphics.BitmapFactory.decodeFile(snapshot.data.toFile().absolutePath)
                    } ?: imageLoader.memoryCache?.get(coil.memory.MemoryCache.Key(cacheKey))?.bitmap
                    
                    if (drawable != null) {
                        // Create circular bitmap from cached image
                        val scaledBitmap = Bitmap.createScaledBitmap(drawable, size, size, true)
                        val circlePaint = Paint().apply {
                            isAntiAlias = true
                        }
                        canvas.drawBitmap(scaledBitmap, 0f, 0f, circlePaint)
                        return bitmap
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading custom avatar from cache", e)
                }
                
                // Fallback to default icon if cache load fails
                val iconPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                    strokeWidth = 8f
                    style = Paint.Style.STROKE
                }
                val centerX = size / 2f
                val centerY = size / 2f
                val radius = size * 0.2f
                canvas.drawCircle(centerX, centerY, radius, iconPaint)
            }
        }
        
        return bitmap
    }
}
