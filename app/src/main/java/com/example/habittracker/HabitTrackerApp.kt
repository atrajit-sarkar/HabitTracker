package it.atraj.habittracker

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import it.atraj.habittracker.data.HabitRepository
import it.atraj.habittracker.notification.NotificationReliabilityHelper
import it.atraj.habittracker.service.OverdueHabitIconManager
import it.atraj.habittracker.service.AppIconManager
import it.atraj.habittracker.widget.WidgetDataObserver
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
    
    @Inject
    lateinit var habitRepository: HabitRepository
    
    @Inject
    lateinit var widgetDataObserver: WidgetDataObserver
    
    private var activityCount = 0

    override fun onCreate() {
        super.onCreate()
        
        // Set up periodic alarm verification using WorkManager
        NotificationReliabilityHelper.setupAlarmVerification(this)
        
        // Initialize icon manager for overdue habit warnings only
        iconManager.initialize()
        
        // Schedule periodic checks for overdue habits
        OverdueHabitWorker.schedulePeriodicCheck(this)
        
        // Start observing habit data changes for automatic widget updates
        widgetDataObserver.startObserving()
        
        // Register lifecycle callbacks to cleanup icons when app goes to background
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                activityCount++
            }
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                activityCount--
                // When all activities are stopped, app is in background
                if (activityCount == 0) {
                    // Clean up old icons now that user isn't actively using the app
                    appIconManager.cleanupOldIconsInBackground()
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
