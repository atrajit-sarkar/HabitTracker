package it.atraj.habittracker.email

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import it.atraj.habittracker.data.HabitRepository

/**
 * WorkManager worker to send email notifications in the background.
 * This ensures emails are sent reliably even if the app is closed.
 */
@HiltWorker
class EmailReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val emailService: EmailNotificationService,
    private val habitRepository: HabitRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val habitId = inputData.getLong(KEY_HABIT_ID, -1L)
            val userName = inputData.getString(KEY_USER_NAME)
            
            if (habitId == -1L) {
                return Result.failure()
            }
            
            // Get the habit from repository
            val habit = habitRepository.getHabitById(habitId)
            
            if (habit == null) {
                return Result.failure()
            }
            
            // Send the email
            val result = emailService.sendHabitReminderEmail(habit, userName)
            
            when (result) {
                is EmailResult.Success -> Result.success()
                is EmailResult.NotConfigured -> Result.success() // Not an error, user hasn't configured email
                is EmailResult.NoRecipient -> Result.success() // Not an error, user hasn't set their email
                is EmailResult.Error -> Result.retry() // Retry on errors (network issues, etc.)
            }
            
        } catch (e: Exception) {
            // On error, retry the work
            Result.retry()
        }
    }

    companion object {
        const val KEY_HABIT_ID = "habit_id"
        const val KEY_USER_NAME = "user_name"
        const val WORK_NAME_PREFIX = "email_reminder_"
    }
}

