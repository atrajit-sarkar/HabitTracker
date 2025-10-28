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
 * Receiver that reschedules all habit reminders after device boot or app update.
 * Critical for ensuring notifications work after phone restart.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var habitRepository: HabitRepository

    @Inject
    lateinit var reminderScheduler: HabitReminderScheduler
    
    @Inject
    lateinit var overdueScheduler: OverdueNotificationScheduler
    
    @Inject
    lateinit var dailyCompletionScheduler: DailyCompletionScheduler

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d("BootReceiver", "Device booted or app updated, rescheduling reminders...")
                
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        rescheduleAllReminders(context)
                    } catch (e: Exception) {
                        Log.e("BootReceiver", "Error rescheduling reminders: ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private suspend fun rescheduleAllReminders(context: Context) {
        try {
            val habits = habitRepository.getAllHabits()
            var rescheduled = 0
            
            habits.filter { !it.isDeleted && it.reminderEnabled }.forEach { habit ->
                try {
                    reminderScheduler.schedule(habit)
                    overdueScheduler.scheduleOverdueChecks(habit)
                    rescheduled++
                    Log.d("BootReceiver", "Rescheduled reminder for: ${habit.title}")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule ${habit.title}: ${e.message}")
                }
            }
            
            // Reschedule daily completion check at 11:50 PM
            dailyCompletionScheduler.scheduleDailyCheck()
            Log.d("BootReceiver", "Rescheduled daily completion check at 11:50 PM")
            
            Log.d("BootReceiver", "Successfully rescheduled $rescheduled reminders out of ${habits.size} total habits")
        } catch (e: Exception) {
            Log.e("BootReceiver", "Error getting habits from repository: ${e.message}", e)
        }
    }
}
