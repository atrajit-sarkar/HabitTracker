package com.example.habittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.example.habittracker.notification.HabitReminderService
import com.example.habittracker.ui.HabitHomeRoute
import com.example.habittracker.ui.theme.HabitTrackerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        HabitReminderService.ensureChannel(this)
        setContent {
            HabitTrackerTheme {
                HabitHomeRoute()
            }
        }
    }
}