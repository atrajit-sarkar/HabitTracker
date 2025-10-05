package it.atraj.habittracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import it.atraj.habittracker.data.HabitRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var habitRepository: HabitRepository

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra("habitId", -1L)
        if (habitId == -1L) return

        when (intent.action) {
            "COMPLETE_HABIT" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        habitRepository.markCompletedToday(habitId)
                        HabitReminderService.dismissNotification(context, habitId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            "DISMISS_HABIT" -> {
                HabitReminderService.dismissNotification(context, habitId)
            }
        }
    }
}
