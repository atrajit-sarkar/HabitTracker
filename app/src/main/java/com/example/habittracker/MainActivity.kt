package it.atraj.habittracker

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import it.atraj.habittracker.auth.GoogleSignInHelper
import it.atraj.habittracker.data.HabitRepository
import it.atraj.habittracker.data.firestore.UserPresenceManager
import it.atraj.habittracker.notification.HabitReminderService
import it.atraj.habittracker.notification.NotificationReliabilityHelper
import it.atraj.habittracker.ui.HabitTrackerNavigation
import it.atraj.habittracker.ui.theme.HabitTrackerTheme
import it.atraj.habittracker.update.CheckingUpdatesDialog
import it.atraj.habittracker.update.UpdateDialog
import it.atraj.habittracker.update.UpdateInfo
import it.atraj.habittracker.update.UpdateManager
import it.atraj.habittracker.update.UpdateResultDialog
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var googleSignInHelper: GoogleSignInHelper
    
    @Inject
    lateinit var habitRepository: HabitRepository
    
    @Inject
    lateinit var avatarConfig: it.atraj.habittracker.avatar.AvatarConfig
    
    private var hasCheckedBatteryOptimization = false
    private lateinit var updateManager: UpdateManager
    
    override fun attachBaseContext(newBase: Context) {
        // Apply saved language before attaching context
        val prefs = newBase.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("selected_language", "en") ?: "en"
        
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val configuration = Configuration(newBase.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            configuration.setLocales(android.os.LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }
        
        val context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            newBase.createConfigurationContext(configuration)
        } else {
            newBase.resources.updateConfiguration(configuration, newBase.resources.displayMetrics)
            newBase
        }
        
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Apply saved language preference
        it.atraj.habittracker.util.LanguageManager.applyLanguage(this)
        
        enableEdgeToEdge()
        HabitReminderService.ensureDefaultChannel(this)
        
        // Initialize update manager
        updateManager = UpdateManager(this)
        
        // Initialize avatar upload feature with secure token storage
        initializeAvatarFeature()
        
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
        var habitId = intent.getLongExtra("habitId", -1L)
        
        // Handle deep link from email
        // Supports both: habittracker://habit/{habitId} and https://atraj.it/habittracker/habit/{habitId}
        intent.data?.let { deepLinkUri ->
            Log.d("MainActivity", "Deep link received: $deepLinkUri")
            
            // Handle custom scheme: habittracker://habit/{habitId}
            if (deepLinkUri.scheme == "habittracker" && deepLinkUri.host == "habit") {
                deepLinkUri.pathSegments.firstOrNull()?.toLongOrNull()?.let { id ->
                    habitId = id
                    Log.d("MainActivity", "Custom scheme deep link opened for habit ID: $habitId")
                }
            }
            // Handle HTTPS scheme: https://habittracker.atraj.it/habit/{habitId}
            else if (deepLinkUri.scheme == "https" && deepLinkUri.host == "habittracker.atraj.it") {
                val pathSegments = deepLinkUri.pathSegments
                // Path format: /habit/{habitId}
                if (pathSegments.size >= 2 && pathSegments[0] == "habit") {
                    pathSegments[1].toLongOrNull()?.let { id ->
                        habitId = id
                        Log.d("MainActivity", "HTTPS deep link opened for habit ID: $habitId")
                    }
                }
            }
        }
        
        val startDestination = when {
            openChat && friendId != null -> {
                val photoUrlEncoded = friendPhotoUrl?.let { 
                    java.net.URLEncoder.encode(it, "UTF-8") 
                } ?: "null"
                "chat/$friendId/${friendName ?: "Friend"}/${friendAvatar ?: "😊"}/$photoUrlEncoded"
            }
            habitId != -1L -> "habit_details/$habitId"  // Works for both notification and deep link
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
    
    /**
     * Initialize avatar upload feature with secure token storage
     * 
     * Note: Token is stored in encrypted SharedPreferences on device.
     * To add/update token: Use SecureTokenStorage.storeToken() manually or via settings.
     */
    private fun initializeAvatarFeature() {
        try {
            // Retrieve token from secure encrypted storage
            var token = it.atraj.habittracker.avatar.SecureTokenStorage.getToken(this)
            
            // If no token is stored, try to use BuildConfig token (for first run)
            if (token == null && BuildConfig.GITHUB_TOKEN.isNotEmpty()) {
                token = BuildConfig.GITHUB_TOKEN
                // Store it securely for future use
                it.atraj.habittracker.avatar.SecureTokenStorage.storeToken(this, token)
                Log.d("MainActivity", "✅ GitHub token stored from BuildConfig")
            }
            
            if (token != null) {
                // Initialize avatar feature with stored token
                avatarConfig.initialize(token)
                Log.d("MainActivity", "✅ Avatar upload feature initialized")
            } else {
                // No token stored - upload feature will be disabled
                Log.w("MainActivity", "⚠️ GitHub token not found - avatar upload disabled")
                Log.i("MainActivity", "To enable: Store token using SecureTokenStorage.storeToken()")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Failed to initialize avatar feature: ${e.message}", e)
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
