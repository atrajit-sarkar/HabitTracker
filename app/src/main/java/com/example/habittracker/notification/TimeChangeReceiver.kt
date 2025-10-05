package it.atraj.habittracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import it.atraj.habittracker.data.HabitRepository
import javax.inject.Inject

/**
 * Reschedules all active habit reminders when system time, timezone, or date changes.
 * Helps keep alarms precise after:
 *  - User manually changes clock time
 *  - Daylight saving transitions
 *  - Timezone travel
 */
@AndroidEntryPoint
class TimeChangeReceiver : BroadcastReceiver() {

    @Inject lateinit var habitRepository: HabitRepository
    @Inject lateinit var reminderScheduler: HabitReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("TimeChangeReceiver", "Received action: $action. Rescheduling reminders...")
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habits = habitRepository.getAllHabits()
                var count = 0
                habits.filter { !it.isDeleted && it.reminderEnabled }.forEach { habit ->
                    try {
                        reminderScheduler.schedule(habit)
                        count++
                    } catch (e: Exception) {
                        Log.e("TimeChangeReceiver", "Failed to reschedule ${habit.title}: ${e.message}")
                    }
                }
                Log.d("TimeChangeReceiver", "Rescheduled $count reminders after time change event")
            } catch (e: Exception) {
                Log.e("TimeChangeReceiver", "Error loading habits: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
