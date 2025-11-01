package it.atraj.habittracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import it.atraj.habittracker.auth.AuthRepository
import it.atraj.habittracker.data.HabitRepository
import it.atraj.habittracker.data.local.HabitFrequency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * BroadcastReceiver for daily completion check at 11:50 PM
 * Shows congratulatory notification if all habits for the day are completed
 */
@AndroidEntryPoint
class DailyCompletionReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var habitRepository: HabitRepository
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    companion object {
        private const val TAG = "DailyCompletionReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Daily completion check triggered at 11:50 PM")
        
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                checkAndShowCompletionNotification(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking daily completion", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    /**
     * Process all bad habits at end of day
     * Mark as completed if app wasn't used, leave unmarked if it was used
     */
    private suspend fun processBadHabitsForToday(context: Context) {
        try {
            Log.d(TAG, "Processing bad habits for end-of-day completion")
            
            // Get all bad habits
            val allHabits = habitRepository.getAllHabits()
            val badHabits = allHabits.filter { it.isBadHabit && !it.isDeleted }
            
            if (badHabits.isEmpty()) {
                Log.d(TAG, "No bad habits to process")
                return
            }
            
            val today = LocalDate.now()
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            
            for (habit in badHabits) {
                try {
                    // Check if already completed today
                    val completions = habitRepository.getHabitCompletions(habit.id)
                    val alreadyCompleted = completions.any { it.completedDate == today }
                    
                    if (alreadyCompleted) {
                        Log.d(TAG, "Bad habit '${habit.title}' already completed today")
                        continue
                    }
                    
                    val targetPackage = habit.targetAppPackageName
                    if (targetPackage.isNullOrBlank()) {
                        Log.d(TAG, "Bad habit '${habit.title}' has no target package, marking as completed")
                        // For custom app names without package, mark as completed (benefit of doubt)
                        habitRepository.markCompletedForDate(habit.id, today)
                        
                        // Increment totalCompletions count
                        val updatedHabit = habit.copy(totalCompletions = habit.totalCompletions + 1)
                        habitRepository.updateHabit(updatedHabit)
                        Log.d(TAG, "Incremented totalCompletions for '${habit.title}': ${updatedHabit.totalCompletions}")
                        continue
                    }
                    
                    // Get today's usage for the target app
                    val todayUsage = getTodayUsageForApp(context, targetPackage)
                    
                    Log.d(TAG, "Bad habit '${habit.title}': App $targetPackage usage today = ${todayUsage}ms")
                    
                    if (todayUsage == 0L) {
                        // Success! User didn't use the app today - mark as completed
                        Log.d(TAG, "Marking bad habit '${habit.title}' as completed (app not used)")
                        habitRepository.markCompletedForDate(habit.id, today)
                        
                        // Increment totalCompletions count
                        val updatedHabit = habit.copy(totalCompletions = habit.totalCompletions + 1)
                        habitRepository.updateHabit(updatedHabit)
                        Log.d(TAG, "Incremented totalCompletions for '${habit.title}': ${updatedHabit.totalCompletions}")
                    } else {
                        // Failure - user used the app, don't mark as completed
                        Log.d(TAG, "Bad habit '${habit.title}' NOT completed (app was used)")
                        // The absence of a completion record indicates failure
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing bad habit '${habit.title}'", e)
                }
            }
            
            Log.d(TAG, "Finished processing ${badHabits.size} bad habits")
        } catch (e: Exception) {
            Log.e(TAG, "Error in processBadHabitsForToday", e)
        }
    }
    
    /**
     * Get today's total usage time for a specific app
     */
    private fun getTodayUsageForApp(context: Context, packageName: String): Long {
        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            
            // Get start of today
            val startOfDay = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endTime = System.currentTimeMillis()
            
            val stats = usageStatsManager.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                startOfDay,
                endTime
            )
            
            if (stats == null) {
                Log.w(TAG, "Usage stats are null for $packageName")
                return 0
            }
            
            // Find the target app in the stats
            val appStats = stats.find { it.packageName == packageName }
            
            return appStats?.totalTimeInForeground ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting usage stats for $packageName", e)
            return 0
        }
    }
    
    private suspend fun checkAndShowCompletionNotification(context: Context) {
        try {
            // First, process bad habits to finalize today's status
            processBadHabitsForToday(context)
            
            // Get all active habits
            val allHabits = habitRepository.getAllHabits()
                .filter { !it.isDeleted && it.reminderEnabled }
            
            if (allHabits.isEmpty()) {
                Log.d(TAG, "No active habits, skipping completion check")
                return
            }
            
            // Get today's date
            val today = LocalDate.now()
            
            // Filter habits that are due today
            val habitsForToday = allHabits.filter { habit ->
                when (habit.frequency) {
                    HabitFrequency.DAILY -> true
                    HabitFrequency.WEEKLY -> {
                        val targetDay = habit.dayOfWeek ?: 1
                        today.dayOfWeek.value == targetDay
                    }
                    HabitFrequency.MONTHLY -> {
                        val targetDay = habit.dayOfMonth ?: 1
                        today.dayOfMonth == targetDay
                    }
                    HabitFrequency.YEARLY -> {
                        val targetMonth = habit.monthOfYear ?: 1
                        val targetDay = habit.dayOfMonth ?: 1
                        today.monthValue == targetMonth && today.dayOfMonth == targetDay
                    }
                }
            }
            
            if (habitsForToday.isEmpty()) {
                Log.d(TAG, "No habits due today, skipping completion check")
                return
            }
            
            // Check if all habits for today are completed
            var allCompleted = true
            for (habit in habitsForToday) {
                val completions = habitRepository.getHabitCompletions(habit.id)
                val completedToday = completions.any { it.completedDate == today }
                
                if (!completedToday) {
                    allCompleted = false
                    Log.d(TAG, "Habit '${habit.title}' not completed today")
                    break
                }
            }
            
            if (allCompleted) {
                // Get user name for personalization
                val user = authRepository.currentUser.firstOrNull()
                val userName = user?.displayName
                
                // Show congratulatory notification
                Log.d(TAG, "All ${habitsForToday.size} habits completed! Showing congratulatory notification")
                DailyCompletionNotificationService.showCompletionNotification(
                    context,
                    userName,
                    habitsForToday.size
                )
            } else {
                Log.d(TAG, "Not all habits completed, skipping notification")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in completion check", e)
        }
    }
}
