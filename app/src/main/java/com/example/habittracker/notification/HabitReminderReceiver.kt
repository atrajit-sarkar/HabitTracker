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
        if (shouldNotify) {
            withContext(Dispatchers.Main) {
                HabitReminderService.showHabitNotification(context, habit)
            }
        }
    }
}

private fun Habit.isCompletedToday(): Boolean {
    val today = java.time.LocalDate.now()
    return lastCompletedDate == today
}
