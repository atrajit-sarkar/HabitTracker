package it.atraj.habittracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import it.atraj.habittracker.notification.NotificationReliabilityHelper
import it.atraj.habittracker.service.OverdueHabitIconManager
import it.atraj.habittracker.service.AppIconManager
import it.atraj.habittracker.worker.OverdueHabitWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HabitTrackerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var iconManager: OverdueHabitIconManager
    
    @Inject
    lateinit var appIconManager: AppIconManager

    override fun onCreate() {
        super.onCreate()
        
        // Set up periodic alarm verification using WorkManager
        NotificationReliabilityHelper.setupAlarmVerification(this)
        
        // Initialize icon manager for overdue habit warnings only
        iconManager.initialize()
        
        // Schedule periodic checks for overdue habits
        OverdueHabitWorker.schedulePeriodicCheck(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
