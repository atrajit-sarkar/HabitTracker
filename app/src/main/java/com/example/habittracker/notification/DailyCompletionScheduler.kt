package it.atraj.habittracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for daily completion notification at 11:50 PM
 * Checks if all habits are completed and shows congratulatory message
 */
@Singleton
class DailyCompletionScheduler @Inject constructor(
    private val context: Context
) {
    
    private val alarmManager: AlarmManager? =
        ContextCompat.getSystemService(context, AlarmManager::class.java)
    
    companion object {
        private const val TAG = "DailyCompletionScheduler"
        private const val REQUEST_CODE = 888888
        
        // Schedule for 11:50 PM (23:50)
        private const val CHECK_HOUR = 23
        private const val CHECK_MINUTE = 50
    }
    
    /**
     * Schedule daily alarm at 11:50 PM to check for completion
     */
    fun scheduleDailyCheck() {
        try {
            val pendingIntent = createPendingIntent()
            val triggerTime = calculateNextTriggerTime()
            
            // Use setRepeating for daily recurring alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager?.canScheduleExactAlarms() == false) {
                    // Fallback to inexact alarm if exact permission not granted
                    alarmManager?.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled inexact daily completion check at 11:50 PM")
                    return
                }
            }
            
            // Schedule exact repeating alarm
            alarmManager?.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            
            Log.d(TAG, "Scheduled daily completion check at 11:50 PM every day")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling daily completion check", e)
        }
    }
    
    /**
     * Cancel daily completion check
     */
    fun cancelDailyCheck() {
        try {
            val pendingIntent = createPendingIntent()
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled daily completion check")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling daily completion check", e)
        }
    }
    
    /**
     * Calculate next trigger time for 11:50 PM
     */
    private fun calculateNextTriggerTime(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, CHECK_HOUR)
            set(Calendar.MINUTE, CHECK_MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // If 11:50 PM has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        Log.d(TAG, "Next daily completion check scheduled for: ${calendar.time}")
        return calendar.timeInMillis
    }
    
    /**
     * Create PendingIntent for daily completion check
     */
    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(context, DailyCompletionReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }
}
