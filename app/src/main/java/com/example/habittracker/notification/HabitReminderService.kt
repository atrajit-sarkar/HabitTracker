package it.atraj.habittracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.app.Notification
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.toColorInt
import it.atraj.habittracker.MainActivity
import it.atraj.habittracker.R
import it.atraj.habittracker.data.local.Habit
import it.atraj.habittracker.data.local.HabitAvatar
import it.atraj.habittracker.data.local.HabitAvatarType
import it.atraj.habittracker.data.local.NotificationSound
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import android.util.Log

object HabitReminderService {
    private const val CHANNEL_PREFIX = "habit_reminder_channel_"
    private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    
    /**
     * Get the icon resource ID of the currently active launcher icon
     * Falls back to default notification icon if unable to retrieve
     */
    private fun getCurrentLauncherIconResource(context: Context): Int {
        try {
            val packageManager = context.packageManager
            val packageName = context.packageName
            
            // Get all activity aliases for MainActivity
            val mainActivityName = "it.atraj.habittracker.MainActivity"
            val activityAliases = listOf(
                mainActivityName,
                "$mainActivityName.Default",
                "$mainActivityName.Warning",
                "$mainActivityName.Angry",
                "$mainActivityName.WarningDefault",
                "$mainActivityName.AngryDefault",
                "$mainActivityName.Anime",
                "$mainActivityName.WarningAnime",
                "$mainActivityName.AngryAnime",
                "$mainActivityName.Sitama",
                "$mainActivityName.WarningSitama",
                "$mainActivityName.AngrySitama",
                "$mainActivityName.Bird",
                "$mainActivityName.WarningBird",
                "$mainActivityName.AngryBird",
                "$mainActivityName.Atrajit",
                "$mainActivityName.WarningAtrajit",
                "$mainActivityName.AngryAtrajit"
            )
            
            // Find the currently enabled launcher activity
            for (aliasName in activityAliases) {
                try {
                    val componentName = ComponentName(packageName, aliasName)
                    val componentInfo = packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)
                    val componentState = packageManager.getComponentEnabledSetting(componentName)
                    
                    // Check if this component is enabled
                    if (componentState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                        (componentState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && aliasName == mainActivityName)) {
                        
                        // Get the icon resource ID from the activity
                        val iconResId = componentInfo.icon
                        if (iconResId != 0) {
                            android.util.Log.d("HabitReminderService", "Using launcher icon from $aliasName: $iconResId")
                            return iconResId
                        }
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    // Activity alias not found, continue to next
                    continue
                }
            }
            
            // Fallback: Get the main application icon
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            if (appInfo.icon != 0) {
                android.util.Log.d("HabitReminderService", "Using application icon: ${appInfo.icon}")
                return appInfo.icon
            }
        } catch (e: Exception) {
            android.util.Log.e("HabitReminderService", "Error getting launcher icon", e)
        }
        
        // Final fallback to default notification icon
        android.util.Log.d("HabitReminderService", "Using fallback notification icon")
        return R.drawable.ic_notification_habit
    }

    /**
     * Create or update a notification channel for a specific habit
     * This allows each habit to have its own custom sound
     */
    private fun ensureHabitChannel(context: Context, habit: Habit): String {
        val channelId = "${CHANNEL_PREFIX}${habit.id}"
        
        android.util.Log.d("HabitReminderService", "ensureHabitChannel called for habit: ${habit.title} (ID: ${habit.id})")
        android.util.Log.d("HabitReminderService", "  Habit sound: ${habit.notificationSoundName} (ID: ${habit.notificationSoundId}, URI: ${habit.notificationSoundUri})")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
                ?: return channelId
            
            // Check if channel already exists
            val existing = manager.getNotificationChannel(channelId)
            
            // Delete existing channel if sound has changed
            if (existing != null) {
                val currentSoundUri = existing.sound
                val newSoundUri = NotificationSound.getActualUri(context, habit.getNotificationSound())
                
                android.util.Log.d("HabitReminderService", "  Channel exists. Current sound: $currentSoundUri, New sound: $newSoundUri")
                
                // If sounds are different, delete and recreate the channel
                if (currentSoundUri != newSoundUri) {
                    android.util.Log.d("HabitReminderService", "  Sounds are different! Deleting channel to recreate...")
                    manager.deleteNotificationChannel(channelId)
                } else {
                    android.util.Log.d("HabitReminderService", "  Sounds are same, keeping existing channel")
                }
            } else {
                android.util.Log.d("HabitReminderService", "  Channel does not exist, will create new one")
            }
            
            // Create or recreate the channel
            if (manager.getNotificationChannel(channelId) == null) {
                val soundUri = NotificationSound.getActualUri(context, habit.getNotificationSound())
                
                android.util.Log.d("HabitReminderService", "  Creating channel with sound URI: $soundUri")
                
                val channel = NotificationChannel(
                    channelId,
                    "Reminder: ${habit.title}",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for ${habit.title} habit"
                    enableLights(true)
                    lightColor = Color.BLUE
                    enableVibration(true)
                    setShowBadge(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setBypassDnd(false)
                    
                    // Set custom sound for this channel
                    if (soundUri != null) {
                        val audioAttributes = android.media.AudioAttributes.Builder()
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .build()
                        setSound(soundUri, audioAttributes)
                        android.util.Log.d("HabitReminderService", "  Sound set on channel: $soundUri")
                    } else {
                        android.util.Log.w("HabitReminderService", "  Sound URI is null! Channel will use default sound")
                    }
                }
                manager.createNotificationChannel(channel)
                android.util.Log.d("HabitReminderService", "✓ Created channel $channelId with sound: ${soundUri}")
                
                // Verify the channel was created with correct sound
                val verifyChannel = manager.getNotificationChannel(channelId)
                android.util.Log.d("HabitReminderService", "  Verification: Channel sound after creation: ${verifyChannel?.sound}")
            } else {
                android.util.Log.d("HabitReminderService", "  Channel already exists after check, not recreating")
            }
        }
        
        return channelId
    }
    
    /**
     * Force update/recreate the notification channel for a habit
     * Call this when the user changes the notification sound in settings
     */
    fun updateHabitChannel(context: Context, habit: Habit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "${CHANNEL_PREFIX}${habit.id}"
            val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
                ?: return
            
            // Always delete the existing channel to force recreation with new sound
            manager.deleteNotificationChannel(channelId)
            android.util.Log.d("HabitReminderService", "Deleted channel $channelId for sound update")
            
            // Recreate the channel with new sound
            ensureHabitChannel(context, habit)
        }
    }
    
    /**
     * Delete the notification channel for a deleted habit
     * Call this when a habit is permanently deleted to clean up system settings
     */
    fun deleteHabitChannel(context: Context, habitId: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "${CHANNEL_PREFIX}${habitId}"
            val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
                ?: return
            
            manager.deleteNotificationChannel(channelId)
            android.util.Log.d("HabitReminderService", "Deleted channel $channelId for habit deletion")
        }
    }
    
    /**
     * Delete multiple notification channels at once
     * Call this when emptying trash to clean up all deleted habits' channels
     */
    fun deleteMultipleHabitChannels(context: Context, habitIds: List<Long>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
                ?: return
            
            habitIds.forEach { habitId ->
                val channelId = "${CHANNEL_PREFIX}${habitId}"
                manager.deleteNotificationChannel(channelId)
                android.util.Log.d("HabitReminderService", "Deleted channel $channelId for batch deletion")
            }
        }
    }
    
    /**
     * Sync all habit notification channels with the system
     * Ensures all active habits have their notification channels created
     * Also removes orphaned/duplicate channels that don't match any habit
     * Call this on app startup to handle:
     * - App updates that might have cleared channels
     * - System cleanup that removed channels
     * - Database restore scenarios
     * - Duplicate channels from bugs or race conditions
     * - Orphaned channels from deleted habits
     */
    fun syncAllHabitChannels(context: Context, habits: List<Habit>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
                ?: return
            
            // Get all existing habit channels
            val existingChannels = manager.notificationChannels
                .filter { it.id.startsWith(CHANNEL_PREFIX) }
            
            // Get active habit IDs (non-deleted)
            val activeHabitIds = habits.filter { !it.isDeleted }.map { it.id }.toSet()
            
            var channelsCreated = 0
            var channelsDeleted = 0
            var channelsSkipped = 0
            
            // Step 1: Remove orphaned/duplicate channels
            // Group channels by habit ID to detect duplicates
            val channelsByHabitId = existingChannels.groupBy { channel ->
                channel.id.removePrefix(CHANNEL_PREFIX).toLongOrNull()
            }
            
            channelsByHabitId.forEach { (habitId, channels) ->
                if (habitId == null) {
                    // Invalid channel ID format - delete it
                    channels.forEach { channel ->
                        manager.deleteNotificationChannel(channel.id)
                        channelsDeleted++
                        android.util.Log.d("HabitReminderService", "Deleted invalid channel: ${channel.id}")
                    }
                } else if (habitId !in activeHabitIds) {
                    // Orphaned channel (habit deleted or doesn't exist) - delete it
                    channels.forEach { channel ->
                        manager.deleteNotificationChannel(channel.id)
                        channelsDeleted++
                        android.util.Log.d("HabitReminderService", "Deleted orphaned channel: ${channel.id}")
                    }
                } else if (channels.size > 1) {
                    // Duplicate channels for same habit - delete all and will recreate
                    channels.forEach { channel ->
                        manager.deleteNotificationChannel(channel.id)
                        channelsDeleted++
                        android.util.Log.d("HabitReminderService", "Deleted duplicate channel: ${channel.id}")
                    }
                }
            }
            
            // Step 2: Get current state after cleanup
            val remainingChannelIds = manager.notificationChannels
                .map { it.id }
                .filter { it.startsWith(CHANNEL_PREFIX) }
                .toSet()
            
            // Step 3: Ensure channel exists for each active habit
            habits.filter { !it.isDeleted }.forEach { habit ->
                val channelId = "${CHANNEL_PREFIX}${habit.id}"
                
                if (channelId !in remainingChannelIds) {
                    // Channel missing - create it
                    ensureHabitChannel(context, habit)
                    channelsCreated++
                    android.util.Log.d("HabitReminderService", "Created channel for habit: ${habit.title}")
                } else {
                    channelsSkipped++
                }
            }
            
            android.util.Log.d(
                "HabitReminderService",
                "Channel sync complete: $channelsCreated created, $channelsDeleted deleted, $channelsSkipped kept, ${activeHabitIds.size} active habits"
            )
        }
    }
    
    /**
     * Ensure default channel for backward compatibility
     */
    fun ensureDefaultChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "habit_reminder_default"
            val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
                ?: return
            val existing = manager.getNotificationChannel(channelId)
            if (existing == null) {
                val channel = NotificationChannel(
                    channelId,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_description)
                    enableLights(true)
                    lightColor = Color.BLUE
                    enableVibration(true)
                    setShowBadge(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setBypassDnd(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showHabitNotification(context: Context, habit: Habit) {
        // Ensure default channel exists (for backward compatibility)
        ensureDefaultChannel(context)
        
        // Create or update habit-specific channel with custom sound
        val channelId = ensureHabitChannel(context, habit)
        
        val notificationManager = NotificationManagerCompat.from(context)
        
        Log.d("HabitReminderService", "════════════════════════════════════════════════════")
        Log.d("HabitReminderService", "Creating regular notification content intent")
        Log.d("HabitReminderService", "Habit ID: ${habit.id}, Title: ${habit.title}")
        
        // Create an intent that will launch the app's main activity regardless of which alias is active
        // Use the launcher intent to ensure it always works even when activity-alias changes
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val intent = launchIntent?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("habitId", habit.id)
            putExtra("openHabitDetails", true)
            Log.d("HabitReminderService", "Using launcher intent: ${component?.className}")
            Log.d("HabitReminderService", "Intent extras set: habitId=${habit.id}, openHabitDetails=true")
            Log.d("HabitReminderService", "Intent flags: $flags")
        } ?: Intent(context, MainActivity::class.java).apply {
            // Fallback if getLaunchIntentForPackage fails
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("habitId", habit.id)
            putExtra("openHabitDetails", true)
            Log.d("HabitReminderService", "Using fallback intent to MainActivity")
        }
        
        val contentIntent = PendingIntent.getActivity(
            context,
            habit.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        Log.d("HabitReminderService", "PendingIntent created with requestCode: ${habit.id.toInt()}")
        Log.d("HabitReminderService", "════════════════════════════════════════════════════")

        val reminderTime = timeFormatter.format(
            habit.toLocalTime().atDate(java.time.LocalDate.now())
        )

        val contentText = context.getString(
            R.string.notification_body,
            habit.description.ifBlank { context.getString(R.string.notification_default_description) },
            reminderTime
        )

        // Get current launcher icon dynamically
        val notificationIconResId = getCurrentLauncherIconResource(context)

        // Create small avatar for collapsed view (48dp)
        val smallAvatarBitmap = createLargeAvatarBitmap(habit.avatar, context, habit.id, 128)
        
        // Create large avatar for expanded view (96dp)
        val largeAvatarBitmap = createLargeAvatarBitmap(habit.avatar, context, habit.id, 256)

        // Create custom notification layout for collapsed view (small avatar, ellipsized text)
        val collapsedView = android.widget.RemoteViews(context.packageName, R.layout.notification_habit_reminder)
        collapsedView.setTextViewText(R.id.notification_title, habit.title)
        collapsedView.setTextViewText(R.id.notification_text, contentText)
        collapsedView.setImageViewBitmap(R.id.notification_avatar, smallAvatarBitmap)

        // Create custom notification layout for expanded view (large avatar, full text)
        val expandedView = android.widget.RemoteViews(context.packageName, R.layout.notification_habit_reminder_expanded)
        expandedView.setTextViewText(R.id.notification_title, habit.title)
        expandedView.setTextViewText(R.id.notification_text, contentText)
        expandedView.setImageViewBitmap(R.id.notification_avatar, largeAvatarBitmap)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(notificationIconResId) // Use current launcher icon dynamically
            .setColor(ContextCompat.getColor(context, R.color.teal_700)) // Teal/green color like Duolingo
            .setCustomContentView(collapsedView) // Small avatar + ellipsized text when collapsed
            .setCustomBigContentView(expandedView) // Large avatar + full text when expanded
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()) // Ensures proper styling
            .setContentIntent(contentIntent)
            .setAutoCancel(true) // Allow swipe to dismiss
            .setOngoing(false) // Not an ongoing notification
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for visibility
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setNumber(1) // Show badge number
            .setTicker(habit.title) // Legacy ticker text for older Android versions

        // For Android versions below O, set sound directly on notification
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val soundUri = NotificationSound.getActualUri(context, habit.getNotificationSound())
            if (soundUri != null) {
                notificationBuilder.setSound(soundUri)
                android.util.Log.d("HabitReminderService", "Set sound on notification (pre-O): $soundUri")
            } else {
                notificationBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND)
            }
            
            // Add custom vibration pattern
            val vibrationPattern = longArrayOf(0, 250, 250, 250)
            notificationBuilder.setVibrate(vibrationPattern)
        }
        
        // Enable heads-up notification (appears on top of screen)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setFullScreenIntent(contentIntent, false)
        }

        // Add action buttons
        val completeAction = NotificationCompat.Action.Builder(
            R.drawable.ic_check_24,
            context.getString(R.string.mark_as_completed),
            createCompleteActionPendingIntent(context, habit.id)
        ).build()

        val dismissAction = NotificationCompat.Action.Builder(
            R.drawable.ic_close_24,
            context.getString(R.string.dismiss),
            createDismissActionPendingIntent(context, habit.id)
        ).build()

        notificationBuilder
            .addAction(completeAction)
            .addAction(dismissAction)

        val notification = notificationBuilder.build()
        
        try {
            // Check if notifications are enabled before showing
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(habit.id.toInt(), notification)
                android.util.Log.d("HabitReminderService", "Notification shown for habit: ${habit.title} with sound: ${habit.notificationSoundName}")
                
                // Update widget to reflect current habit status
                it.atraj.habittracker.widget.HabitWidgetProvider.requestUpdate(context)
                android.util.Log.d("HabitReminderService", "Widget update requested after notification sent")
            } else {
                android.util.Log.w("HabitNotification", "Notifications are disabled by user")
            }
        } catch (e: SecurityException) {
            // Handle case where notification permission is not granted
            android.util.Log.e("HabitNotification", "Failed to show notification: ${e.message}")
        }
    }

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
                // Draw a simple icon (could be improved with actual vector drawable)
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
                // For custom images in notifications, try to load from cache using habit ID as key
                // This ensures cache works even when GitHub token in URL changes
                try {
                    android.util.Log.d("HabitReminderService", "Attempting to load custom image from cache for habit ID: $habitId")
                    
                    val imageLoader = coil.ImageLoader.Builder(context)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build()
                    
                    // Use habit ID as stable cache key (survives URL token changes)
                    val cacheKey = "habit_avatar_$habitId"
                    
                    // Only load from cache - disable network to prevent blocking
                    val request = coil.request.ImageRequest.Builder(context)
                        .data(avatar.value)
                        .size(size)
                        .allowHardware(false) // Required for bitmap conversion
                        .diskCacheKey(cacheKey) // Use stable cache key
                        .memoryCacheKey(cacheKey) // Use stable cache key
                        .diskCachePolicy(coil.request.CachePolicy.READ_ONLY) // Only read from cache
                        .memoryCachePolicy(coil.request.CachePolicy.READ_ONLY) // Only read from cache
                        .networkCachePolicy(coil.request.CachePolicy.DISABLED) // Disable network completely
                        .build()
                    
                    // Try to get from cache synchronously (fast, no network)
                    val drawable = imageLoader.diskCache?.get(cacheKey)?.use { snapshot ->
                        android.graphics.BitmapFactory.decodeFile(snapshot.data.toFile().absolutePath)
                    } ?: imageLoader.memoryCache?.get(coil.memory.MemoryCache.Key(cacheKey))?.bitmap
                    
                    if (drawable != null) {
                        android.util.Log.d("HabitReminderService", "Successfully loaded custom image from cache for notification")
                        // Create circular bitmap from cached image
                        val scaledBitmap = Bitmap.createScaledBitmap(drawable, size, size, true)
                        val circlePaint = Paint().apply {
                            isAntiAlias = true
                        }
                        canvas.drawBitmap(scaledBitmap, 0f, 0f, circlePaint)
                        return bitmap
                    } else {
                        android.util.Log.d("HabitReminderService", "Custom image not in cache, using fallback icon")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HabitReminderService", "Error checking cache for custom image: ${e.message}", e)
                }
                
                // Fallback if not in cache - show a camera icon
                android.util.Log.d("HabitReminderService", "Using fallback icon for custom image notification")
                val iconPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                    textSize = size * 0.5f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                }
                val textY = size / 2f - (iconPaint.descent() + iconPaint.ascent()) / 2
                // Use a camera emoji as placeholder for custom images
                canvas.drawText("📷", size / 2f, textY, iconPaint)
            }
        }
        
        return bitmap
    }

    // Create large avatar for Duolingo-style notification (bigger mascot image)
    private fun createLargeAvatarBitmap(avatar: HabitAvatar, context: Context, habitId: Long? = null, size: Int = 256): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw background circle with slight shadow/glow effect
        val backgroundPaint = Paint().apply {
            color = avatar.backgroundColor.toColorInt()
            isAntiAlias = true
            setShadowLayer(10f, 0f, 0f, android.graphics.Color.BLACK)
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, backgroundPaint)
        
        when (avatar.type) {
            HabitAvatarType.EMOJI -> {
                // Draw emoji much larger
                val textPaint = Paint().apply {
                    textSize = size * 0.65f  // Larger emoji
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                val textY = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(avatar.value, size / 2f, textY, textPaint)
            }
            HabitAvatarType.DEFAULT_ICON -> {
                // Draw larger default icon
                val iconPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                    strokeWidth = 12f
                    style = Paint.Style.STROKE
                }
                val centerX = size / 2f
                val centerY = size / 2f
                val radius = size * 0.25f
                canvas.drawCircle(centerX, centerY, radius, iconPaint)
            }
            HabitAvatarType.CUSTOM_IMAGE -> {
                // For custom images, try to load from cache
                try {
                    android.util.Log.d("HabitReminderService", "Loading large custom image for notification")
                    
                    val imageLoader = coil.ImageLoader.Builder(context)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build()
                    
                    val cacheKey = "habit_avatar_$habitId"
                    
                    val drawable = imageLoader.diskCache?.get(cacheKey)?.use { snapshot ->
                        android.graphics.BitmapFactory.decodeFile(snapshot.data.toFile().absolutePath)
                    } ?: imageLoader.memoryCache?.get(coil.memory.MemoryCache.Key(cacheKey))?.bitmap
                    
                    if (drawable != null) {
                        android.util.Log.d("HabitReminderService", "Successfully loaded large custom image")
                        // Create circular bitmap from cached image
                        val scaledBitmap = Bitmap.createScaledBitmap(drawable, size, size, true)
                        val circlePaint = Paint().apply {
                            isAntiAlias = true
                        }
                        canvas.drawBitmap(scaledBitmap, 0f, 0f, circlePaint)
                        return bitmap
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HabitReminderService", "Error loading large custom image", e)
                }
                
                // Fallback - larger camera icon
                val iconPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                    textSize = size * 0.55f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                }
                val textY = size / 2f - (iconPaint.descent() + iconPaint.ascent()) / 2
                canvas.drawText("📷", size / 2f, textY, iconPaint)
            }
        }
        
        return bitmap
    }

    private fun createCompleteActionPendingIntent(context: Context, habitId: Long): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "COMPLETE_HABIT"
            putExtra("habitId", habitId)
        }
        return PendingIntent.getBroadcast(
            context,
            "complete_$habitId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createDismissActionPendingIntent(context: Context, habitId: Long): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "DISMISS_HABIT"
            putExtra("habitId", habitId)
        }
        return PendingIntent.getBroadcast(
            context,
            "dismiss_$habitId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun dismissNotification(context: Context, habitId: Long) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(habitId.toInt())
    }
}

private fun Habit.toLocalTime(): java.time.LocalTime =
    java.time.LocalTime.of(reminderHour, reminderMinute)
