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
    private const val CHANNEL_ID = "habit_reminder_channel"
    private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_description)
                    enableLights(true)
                    lightColor = Color.BLUE
                    enableVibration(true)
                    // Enable heads-up notifications and status bar icon
                    setShowBadge(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    // Allow notification to bypass DND
                    setBypassDnd(false)
                    // Note: For per-habit custom sounds, we'll set sound on individual notifications
                    setSound(null, null)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showHabitNotification(context: Context, habit: Habit) {
        ensureChannel(context)
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

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_habit) // Use proper notification icon
            .setColor(ContextCompat.getColor(context, R.color.purple_500))
            .setContentTitle(habit.title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(contentIntent)
            .setAutoCancel(true) // Allow swipe to dismiss
            .setOngoing(false) // Not an ongoing notification
            .setPriority(NotificationCompat.PRIORITY_MAX) // Maximum priority for status bar
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Default sound, vibration, lights
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setNumber(1) // Show badge number
            .setTicker(habit.title) // Legacy ticker text for older Android versions

        // Set avatar as large icon
        val avatarBitmap = createAvatarBitmap(habit.avatar)
        notificationBuilder.setLargeIcon(avatarBitmap)

        // Set custom sound for the habit
        val soundUri = habit.notificationSound.getUri(context)
        if (soundUri != null) {
            notificationBuilder.setSound(soundUri)
        } else {
            // Use default notification sound if custom sound not available
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND)
        }
        
        // Add custom vibration pattern based on sound type
        val vibrationPattern = when (habit.notificationSound) {
            NotificationSound.DEFAULT -> longArrayOf(0, 250, 250, 250)
            NotificationSound.RINGTONE -> longArrayOf(0, 500, 200, 500)
            NotificationSound.ALARM -> longArrayOf(0, 100, 100, 100, 100, 100)
            NotificationSound.SYSTEM_DEFAULT -> longArrayOf(0, 300, 200, 300)
        }
        notificationBuilder.setVibrate(vibrationPattern)
        
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
