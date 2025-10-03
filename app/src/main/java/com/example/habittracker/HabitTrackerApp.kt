package com.example.habittracker

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.habittracker.notification.NotificationReliabilityHelper
import com.example.habittracker.util.LanguageManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HabitTrackerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var languageManager: LanguageManager

    override fun onCreate() {
        super.onCreate()
        
        // Apply saved language
        val savedLanguage = languageManager.getCurrentLanguage()
        languageManager.applyLanguage(this, savedLanguage)
        
        // Set up periodic alarm verification using WorkManager
        NotificationReliabilityHelper.setupAlarmVerification(this)
    }
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
