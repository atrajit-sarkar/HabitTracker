package it.atraj.habittracker.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import it.atraj.habittracker.data.HabitRepository
import it.atraj.habittracker.util.IconState
import it.atraj.habittracker.util.OverdueHabitChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverdueHabitIconManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitRepository: HabitRepository,
    private val appIconManager: AppIconManager
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val packageManager = context.packageManager
    private var currentIconState = IconState.DEFAULT
    
    companion object {
        private const val TAG = "OverdueHabitIconManager"
        private const val MAIN_ACTIVITY = "it.atraj.habittracker.MainActivity"
        private const val WARNING_ACTIVITY = "it.atraj.habittracker.MainActivity.Warning"
        private const val ANGRY_ACTIVITY = "it.atraj.habittracker.MainActivity.Angry"
        
        // Anime-themed overdue icons
        private const val WARNING_ANIME_ACTIVITY = "it.atraj.habittracker.MainActivity.WarningAnime"
        private const val ANGRY_ANIME_ACTIVITY = "it.atraj.habittracker.MainActivity.AngryAnime"
        
        // Sitama-themed overdue icons
        private const val WARNING_SITAMA_ACTIVITY = "it.atraj.habittracker.MainActivity.WarningSitama"
        private const val ANGRY_SITAMA_ACTIVITY = "it.atraj.habittracker.MainActivity.AngrySitama"

    // Bird-themed overdue icons (user provided)
    private const val WARNING_BIRD_ACTIVITY = "it.atraj.habittracker.MainActivity.WarningBird"
    private const val ANGRY_BIRD_ACTIVITY = "it.atraj.habittracker.MainActivity.AngryBird"
        
        // Atrajit-themed overdue icons (developer photo)
        private const val WARNING_ATRAJIT_ACTIVITY = "it.atraj.habittracker.MainActivity.WarningAtrajit"
        private const val ANGRY_ATRAJIT_ACTIVITY = "it.atraj.habittracker.MainActivity.AngryAtrajit"
        
        // Custom user icon aliases - NI/custom1/custom2 removed to reduce APK size
        private val CUSTOM_ICON_ALIASES = setOf(
            "it.atraj.habittracker.MainActivity.Anime",
            "it.atraj.habittracker.MainActivity.WarningAnime",
            "it.atraj.habittracker.MainActivity.AngryAnime",
            "it.atraj.habittracker.MainActivity.Sitama",
            "it.atraj.habittracker.MainActivity.WarningSitama",
            "it.atraj.habittracker.MainActivity.AngrySitama",
            "it.atraj.habittracker.MainActivity.Bird",
            "it.atraj.habittracker.MainActivity.WarningBird",
            "it.atraj.habittracker.MainActivity.AngryBird",
            "it.atraj.habittracker.MainActivity.Atrajit",
            "it.atraj.habittracker.MainActivity.WarningAtrajit",
            "it.atraj.habittracker.MainActivity.AngryAtrajit"
        )
    }
    
    /**
     * Check all habits and update the app icon if necessary (optimized)
     */
    fun checkAndUpdateIcon(forceRefresh: Boolean = false) {
        scope.launch {
            try {
                // Skip on first launch to avoid false positives
                if (appIconManager.isFirstLaunch()) {
                    Log.d(TAG, "First launch detected, skipping overdue check")
                    appIconManager.markFirstLaunchComplete()
                    return@launch
                }
                
                // Skip if user is actively changing icons
                if (appIconManager.isChangingIcon()) {
                    Log.d(TAG, "User is changing icon, skipping overdue check")
                    return@launch
                }
                
                // Add delay to avoid conflicts on app startup
                // Longer delay to ensure app has fully restarted and Firestore data has synced
                kotlinx.coroutines.delay(5000)
                
                Log.d(TAG, "Running overdue check now...")
                val habits = habitRepository.getAllHabits()
                Log.d(TAG, "Found ${habits.size} total habits")
                val currentTime = LocalDateTime.now()
                
                // Get completion data for all habits
                val allCompletions = habits.associate { habit ->
                    habit.id to habitRepository.getHabitCompletions(habit.id)
                }
                
                // Check for overdue habits (with caching for performance)
                val overdueHabits = OverdueHabitChecker.getOverdueHabits(
                    habits = habits,
                    allCompletions = allCompletions,
                    currentTime = currentTime,
                    forceRefresh = forceRefresh
                )
                
                // Determine required icon state
                val requiredIconState = OverdueHabitChecker.determineIconState(overdueHabits)
                
                Log.d(TAG, "Found ${overdueHabits.size} overdue habits. Required icon state: $requiredIconState")
                
                // Log details of each overdue habit
                overdueHabits.forEach { (habit, status) ->
                    Log.d(TAG, "Overdue habit: ${habit.title}, ${status.overdueHours} hours overdue, reminder: ${habit.reminderHour}:${habit.reminderMinute}")
                }
                
                // Update icon if state changed
                if (requiredIconState != currentIconState) {
                    updateAppIcon(requiredIconState)
                    currentIconState = requiredIconState
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking overdue habits", e)
            }
        }
    }
    
    /**
     * Update app icon safely without killing the app
     */
    private fun updateAppIconSafely(iconState: IconState) {
        try {
            when (iconState) {
                IconState.DEFAULT -> {
                    // Restore user's selected icon (enable first, then disable others)
                    val userIconAlias = appIconManager.getUserSelectedAlias()
                    val userIconId = appIconManager.getUserSelectedIconId()

                    // Enable user's preferred icon first to ensure there is always an active launcher
                    setComponentEnabledSafely(userIconAlias, true)
                    appIconManager.setCurrentIconIdTemporarily(userIconId)

                    // Disable all other overdue/default/custom aliases except the one we just enabled
                    if (MAIN_ACTIVITY != userIconAlias) setComponentEnabledSafely(MAIN_ACTIVITY, false)
                    listOf(
                        WARNING_ACTIVITY, ANGRY_ACTIVITY,
                        WARNING_ANIME_ACTIVITY, ANGRY_ANIME_ACTIVITY,
                        WARNING_SITAMA_ACTIVITY, ANGRY_SITAMA_ACTIVITY,
                        WARNING_BIRD_ACTIVITY, ANGRY_BIRD_ACTIVITY,
                        WARNING_ATRAJIT_ACTIVITY, ANGRY_ATRAJIT_ACTIVITY
                    ).forEach {
                        if (it != userIconAlias) setComponentEnabledSafely(it, false)
                    }
                    CUSTOM_ICON_ALIASES.forEach {
                        if (it != userIconAlias) setComponentEnabledSafely(it, false)
                    }

                    Log.d(TAG, "Restored user's icon: $userIconId ($userIconAlias)")
                }
                
                IconState.WARNING -> {
                    // Enable themed warning icon (respect user's selected custom icon) and disable others
                    val userIconId = appIconManager.getUserSelectedIconId()
                    val warningAlias = when (userIconId) {
                        "anime" -> WARNING_ANIME_ACTIVITY
                        "sitama" -> WARNING_SITAMA_ACTIVITY
                        "bird" -> WARNING_BIRD_ACTIVITY
                        "atrajit" -> WARNING_ATRAJIT_ACTIVITY
                        else -> WARNING_ACTIVITY
                    }

                    setComponentEnabledSafely(warningAlias, true)
                    appIconManager.setCurrentIconIdTemporarily("warning")

                    // Disable all other aliases except the one we just enabled
                    if (MAIN_ACTIVITY != warningAlias) setComponentEnabledSafely(MAIN_ACTIVITY, false)
                    listOf(
                        WARNING_ACTIVITY, WARNING_ANIME_ACTIVITY, WARNING_SITAMA_ACTIVITY, WARNING_BIRD_ACTIVITY, WARNING_ATRAJIT_ACTIVITY,
                        ANGRY_ACTIVITY, ANGRY_ANIME_ACTIVITY, ANGRY_SITAMA_ACTIVITY, ANGRY_BIRD_ACTIVITY, ANGRY_ATRAJIT_ACTIVITY
                    ).forEach {
                        if (it != warningAlias) setComponentEnabledSafely(it, false)
                    }
                    CUSTOM_ICON_ALIASES.forEach {
                        if (it != warningAlias) setComponentEnabledSafely(it, false)
                    }

                    Log.d(TAG, "Switched to warning app icon (themed: $warningAlias)")
                }
                
                IconState.CRITICAL_WARNING -> {
                    // Enable themed angry icon (respect user's selected custom icon) and disable others
                    val userIconId = appIconManager.getUserSelectedIconId()
                    val angryAlias = when (userIconId) {
                        "anime" -> ANGRY_ANIME_ACTIVITY
                        "sitama" -> ANGRY_SITAMA_ACTIVITY
                        "bird" -> ANGRY_BIRD_ACTIVITY
                        "atrajit" -> ANGRY_ATRAJIT_ACTIVITY
                        else -> ANGRY_ACTIVITY
                    }

                    setComponentEnabledSafely(angryAlias, true)
                    appIconManager.setCurrentIconIdTemporarily("angry")

                    // Disable all other aliases except the one we just enabled
                    if (MAIN_ACTIVITY != angryAlias) setComponentEnabledSafely(MAIN_ACTIVITY, false)
                    listOf(
                        WARNING_ACTIVITY, WARNING_ANIME_ACTIVITY, WARNING_SITAMA_ACTIVITY, WARNING_BIRD_ACTIVITY, WARNING_ATRAJIT_ACTIVITY,
                        ANGRY_ACTIVITY, ANGRY_ANIME_ACTIVITY, ANGRY_SITAMA_ACTIVITY, ANGRY_BIRD_ACTIVITY, ANGRY_ATRAJIT_ACTIVITY
                    ).forEach {
                        if (it != angryAlias) setComponentEnabledSafely(it, false)
                    }
                    CUSTOM_ICON_ALIASES.forEach {
                        if (it != angryAlias) setComponentEnabledSafely(it, false)
                    }

                    Log.d(TAG, "Switched to angry app icon (themed: $angryAlias)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update app icon safely", e)
        }
    }
    
    /**
     * Force update the app icon to specific state
     */
    fun updateAppIcon(iconState: IconState) {
        try {
            Log.d(TAG, "updateAppIcon called with state: $iconState")
            
            when (iconState) {
                IconState.DEFAULT -> {
                    // Restore user's selected icon
                    val userIconAlias = appIconManager.getUserSelectedAlias()
                    val userIconId = appIconManager.getUserSelectedIconId()
                    
                    Log.d(TAG, "Restoring to DEFAULT - User icon: $userIconId, Alias: $userIconAlias")
                    
                    // First: Disable ALL activity aliases (including main and all custom)
                    setComponentEnabled(MAIN_ACTIVITY, false)
                    CUSTOM_ICON_ALIASES.forEach { alias ->
                        setComponentEnabled(alias, false)
                    }
                    
                    // Second: Disable ALL overdue icons (default and themed)
                    setComponentEnabled(WARNING_ACTIVITY, false)
                    setComponentEnabled(ANGRY_ACTIVITY, false)
                    setComponentEnabled(WARNING_ANIME_ACTIVITY, false)
                    setComponentEnabled(ANGRY_ANIME_ACTIVITY, false)
                    setComponentEnabled(WARNING_SITAMA_ACTIVITY, false)
                    setComponentEnabled(ANGRY_SITAMA_ACTIVITY, false)
                    
                    // Third: Enable ONLY user's preferred icon
                    appIconManager.setComponentEnabled(userIconAlias, true)
                    appIconManager.setCurrentIconIdTemporarily(userIconId)
                    
                    Log.d(TAG, "✓ Restored user's icon: $userIconId ($userIconAlias)")
                }
                
                IconState.WARNING -> {
                    // Enable appropriate warning icon first (themed variants if user selected anime/sitama)
                    val userIconId = appIconManager.getUserSelectedIconId()
                    val warningAlias = when (userIconId) {
                        "anime" -> WARNING_ANIME_ACTIVITY
                        "sitama" -> WARNING_SITAMA_ACTIVITY
                        "bird" -> WARNING_BIRD_ACTIVITY
                        "atrajit" -> WARNING_ATRAJIT_ACTIVITY
                        else -> WARNING_ACTIVITY
                    }

                    Log.d(TAG, "Switching to WARNING - User icon: $userIconId, Warning alias: $warningAlias")

                    // Enable the target alias first
                    setComponentEnabled(warningAlias, true)
                    appIconManager.setCurrentIconIdTemporarily("warning")

                    // Disable all other aliases except the one we just enabled
                    if (MAIN_ACTIVITY != warningAlias) setComponentEnabled(MAIN_ACTIVITY, false)
                    listOf(
                        WARNING_ACTIVITY, WARNING_ANIME_ACTIVITY, WARNING_SITAMA_ACTIVITY, WARNING_BIRD_ACTIVITY, WARNING_ATRAJIT_ACTIVITY,
                        ANGRY_ACTIVITY, ANGRY_ANIME_ACTIVITY, ANGRY_SITAMA_ACTIVITY, ANGRY_BIRD_ACTIVITY, ANGRY_ATRAJIT_ACTIVITY
                    ).forEach {
                        if (it != warningAlias) setComponentEnabled(it, false)
                    }
                    CUSTOM_ICON_ALIASES.forEach {
                        if (it != warningAlias) setComponentEnabled(it, false)
                    }

                    Log.d(TAG, "\u2713 Switched to warning app icon (themed: $warningAlias)")
                }
                
                IconState.CRITICAL_WARNING -> {
                    // Enable appropriate angry icon first (themed variants if user selected anime/sitama)
                    val userIconId = appIconManager.getUserSelectedIconId()
                    val angryAlias = when (userIconId) {
                        "anime" -> ANGRY_ANIME_ACTIVITY
                        "sitama" -> ANGRY_SITAMA_ACTIVITY
                        "bird" -> ANGRY_BIRD_ACTIVITY
                        "atrajit" -> ANGRY_ATRAJIT_ACTIVITY
                        else -> ANGRY_ACTIVITY
                    }

                    Log.d(TAG, "Switching to CRITICAL_WARNING - User icon: $userIconId, Angry alias: $angryAlias")

                    // Enable the target alias first
                    setComponentEnabled(angryAlias, true)
                    appIconManager.setCurrentIconIdTemporarily("angry")

                    // Disable all other aliases except the one we just enabled
                    if (MAIN_ACTIVITY != angryAlias) setComponentEnabled(MAIN_ACTIVITY, false)
                    listOf(
                        WARNING_ACTIVITY, WARNING_ANIME_ACTIVITY, WARNING_SITAMA_ACTIVITY, WARNING_BIRD_ACTIVITY, WARNING_ATRAJIT_ACTIVITY,
                        ANGRY_ACTIVITY, ANGRY_ANIME_ACTIVITY, ANGRY_SITAMA_ACTIVITY, ANGRY_BIRD_ACTIVITY, ANGRY_ATRAJIT_ACTIVITY
                    ).forEach {
                        if (it != angryAlias) setComponentEnabled(it, false)
                    }
                    CUSTOM_ICON_ALIASES.forEach {
                        if (it != angryAlias) setComponentEnabled(it, false)
                    }

                    Log.d(TAG, "\u2713 Switched to angry app icon (themed: $angryAlias)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update app icon", e)
        }
    }
    
    /**
     * Reset to default icon state
     */
    fun resetToDefaultIcon() {
        updateAppIcon(IconState.DEFAULT)
        currentIconState = IconState.DEFAULT
    }
    
    /**
     * Get current icon state
     */
    fun getCurrentIconState(): IconState = currentIconState
    
    private fun setComponentEnabled(componentName: String, enabled: Boolean) {
        try {
            val component = ComponentName(context, componentName)
            val newState = if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            
            packageManager.setComponentEnabledSetting(
                component,
                newState,
                PackageManager.DONT_KILL_APP
            )
            
            Log.d(TAG, "Set component $componentName enabled=$enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting component state for $componentName", e)
        }
    }
    
    private fun setComponentEnabledSafely(componentName: String, enabled: Boolean) {
        val component = ComponentName(context, componentName)
        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        
        // Use synchronous call without DONT_KILL_APP flag when safe
        try {
            packageManager.setComponentEnabledSetting(
                component,
                newState,
                0 // No flags - let the system handle it properly
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set component state safely, falling back to DONT_KILL_APP")
            packageManager.setComponentEnabledSetting(
                component,
                newState,
                PackageManager.DONT_KILL_APP
            )
        }
    }
    
    /**
     * Initialize icon manager - call from Application onCreate
     */
    fun initialize() {
        // Just initialize state tracking - don't change components at startup
        try {
            // Initialize currentIconState from persisted AppIconManager to avoid
            // process-restart desynchronization between in-memory state and actual enabled alias
            val persistedIconId = appIconManager.getCurrentIconId()
            currentIconState = when (persistedIconId) {
                "angry" -> IconState.CRITICAL_WARNING
                "warning" -> IconState.WARNING
                else -> IconState.DEFAULT
            }
            Log.d(TAG, "OverdueHabitIconManager initialized - persisted icon id: $persistedIconId, currentIconState: $currentIconState")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize icon manager", e)
        }
    }
    
    /**
     * Clear the changing icon flag (call after app has fully started)
     */
    fun clearChangingIconFlag() {
        appIconManager.clearChangingIconFlag()
    }
    
    /**
     * Handle habit completion - clear cache and force refresh
     */
    fun onHabitCompleted() {
        Log.d(TAG, "Habit completed, clearing cache and updating icon...")
        OverdueHabitChecker.clearCache()
        // Use immediate check without delay for habit completion
        scope.launch {
            try {
                // Small delay to let data sync
                kotlinx.coroutines.delay(500)
                
                val habits = habitRepository.getAllHabits()
                val currentTime = LocalDateTime.now()
                
                Log.d(TAG, "Checking ${habits.size} habits after completion")
                
                // Get completion data for all habits
                val allCompletions = habits.associate { habit ->
                    habit.id to habitRepository.getHabitCompletions(habit.id)
                }
                
                // Check for overdue habits (force refresh)
                val overdueHabits = OverdueHabitChecker.getOverdueHabits(
                    habits = habits,
                    allCompletions = allCompletions,
                    currentTime = currentTime,
                    forceRefresh = true
                )
                
                // Determine required icon state
                val requiredIconState = OverdueHabitChecker.determineIconState(overdueHabits)
                
                Log.d(TAG, "After completion: ${overdueHabits.size} overdue habits. Required icon state: $requiredIconState")
                
                // Update icon if state changed
                if (requiredIconState != currentIconState) {
                    updateAppIcon(requiredIconState)
                    currentIconState = requiredIconState
                    Log.d(TAG, "Icon updated to: $requiredIconState")
                } else {
                    Log.d(TAG, "Icon state unchanged: $currentIconState")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking overdue habits after completion", e)
            }
        }
    }
    
    /**
     * Force check and update icon immediately (for testing)
     * Only updates if app is not in foreground to avoid killing the app
     */
    fun forceCheckAndUpdate() {
        Log.d(TAG, "Force checking icon status...")
        checkAndUpdateIconSafely()
    }
    
    /**
     * Check and update icon only if app is not actively being used
     */
    private fun checkAndUpdateIconSafely() {
        scope.launch {
            try {
                val habits = habitRepository.getAllHabits()
                val currentTime = LocalDateTime.now()
                
                // Get completion data for all habits
                val allCompletions = habits.associate { habit ->
                    habit.id to habitRepository.getHabitCompletions(habit.id)
                }
                
                // Check for overdue habits
                val overdueHabits = OverdueHabitChecker.getOverdueHabits(
                    habits = habits,
                    allCompletions = allCompletions,
                    currentTime = currentTime
                )
                
                // Determine required icon state
                val requiredIconState = OverdueHabitChecker.determineIconState(overdueHabits)
                
                Log.d(TAG, "Found ${overdueHabits.size} overdue habits. Required icon state: $requiredIconState")
                
                // Update icon if state changed - delay to avoid app restart during usage
                if (requiredIconState != currentIconState) {
                    // Wait a bit to ensure app is not actively being used
                    kotlinx.coroutines.delay(2000)
                    updateAppIconSafely(requiredIconState)
                    currentIconState = requiredIconState
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking overdue habits safely", e)
            }
        }
    }
    
    /**
     * Check if we have any overdue habits (for debugging/testing)
     */
    suspend fun getOverdueHabitsStatus(): List<String> {
        return try {
            val habits = habitRepository.getAllHabits()
            val allCompletions = habits.associate { habit ->
                habit.id to habitRepository.getHabitCompletions(habit.id)
            }
            
            val overdueHabits = OverdueHabitChecker.getOverdueHabits(habits, allCompletions)
            
            overdueHabits.map { (habit, status) ->
                "${habit.title}: ${status.overdueHours} hours overdue"
            }
        } catch (e: Exception) {
            listOf("Error checking habits: ${e.message}")
        }
    }
}