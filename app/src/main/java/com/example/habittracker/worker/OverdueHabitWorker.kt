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
        
        // Production-ready intervals
        private const val PERIODIC_CHECK_HOURS = 1L // Check every hour as fallback
        private const val SMART_CHECK_DELAY_MINUTES = 5L // Check 5 minutes after habit due time
        
        /**
         * Schedule periodic checks for overdue habits
         * Uses 1-hour interval as fallback - main checks are smart-scheduled around habit times
         */
        fun schedulePeriodicCheck(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<OverdueHabitWorker>(
                PERIODIC_CHECK_HOURS,
                TimeUnit.HOURS
            ).build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            
            Log.d(TAG, "Scheduled periodic overdue habit checks every $PERIODIC_CHECK_HOURS hour(s)")
        }
        
        /**
         * Schedule smart checks around habit due times for maximum responsiveness
         * Checks 5 minutes after each habit's due time to detect overdue status
         */
        fun scheduleSmartChecks(context: Context, upcomingHabitTimes: List<LocalDateTime>) {
            // Cancel existing smart checks
            WorkManager.getInstance(context).cancelAllWorkByTag("smart_check")
            
            // Schedule checks after each habit due time
            upcomingHabitTimes.take(10).forEach { habitTime -> // Limit to next 10 habits
                val checkTime = habitTime.plusMinutes(SMART_CHECK_DELAY_MINUTES)
                val now = LocalDateTime.now()
                
                if (checkTime.isAfter(now)) {
                    val delayMinutes = ChronoUnit.MINUTES.between(now, checkTime)
                    
                    // Only schedule if within next 24 hours
                    if (delayMinutes <= 1440) { // 24 hours
                        val workRequest = OneTimeWorkRequestBuilder<OverdueHabitWorker>()
                            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                            .addTag("smart_check")
                            .build()
                        
                        WorkManager.getInstance(context).enqueue(workRequest)
                        Log.d(TAG, "Scheduled smart check in $delayMinutes minutes (habit due at $habitTime)")
                    }
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