package com.example.habittracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.habittracker.data.HabitRepository
import com.example.habittracker.data.local.Habit

@AndroidEntryPoint
class HabitReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var habitRepository: HabitRepository
    @Inject lateinit var reminderScheduler: HabitReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(HabitReminderSchedulerImpl.HABIT_ID_KEY, -1L)
        android.util.Log.d("HabitReminderReceiver", "═══════════════════════════════════════")
        android.util.Log.d("HabitReminderReceiver", "Alarm triggered for habit ID: $habitId")
        android.util.Log.d("HabitReminderReceiver", "Time: ${java.time.LocalDateTime.now()}")
        
        if (habitId == -1L) {
            android.util.Log.e("HabitReminderReceiver", "Invalid habit ID received")
            return
        }
        
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habit = habitRepository.getHabitById(habitId)
                if (habit != null) {
                    android.util.Log.d("HabitReminderReceiver", "Found habit: ${habit.title}")
                    handleReminder(context, habit)
                } else {
                    android.util.Log.e("HabitReminderReceiver", "Habit not found for ID: $habitId")
                }
            } catch (e: Exception) {
                android.util.Log.e("HabitReminderReceiver", "Error processing reminder", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleReminder(context: Context, habit: Habit) {
        android.util.Log.d("HabitReminderReceiver", "Handling reminder for: ${habit.title}")
        
        // Reschedule for next occurrence
        reminderScheduler.schedule(habit)
        android.util.Log.d("HabitReminderReceiver", "Rescheduled next alarm")
        
        // Check if already completed today
        val shouldNotify = !habit.isCompletedToday()
        android.util.Log.d("HabitReminderReceiver", 
            "Should notify: $shouldNotify (lastCompleted: ${habit.lastCompletedDate}, today: ${java.time.LocalDate.now()})")
        
        if (shouldNotify) {
            withContext(Dispatchers.Main) {
                android.util.Log.d("HabitReminderReceiver", "Showing notification...")
                HabitReminderService.showHabitNotification(context, habit)
                android.util.Log.d("HabitReminderReceiver", "Notification shown successfully")
            }
        } else {
            android.util.Log.d("HabitReminderReceiver", "Skipping notification - already completed today")
        }
        android.util.Log.d("HabitReminderReceiver", "═══════════════════════════════════════")
    }
}

private fun Habit.isCompletedToday(): Boolean {
    val today = java.time.LocalDate.now()
    return lastCompletedDate == today
}
