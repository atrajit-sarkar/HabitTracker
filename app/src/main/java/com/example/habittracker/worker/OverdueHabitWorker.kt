package it.atraj.habittracker.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import it.atraj.habittracker.service.OverdueHabitIconManager
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@HiltWorker
class OverdueHabitWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val iconManager: OverdueHabitIconManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "OverdueHabitWorker"
        private const val WORK_NAME = "overdue_habit_check_work"
        private const val CHECK_INTERVAL_MINUTES = 2L // Check every 2 minutes for maximum responsiveness
        
        /**
         * Schedule periodic checks for overdue habits
         */
        fun schedulePeriodicCheck(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<OverdueHabitWorker>(
                CHECK_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            ).build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            
            Log.d(TAG, "Scheduled periodic overdue habit checks every $CHECK_INTERVAL_MINUTES minutes")
        }
        
        /**
         * Schedule smart checks around habit due times for maximum responsiveness
         */
        fun scheduleSmartChecks(context: Context, upcomingHabitTimes: List<LocalDateTime>) {
            // Cancel existing immediate checks
            WorkManager.getInstance(context).cancelUniqueWork("smart_checks")
            
            // Schedule checks 1 minute before and after each habit due time
            upcomingHabitTimes.forEach { habitTime ->
                val checkTime = habitTime.plusMinutes(1) // 1 minute after due time
                val now = LocalDateTime.now()
                
                if (checkTime.isAfter(now)) {
                    val delayMinutes = ChronoUnit.MINUTES.between(now, checkTime)
                    
                    val workRequest = OneTimeWorkRequestBuilder<OverdueHabitWorker>()
                        .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                        .addTag("smart_check_${habitTime.toEpochSecond(java.time.ZoneOffset.UTC)}")
                        .build()
                    
                    WorkManager.getInstance(context).enqueue(workRequest)
                    Log.d(TAG, "Scheduled smart check for $delayMinutes minutes from now (habit due at $habitTime)")
                }
            }
        }
        
        /**
         * Trigger immediate icon check in background (avoids app restart)
         */
        fun triggerImmediateCheck(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<OverdueHabitWorker>()
                .addTag("immediate_check")
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                "immediate_icon_check",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            
            Log.d(TAG, "Triggered immediate overdue habit check")
        }
        
        /**
         * Cancel periodic checks
         */
        fun cancelPeriodicCheck(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Cancelled periodic overdue habit checks")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Checking for overdue habits...")
            
            // Check and update icon based on overdue habits
            iconManager.checkAndUpdateIcon()
            
            Log.d(TAG, "Overdue habit check completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check overdue habits", e)
            Result.retry()
        }
    }
}