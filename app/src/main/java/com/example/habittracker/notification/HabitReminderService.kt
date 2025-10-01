package com.example.habittracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.toColorInt
import com.example.habittracker.MainActivity
import com.example.habittracker.R
import com.example.habittracker.data.local.Habit
import com.example.habittracker.data.local.HabitAvatar
import com.example.habittracker.data.local.HabitAvatarType
import com.example.habittracker.data.local.NotificationSound
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object HabitReminderService {
    private const val CHANNEL_PREFIX = "habit_reminder_channel_"
    private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    /**
     * Create or update a notification channel for a specific habit
     * This allows each habit to have its own custom sound
     */
    private fun ensureHabitChannel(context: Context, habit: Habit): String {
        val channelId = "${CHANNEL_PREFIX}${habit.id}"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
                ?: return channelId
            
            // Check if channel already exists
            val existing = manager.getNotificationChannel(channelId)
            
            // Delete existing channel if sound has changed
            if (existing != null) {
                val currentSoundUri = existing.sound
                val newSoundUri = NotificationSound.getActualUri(context, habit.getNotificationSound())
                
                // If sounds are different, delete and recreate the channel
                if (currentSoundUri != newSoundUri) {
                    manager.deleteNotificationChannel(channelId)
                }
            }
            
            // Create or recreate the channel
            if (manager.getNotificationChannel(channelId) == null) {
                val soundUri = NotificationSound.getActualUri(context, habit.getNotificationSound())
                
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
                    }
                }
                manager.createNotificationChannel(channel)
                android.util.Log.d("HabitReminderService", "Created channel $channelId with sound: ${soundUri}")
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
        val contentIntent = PendingIntent.getActivity(
            context,
            habit.id.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("habitId", habit.id)
                putExtra("openHabitDetails", true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val reminderTime = timeFormatter.format(
            habit.toLocalTime().atDate(java.time.LocalDate.now())
        )

        val contentText = context.getString(
            R.string.notification_body,
            habit.description.ifBlank { context.getString(R.string.notification_default_description) },
            reminderTime
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_habit) // Use proper notification icon
            .setColor(ContextCompat.getColor(context, R.color.purple_500))
            .setContentTitle(habit.title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
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

        // Set avatar as large icon
        val avatarBitmap = createAvatarBitmap(habit.avatar)
        notificationBuilder.setLargeIcon(avatarBitmap)

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
            } else {
                android.util.Log.w("HabitNotification", "Notifications are disabled by user")
            }
        } catch (e: SecurityException) {
            // Handle case where notification permission is not granted
            android.util.Log.e("HabitNotification", "Failed to show notification: ${e.message}")
        }
    }

    private fun createAvatarBitmap(avatar: HabitAvatar): Bitmap {
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
                // Future implementation for custom images
                val iconPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                    textSize = size * 0.4f
                    textAlign = Paint.Align.CENTER
                }
                val textY = size / 2f - (iconPaint.descent() + iconPaint.ascent()) / 2
                canvas.drawText("IMG", size / 2f, textY, iconPaint)
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
