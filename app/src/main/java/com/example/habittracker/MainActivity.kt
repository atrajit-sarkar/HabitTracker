package com.example.habittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import com.example.habittracker.notification.HabitReminderService
import com.example.habittracker.ui.HabitTrackerNavigation
import com.example.habittracker.ui.theme.HabitTrackerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        HabitReminderService.ensureChannel(this)
        
        // Check if opened from notification
        val startDestination = if (intent.getBooleanExtra("openHabitDetails", false)) {
            val habitId = intent.getLongExtra("habitId", -1L)
            if (habitId != -1L) "habit_details/$habitId" else "home"
        } else "home"
        
        setContent {
            HabitTrackerTheme {
                HabitTrackerNavigation(startDestination = startDestination)
            }
        }
    }
}