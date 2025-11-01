package it.atraj.habittracker.worker

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import it.atraj.habittracker.data.HabitRepository
import it.atraj.habittracker.notification.BadHabitNotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that checks app usage for bad habits
 * Runs every 2 hours to monitor if tracked apps are being used
 */
@HiltWorker
class AppUsageTrackingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val habitRepository: HabitRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d(TAG, "AppUsageTrackingWorker started")
            
            // Get all bad habits
            val allHabits = habitRepository.getAllHabits()
            val badHabits = allHabits.filter { it.isBadHabit && !it.isDeleted }
            
            android.util.Log.d(TAG, "Found ${badHabits.size} bad habits to check")
            
            if (badHabits.isEmpty()) {
                return@withContext Result.success()
            }
            
            // Check if usage stats permission is granted
            val usageStatsManager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val currentTime = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                currentTime - 1000 * 60,
                currentTime
            )
            
            if (stats == null || stats.isEmpty()) {
                android.util.Log.w(TAG, "Usage stats permission not granted or no stats available")
                return@withContext Result.retry()
            }
            
            // Check each bad habit
            for (habit in badHabits) {
                checkBadHabit(habit)
            }
            
            android.util.Log.d(TAG, "AppUsageTrackingWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in AppUsageTrackingWorker", e)
            Result.retry()
        }
    }
    
    private suspend fun checkBadHabit(habit: it.atraj.habittracker.data.local.Habit) {
        val today = LocalDate.now()
        
        val targetPackage = habit.targetAppPackageName
        
        if (targetPackage.isNullOrBlank()) {
            android.util.Log.d(TAG, "Habit ${habit.title} has no target package, checking by name only")
            // For custom app names without package, we can't check usage
            // Just send encouragement notification and mark as completed (once per day)
            val completions = habitRepository.getHabitCompletions(habit.id)
            val alreadyCompletedToday = completions.any { it.completedDate == today }
            
            if (!alreadyCompletedToday) {
                sendEncouragementNotification(habit)
                markBadHabitAsCompleted(habit, today)
            } else {
                android.util.Log.d(TAG, "Habit ${habit.title} already has completion for today, skipping")
            }
            return
        }
        
        // Get today's usage for the target app
        val todayUsage = getTodayUsageForApp(targetPackage)
        
        android.util.Log.d(TAG, "Habit ${habit.title}: App $targetPackage usage today = ${todayUsage}ms")
        
        // Get existing completions to check if already marked today
        val completions = habitRepository.getHabitCompletions(habit.id)
        val alreadyCompletedToday = completions.any { it.completedDate == today }
        
        if (todayUsage > 0) {
            // App was used - send disappointment notification
            android.util.Log.d(TAG, "App was used! Sending disappointment notification")
            sendDisappointmentNotification(habit, todayUsage)
            // Mark as failed for today (remove completion if it exists)
            markBadHabitAsFailed(habit, today, alreadyCompletedToday)
        } else {
            // App was not used - send congratulation notification
            if (!alreadyCompletedToday) {
                android.util.Log.d(TAG, "App was NOT used! Sending congratulation notification")
                sendEncouragementNotification(habit)
                // Mark as completed for today
                markBadHabitAsCompleted(habit, today)
            } else {
                android.util.Log.d(TAG, "App still not used and already marked completed today")
            }
        }
    }
    
    private fun getTodayUsageForApp(packageName: String): Long {
        val usageStatsManager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        // Get start of today
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTime = System.currentTimeMillis()
        
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startOfDay,
            endTime
        )
        
        if (stats == null) {
            android.util.Log.w(TAG, "Usage stats are null for $packageName")
            return 0
        }
        
        // Find the target app in the stats
        val appStats = stats.find { it.packageName == packageName }
        
        return appStats?.totalTimeInForeground ?: 0
    }
    
    private suspend fun markBadHabitAsCompleted(habit: it.atraj.habittracker.data.local.Habit, date: LocalDate) {
        // Mark habit as completed for this date
        habitRepository.markCompletedForDate(habit.id, date)
        
        // Update last check date and increment total completions
        val updatedHabit = habit.copy(
            totalCompletions = habit.totalCompletions + 1,
            lastAppUsageCheckDate = date,
            lastCompletedDate = date
        )
        
        habitRepository.updateHabit(updatedHabit)
        android.util.Log.d(TAG, "✅ Marked bad habit ${habit.title} as completed for $date. Total completions: ${updatedHabit.totalCompletions}")
    }
    
    private suspend fun markBadHabitAsFailed(habit: it.atraj.habittracker.data.local.Habit, date: LocalDate, hadCompletion: Boolean) {
        // Update habit - decrement completions if we had marked it as completed earlier today
        val newCompletionCount = if (hadCompletion) {
            maxOf(0, habit.totalCompletions - 1)
        } else {
            habit.totalCompletions
        }
        
        val updatedHabit = habit.copy(
            totalCompletions = newCompletionCount,
            lastAppUsageCheckDate = date
        )
        
        habitRepository.updateHabit(updatedHabit)
        
        if (hadCompletion) {
            android.util.Log.d(TAG, "❌ User failed after initial success! Decremented completion count for ${habit.title}")
        } else {
            android.util.Log.d(TAG, "❌ Marked bad habit ${habit.title} as failed for $date")
        }
    }
    
    private fun sendEncouragementNotification(habit: it.atraj.habittracker.data.local.Habit) {
        BadHabitNotificationService.sendEncouragementNotification(
            context = applicationContext,
            habit = habit
        )
    }
    
    private fun sendDisappointmentNotification(habit: it.atraj.habittracker.data.local.Habit, usageTimeMs: Long) {
        BadHabitNotificationService.sendDisappointmentNotification(
            context = applicationContext,
            habit = habit,
            usageTimeMs = usageTimeMs
        )
    }
    
    companion object {
        private const val TAG = "AppUsageWorker"
        private const val WORK_NAME_PREFIX = "app_usage_check_"
        private const val IMMEDIATE_WORK_PREFIX = "immediate_check_"
        
        /**
         * Schedule an immediate check followed by periodic checks for a bad habit
         */
        fun scheduleWithImmediateCheck(context: Context, habitId: Long) {
            // First, schedule an immediate one-time check
            val immediateRequest = OneTimeWorkRequestBuilder<AppUsageTrackingWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .addTag("bad_habit_$habitId")
                .addTag("immediate_check")
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "$IMMEDIATE_WORK_PREFIX$habitId",
                    ExistingWorkPolicy.REPLACE, // Replace any existing immediate check
                    immediateRequest
                )
            
            android.util.Log.d(TAG, "Scheduled immediate app usage check for habit $habitId")
            
            // Then schedule the periodic checks
            schedule(context, habitId)
        }
        
        /**
         * Schedule periodic app usage checking for a bad habit
         */
        fun schedule(context: Context, habitId: Long) {
            val workRequest = PeriodicWorkRequestBuilder<AppUsageTrackingWorker>(
                2, TimeUnit.HOURS, // Run every 2 hours
                30, TimeUnit.MINUTES // With 30 minute flex period
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false) // Run even on low battery
                        .build()
                )
                .addTag("bad_habit_$habitId")
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "$WORK_NAME_PREFIX$habitId",
                    ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
                    workRequest
                )
            
            android.util.Log.d(TAG, "Scheduled periodic app usage tracking for habit $habitId (every 2 hours)")
        }
        
        /**
         * Cancel app usage checking for a bad habit
         */
        fun cancel(context: Context, habitId: Long) {
            WorkManager.getInstance(context)
                .cancelUniqueWork("$WORK_NAME_PREFIX$habitId")
            WorkManager.getInstance(context)
                .cancelUniqueWork("$IMMEDIATE_WORK_PREFIX$habitId")
            android.util.Log.d(TAG, "Cancelled app usage tracking for habit $habitId")
        }
    }
}
