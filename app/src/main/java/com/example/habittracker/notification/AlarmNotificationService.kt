package com.example.habittracker.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.habittracker.MainActivity
import com.example.habittracker.R

class AlarmNotificationService : Service() {
    
    private var ringtone: android.media.Ringtone? = null
    private var vibrator: Vibrator? = null
    
    companion object {
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_TITLE = "habit_title"
        const val EXTRA_SOUND_URI = "sound_uri"
        const val CHANNEL_ID = "habit_alarm_channel"
        const val NOTIFICATION_ID = 9999
        
        fun start(context: Context, habitId: Long, habitTitle: String, soundUri: String?) {
            val intent = Intent(context, AlarmNotificationService::class.java).apply {
                putExtra(EXTRA_HABIT_ID, habitId)
                putExtra(EXTRA_HABIT_TITLE, habitTitle)
                putExtra(EXTRA_SOUND_URI, soundUri)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            android.util.Log.d("AlarmNotificationService", "Started alarm service for habit: $habitTitle")
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmNotificationService::class.java))
            android.util.Log.d("AlarmNotificationService", "Stopped alarm service")
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        android.util.Log.d("AlarmNotificationService", "Service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val habitId = intent?.getLongExtra(EXTRA_HABIT_ID, 0L) ?: 0L
        val habitTitle = intent?.getStringExtra(EXTRA_HABIT_TITLE) ?: "Habit Reminder"
        val soundUriString = intent?.getStringExtra(EXTRA_SOUND_URI)
        
        android.util.Log.d("AlarmNotificationService", "onStartCommand: habitId=$habitId, title=$habitTitle, soundUri=$soundUriString")
        
        // Start ringing
        startRinging(soundUriString)
        
        // Show notification
        val notification = buildNotification(habitId, habitTitle)
        startForeground(NOTIFICATION_ID, notification)
        
        android.util.Log.d("AlarmNotificationService", "Alarm started successfully")
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        android.util.Log.d("AlarmNotificationService", "Service destroying, stopping ringtone and vibration")
        stopRinging()
        super.onDestroy()
    }
    
    private fun startRinging(soundUriString: String?) {
        // Start sound
        val soundUri = try {
            if (!soundUriString.isNullOrEmpty()) {
                Uri.parse(soundUriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmNotificationService", "Error parsing sound URI", e)
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
        
        android.util.Log.d("AlarmNotificationService", "Starting ringtone with URI: $soundUri")
        
        ringtone = RingtoneManager.getRingtone(this, soundUri)
        ringtone?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isLooping = true
            }
            play()
            android.util.Log.d("AlarmNotificationService", "Ringtone started playing")
        }
        
        // Start vibration
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 1000, 500, 1000, 500)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            val pattern = longArrayOf(0, 1000, 500, 1000, 500)
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
        android.util.Log.d("AlarmNotificationService", "Vibration started")
    }
    
    private fun stopRinging() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
        android.util.Log.d("AlarmNotificationService", "Ringtone and vibration stopped")
    }
    
    private fun buildNotification(habitId: Long, habitTitle: String): Notification {
        // Open app intent
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Mark as done intent
        val doneIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_DONE
            putExtra(NotificationActionReceiver.EXTRA_HABIT_ID, habitId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            this, habitId.toInt(), doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⏰ $habitTitle")
            .setContentText("Time to complete your habit! Tap to open or mark as done.")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(openPendingIntent, true)
            .setContentIntent(openPendingIntent)
            .addAction(
                R.drawable.ic_check,
                "Mark as Done",
                donePendingIntent
            )
            .setVibrate(null) // We handle vibration ourselves
            .setSound(null) // We handle sound ourselves
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Habit Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm-style notifications for habits that require immediate attention"
                setSound(null, null) // We handle sound ourselves
                enableVibration(false) // We handle vibration ourselves
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            android.util.Log.d("AlarmNotificationService", "Notification channel created")
        }
    }
}
