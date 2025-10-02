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
        if (habitId == -1L) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val habit = habitRepository.getHabitById(habitId)
            if (habit != null) {
                handleReminder(context, habit)
            }
            pendingResult.finish()
        }
    }

    private suspend fun handleReminder(context: Context, habit: Habit) {
        reminderScheduler.schedule(habit)
        val shouldNotify = !habit.isCompletedToday()
        
        android.util.Log.d("HabitReminderReceiver", 
            "Habit: ${habit.title}, Completed today: ${habit.isCompletedToday()}, Should notify: $shouldNotify, IsAlarmType: ${habit.isAlarmType}")
        
        if (shouldNotify) {
            // Check if this is an alarm-type notification
            if (habit.isAlarmType) {
                // Start alarm service for continuous ringing
                withContext(Dispatchers.Main) {
                    AlarmNotificationService.start(
                        context,
                        habit.id,
                        habit.title,
                        habit.notificationSoundUri.ifEmpty { null }
                    )
                }
                android.util.Log.d("HabitReminderReceiver", "Started alarm service for: ${habit.title}")
            } else {
                // Show regular notification
                withContext(Dispatchers.Main) {
                    HabitReminderService.showHabitNotification(context, habit)
                }
                android.util.Log.d("HabitReminderReceiver", "Showed regular notification for: ${habit.title}")
            }
        }
    }
}

private fun Habit.isCompletedToday(): Boolean {
    val today = java.time.LocalDate.now()
    return lastCompletedDate == today
}
