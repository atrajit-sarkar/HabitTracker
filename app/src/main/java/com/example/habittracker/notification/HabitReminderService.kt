package com.example.habittracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.habittracker.MainActivity
import com.example.habittracker.R
import com.example.habittracker.data.local.Habit
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
                    lightColor = Color.MAGENTA
                    enableVibration(true)
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
            .setSmallIcon(R.drawable.ic_notification_habit)
            .setColor(ContextCompat.getColor(context, R.color.purple_500))
            .setContentTitle(habit.title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Set custom sound for the habit
        val soundUri = habit.notificationSound.getUri(context)
        if (soundUri != null) {
            notificationBuilder.setSound(soundUri)
        }
        
        // Add custom vibration pattern based on sound type
        val vibrationPattern = when (habit.notificationSound) {
            NotificationSound.DEFAULT -> longArrayOf(0, 250, 250, 250)
            NotificationSound.RINGTONE -> longArrayOf(0, 500, 200, 500)
            NotificationSound.ALARM -> longArrayOf(0, 100, 100, 100, 100, 100)
            NotificationSound.SYSTEM_DEFAULT -> longArrayOf(0, 300, 200, 300)
        }
        notificationBuilder.setVibrate(vibrationPattern)

        val notification = notificationBuilder.build()

        notificationManager.notify(habit.id.toInt(), notification)
    }
}

private fun Habit.toLocalTime(): java.time.LocalTime =
    java.time.LocalTime.of(reminderHour, reminderMinute)
