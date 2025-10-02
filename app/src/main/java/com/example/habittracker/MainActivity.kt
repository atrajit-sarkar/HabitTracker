package com.example.habittracker

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import com.example.habittracker.auth.GoogleSignInHelper
import com.example.habittracker.data.HabitRepository
import com.example.habittracker.data.firestore.UserPresenceManager
import com.example.habittracker.notification.HabitReminderService
import com.example.habittracker.notification.NotificationReliabilityHelper
import com.example.habittracker.ui.HabitTrackerNavigation
import com.example.habittracker.ui.theme.HabitTrackerTheme
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var googleSignInHelper: GoogleSignInHelper
    
    @Inject
    lateinit var habitRepository: HabitRepository
    
    private var hasCheckedBatteryOptimization = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        HabitReminderService.ensureDefaultChannel(this)
        
        // Get and log FCM token for testing
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM_TOKEN", "═══════════════════════════════════════")
                Log.d("FCM_TOKEN", "Your FCM Token (copy this for testing):")
                Log.d("FCM_TOKEN", token)
                Log.d("FCM_TOKEN", "═══════════════════════════════════════")
                
                // Save token to Firestore
                MainScope().launch {
                    UserPresenceManager.saveFcmToken(token)
                }
            }
        }
        
        // Check if opened from notification
        val openChat = intent.getBooleanExtra("openChat", false)
        val friendId = intent.getStringExtra("extra_friend_id")
        val friendName = intent.getStringExtra("extra_friend_name")
        val friendAvatar = intent.getStringExtra("extra_friend_avatar")
        val friendPhotoUrl = intent.getStringExtra("extra_friend_photo_url")
        
        // Check if opened from habit notification
        val openHabitDetails = intent.getBooleanExtra("openHabitDetails", false)
        val habitId = intent.getLongExtra("habitId", -1L)
        
        val startDestination = when {
            openChat && friendId != null -> {
                val photoUrlEncoded = friendPhotoUrl?.let { 
                    java.net.URLEncoder.encode(it, "UTF-8") 
                } ?: "null"
                "chat/$friendId/${friendName ?: "Friend"}/${friendAvatar ?: "😊"}/$photoUrlEncoded"
            }
            openHabitDetails && habitId != -1L -> "habit_details/$habitId"
            else -> "loading"
        }
        
        setContent {
            HabitTrackerTheme {
                HabitTrackerNavigation(
                    startDestination = startDestination,
                    googleSignInHelper = googleSignInHelper
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Set user online when app comes to foreground
        MainScope().launch {
            UserPresenceManager.setOnlineStatus(true)
        }
        
        // Check battery optimization only once per app session
        // and only if user has reminders enabled
        if (!hasCheckedBatteryOptimization) {
            hasCheckedBatteryOptimization = true
            MainScope().launch {
                try {
                    val habits = habitRepository.getAllHabits()
                    val hasReminders = habits.any { !it.isDeleted && it.reminderEnabled }
                    
                    if (hasReminders && !NotificationReliabilityHelper.isIgnoringBatteryOptimizations(this@MainActivity)) {
                        // Delay to avoid showing dialog immediately on app start
                        kotlinx.coroutines.delay(1500)
                        NotificationReliabilityHelper.requestBatteryOptimizationExemption(this@MainActivity)
                        
                        // Show manufacturer-specific instructions if needed
                        if (NotificationReliabilityHelper.isAggressiveBatteryManagement()) {
                            kotlinx.coroutines.delay(1000)
                            NotificationReliabilityHelper.showManufacturerInstructions(this@MainActivity)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error checking battery optimization: ${e.message}")
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Set user offline when app goes to background
        MainScope().launch {
            UserPresenceManager.setOnlineStatus(false)
        }
    }
}