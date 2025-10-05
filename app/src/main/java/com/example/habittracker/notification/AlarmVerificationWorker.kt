package it.atraj.habittracker.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import it.atraj.habittracker.data.HabitRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker that periodically verifies and reschedules habit reminders.
 * This serves as a backup mechanism if AlarmManager fails or alarms get cancelled by the system.
 */
@HiltWorker
class AlarmVerificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val reminderScheduler: HabitReminderScheduler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("AlarmVerificationWorker", "Starting alarm verification...")
            
            val habits = habitRepository.getAllHabits()
            var verified = 0
            var rescheduled = 0
            
            habits.filter { !it.isDeleted && it.reminderEnabled }.forEach { habit ->
                try {
                    // Reschedule all active reminders to ensure they're registered
                    reminderScheduler.schedule(habit)
                    verified++
                    rescheduled++
                    Log.d("AlarmVerificationWorker", "Verified/rescheduled reminder for: ${habit.title}")
                } catch (e: Exception) {
                    Log.e("AlarmVerificationWorker", "Failed to verify ${habit.title}: ${e.message}")
                }
            }
            
            Log.d("AlarmVerificationWorker", 
                "Alarm verification complete: verified $verified habits, rescheduled $rescheduled reminders")
            
            Result.success()
        } catch (e: Exception) {
            Log.e("AlarmVerificationWorker", "Error during alarm verification: ${e.message}", e)
            Result.retry()
        }
    }
}
