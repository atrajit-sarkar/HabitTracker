package it.atraj.habittracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import it.atraj.habittracker.data.HabitRepository
import it.atraj.habittracker.data.local.Habit
import it.atraj.habittracker.email.EmailReminderWorker
import it.atraj.habittracker.email.SecureEmailStorage
import it.atraj.habittracker.auth.AuthRepository

@AndroidEntryPoint
class HabitReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var habitRepository: HabitRepository
    @Inject lateinit var reminderScheduler: HabitReminderScheduler
    @Inject lateinit var emailStorage: SecureEmailStorage
    @Inject lateinit var authRepository: AuthRepository

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
            // Show in-app notification
            withContext(Dispatchers.Main) {
                HabitReminderService.showHabitNotification(context, habit)
            }
            
            // Schedule email notification if configured
            scheduleEmailNotification(context, habit)
        }
    }
    
    private fun scheduleEmailNotification(context: Context, habit: Habit) {
        // Only schedule if email notifications are enabled
        if (!emailStorage.isConfigured()) {
            return
        }
        
        // Get user's display name for personalization
        val userName = authRepository.currentUserSync?.displayName
        
        // Create work request to send email in background
        val workRequest = OneTimeWorkRequestBuilder<EmailReminderWorker>()
            .setInputData(
                workDataOf(
                    EmailReminderWorker.KEY_HABIT_ID to habit.id,
                    EmailReminderWorker.KEY_USER_NAME to userName
                )
            )
            .build()
        
        // Use unique work with REPLACE policy to prevent duplicate emails
        // If multiple alarms fire simultaneously (e.g., after boot, time change),
        // only the latest work request will be kept
        val workName = "${EmailReminderWorker.WORK_NAME_PREFIX}${habit.id}"
        Log.d("HabitReminderReceiver", "Scheduling unique email work: $workName for habit: ${habit.title}")
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }
}

private fun Habit.isCompletedToday(): Boolean {
    val today = java.time.LocalDate.now()
    return lastCompletedDate == today
}
