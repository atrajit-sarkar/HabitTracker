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
    
    private suspend fun checkAndShowCompletionNotification(context: Context) {
        try {
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
