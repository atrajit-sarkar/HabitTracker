package com.example.habittracker

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import com.example.habittracker.auth.GoogleSignInHelper
import com.example.habittracker.data.HabitRepository
import com.example.habittracker.data.firestore.UserPresenceManager
import com.example.habittracker.notification.HabitReminderService
import com.example.habittracker.notification.NotificationReliabilityHelper
import com.example.habittracker.ui.HabitTrackerNavigation
import com.example.habittracker.ui.theme.HabitTrackerTheme
import com.example.habittracker.update.CheckingUpdatesDialog
import com.example.habittracker.update.UpdateDialog
import com.example.habittracker.update.UpdateInfo
import com.example.habittracker.update.UpdateManager
import com.example.habittracker.update.UpdateResultDialog
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
    private lateinit var updateManager: UpdateManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        HabitReminderService.ensureDefaultChannel(this)
        
        // Initialize update manager
        updateManager = UpdateManager(this)
        
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
                // State for update dialog
                var showUpdateDialog by remember { mutableStateOf(false) }
                var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
                var isDownloading by remember { mutableStateOf(false) }
                var downloadProgress by remember { mutableIntStateOf(0) }
                var isCheckingForUpdates by remember { mutableStateOf(false) }
                var showUpToDateDialog by remember { mutableStateOf(false) }
                var showUpdateCheckError by remember { mutableStateOf(false) }
                var updateCheckErrorMessage by remember { mutableStateOf("") }
                
                // Manual check for updates function
                val checkForUpdatesManually: () -> Unit = {
                    if (!isCheckingForUpdates) {
                        isCheckingForUpdates = true
                        MainScope().launch {
                            try {
                                val update = updateManager.checkForUpdate()
                                if (update != null) {
                                    if (update.isUpdateAvailable) {
                                        // Clear skipped version on manual check
                                        updateManager.clearSkippedVersion()
                                        updateInfo = update
                                        showUpdateDialog = true
                                    } else {
                                        // Show "Already up to date" message
                                        showUpToDateDialog = true
                                        Log.d("MainActivity", "App is up to date: ${update.currentVersion}")
                                    }
                                } else {
                                    // Show error dialog
                                    showUpdateCheckError = true
                                    updateCheckErrorMessage = "Unable to check for updates"
                                    Log.e("MainActivity", "Failed to check for updates")
                                }
                            } catch (e: Exception) {
                                showUpdateCheckError = true
                                updateCheckErrorMessage = e.message ?: "Unknown error occurred"
                                Log.e("MainActivity", "Error checking for updates: ${e.message}")
                            } finally {
                                isCheckingForUpdates = false
                            }
                        }
                    }
                }
                
                // Check for updates on app start
                LaunchedEffect(Unit) {
                    if (updateManager.shouldCheckForUpdate()) {
                        val update = updateManager.checkForUpdate()
                        if (update != null && update.isUpdateAvailable) {
                            // Don't show if user skipped this version
                            if (!updateManager.isVersionSkipped(update.latestVersion)) {
                                updateInfo = update
                                showUpdateDialog = true
                            }
                        }
                    }
                }
                
                // Navigation
                HabitTrackerNavigation(
                    startDestination = startDestination,
                    googleSignInHelper = googleSignInHelper,
                    onCheckForUpdates = checkForUpdatesManually
                )
                
                // Update dialog
                if (showUpdateDialog && updateInfo != null) {
                    UpdateDialog(
                        updateInfo = updateInfo!!,
                        onDismiss = { 
                            showUpdateDialog = false 
                        },
                        onUpdate = {
                            isDownloading = true
                            MainScope().launch {
                                val result = updateManager.downloadAndInstall(
                                    downloadUrl = updateInfo!!.downloadUrl,
                                    onProgress = { progress ->
                                        downloadProgress = progress
                                    }
                                )
                                
                                result.onSuccess { apkFile ->
                                    isDownloading = false
                                    showUpdateDialog = false
                                    updateManager.installApk(apkFile)
                                }.onFailure { error ->
                                    isDownloading = false
                                    Log.e("MainActivity", "Update failed: ${error.message}")
                                    // Fallback to browser
                                    updateManager.openReleasesPage()
                                    showUpdateDialog = false
                                }
                            }
                        },
                        onSkip = {
                            updateManager.skipVersion(updateInfo!!.latestVersion)
                            showUpdateDialog = false
                        },
                        isDownloading = isDownloading,
                        downloadProgress = downloadProgress,
                        isMandatory = false // Set to true for critical updates
                    )
                }
                
                // Checking for updates dialog
                if (isCheckingForUpdates) {
                    CheckingUpdatesDialog()
                }
                
                // Up to date dialog
                if (showUpToDateDialog) {
                    UpdateResultDialog(
                        isUpToDate = true,
                        currentVersion = updateManager.getCurrentVersion(),
                        onDismiss = { showUpToDateDialog = false }
                    )
                }
                
                // Update check error dialog
                if (showUpdateCheckError) {
                    UpdateResultDialog(
                        isUpToDate = false,
                        currentVersion = updateManager.getCurrentVersion(),
                        onDismiss = { showUpdateCheckError = false },
                        error = updateCheckErrorMessage
                    )
                }
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

                    // Prompt for exact alarm permission separately (Android 12+) if missing
                    if (hasReminders && !NotificationReliabilityHelper.hasExactAlarmPermission(this@MainActivity)) {
                        // Slight additional delay so dialogs don't stack abruptly
                        kotlinx.coroutines.delay(400)
                        NotificationReliabilityHelper.requestExactAlarmPermission(this@MainActivity)
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