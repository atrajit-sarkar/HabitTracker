package it.atraj.habittracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import it.atraj.habittracker.auth.AuthRepository
import it.atraj.habittracker.data.HabitRepository
import it.atraj.habittracker.util.OverdueHabitChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * BroadcastReceiver for handling overdue notification alarms
 * Triggered by AlarmManager at 2, 3, 4, 5, and 6 hours after habit due time
 */
@AndroidEntryPoint
class OverdueNotificationReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var habitRepository: HabitRepository
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    companion object {
        private const val TAG = "OverdueNotificationReceiver"
        const val HABIT_ID_KEY = "habit_id"
        const val OVERDUE_HOURS_KEY = "overdue_hours"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(HABIT_ID_KEY, -1L)
        val overdueHours = intent.getIntExtra(OVERDUE_HOURS_KEY, -1)
        
        if (habitId == -1L || overdueHours == -1) {
            Log.w(TAG, "Invalid habit ID or overdue hours")
            return
        }
        
        Log.d(TAG, "Overdue alarm triggered for habit: $habitId, overdue: ${overdueHours}h")
        
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                checkAndShowOverdueNotification(context, habitId, overdueHours)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling overdue notification", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    private suspend fun checkAndShowOverdueNotification(
        context: Context,
        habitId: Long,
        overdueHours: Int
    ) {
        // Get the habit
        val habit = habitRepository.getHabitById(habitId)
        if (habit == null) {
            Log.w(TAG, "Habit not found: $habitId")
            return
        }
        
        // Skip if habit is deleted or reminder is disabled
        if (habit.isDeleted || !habit.reminderEnabled) {
            Log.d(TAG, "Habit deleted or reminder disabled, skipping")
            return
        }
        
        // Get completions for this habit
        val completions = habitRepository.getHabitCompletions(habitId)
        
        // Check if habit is actually overdue
        val overdueStatus = OverdueHabitChecker.checkHabitOverdueStatus(
            habit,
            completions,
            LocalDateTime.now()
        )
        
        // Only show notification if habit is still overdue and matches the expected duration
        if (overdueStatus.isOverdue && overdueStatus.overdueHours >= overdueHours) {
            // Get user name for personalization
            val user = authRepository.currentUser.firstOrNull()
            val userName = user?.displayName
            
            // Show overdue notification
            Log.d(TAG, "Showing overdue notification for habit: ${habit.title}, overdue: ${overdueHours}h")
            OverdueNotificationService.showOverdueNotification(
                context,
                habit,
                overdueHours,
                userName
            )
        } else {
            Log.d(TAG, "Habit no longer overdue or completed, skipping notification")
        }
    }
}
