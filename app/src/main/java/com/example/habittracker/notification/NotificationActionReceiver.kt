package com.example.habittracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.habittracker.data.HabitRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var habitRepository: HabitRepository

    companion object {
        const val ACTION_MARK_DONE = "COMPLETE_HABIT"
        const val ACTION_DISMISS = "DISMISS_HABIT"
        const val EXTRA_HABIT_ID = "habitId"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId == -1L) return

        android.util.Log.d("NotificationActionReceiver", "Received action: ${intent.action} for habitId: $habitId")

        when (intent.action) {
            ACTION_MARK_DONE -> {
                // Stop alarm service if running
                AlarmNotificationService.stop(context)
                android.util.Log.d("NotificationActionReceiver", "Stopped alarm service")
                
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        habitRepository.markCompletedToday(habitId)
                        HabitReminderService.dismissNotification(context, habitId)
                        android.util.Log.d("NotificationActionReceiver", "Habit marked as complete: $habitId")
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationActionReceiver", "Error marking habit complete", e)
                        e.printStackTrace()
                    }
                }
            }
            ACTION_DISMISS -> {
                // Stop alarm service if running
                AlarmNotificationService.stop(context)
                HabitReminderService.dismissNotification(context, habitId)
                android.util.Log.d("NotificationActionReceiver", "Dismissed notification for habit: $habitId")
            }
        }
    }
}