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
    private val habitRepository: HabitRepository
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val packageManager = context.packageManager
    private var currentIconState = IconState.DEFAULT
    
    companion object {
        private const val TAG = "OverdueHabitIconManager"
        private const val MAIN_ACTIVITY = "it.atraj.habittracker.MainActivity"
        private const val WARNING_ACTIVITY = "it.atraj.habittracker.MainActivity.Warning"
        private const val ANGRY_ACTIVITY = "it.atraj.habittracker.MainActivity.Angry"
    }
    
    /**
     * Check all habits and update the app icon if necessary (optimized)
     */
    fun checkAndUpdateIcon(forceRefresh: Boolean = false) {
        scope.launch {
            try {
                val habits = habitRepository.getAllHabits()
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
                    // Enable default icon, disable warning icons
                    setComponentEnabledSafely(MAIN_ACTIVITY, true)
                    setComponentEnabledSafely(WARNING_ACTIVITY, false)
                    setComponentEnabledSafely(ANGRY_ACTIVITY, false)
                    Log.d(TAG, "Switched to default app icon")
                }
                
                IconState.WARNING -> {
                    // Enable warning icon, disable others
                    setComponentEnabledSafely(MAIN_ACTIVITY, false)
                    setComponentEnabledSafely(WARNING_ACTIVITY, true)
                    setComponentEnabledSafely(ANGRY_ACTIVITY, false)
                    Log.d(TAG, "Switched to warning app icon")
                }
                
                IconState.CRITICAL_WARNING -> {
                    // Enable angry icon, disable others
                    setComponentEnabledSafely(MAIN_ACTIVITY, false)
                    setComponentEnabledSafely(WARNING_ACTIVITY, false)
                    setComponentEnabledSafely(ANGRY_ACTIVITY, true)
                    Log.d(TAG, "Switched to angry app icon")
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
            when (iconState) {
                IconState.DEFAULT -> {
                    // Enable default icon, disable warning icons
                    setComponentEnabled(MAIN_ACTIVITY, true)
                    setComponentEnabled(WARNING_ACTIVITY, false)
                    setComponentEnabled(ANGRY_ACTIVITY, false)
                    Log.d(TAG, "Switched to default app icon")
                }
                
                IconState.WARNING -> {
                    // Enable warning icon, disable others
                    setComponentEnabled(MAIN_ACTIVITY, false)
                    setComponentEnabled(WARNING_ACTIVITY, true)
                    setComponentEnabled(ANGRY_ACTIVITY, false)
                    Log.d(TAG, "Switched to warning app icon")
                }
                
                IconState.CRITICAL_WARNING -> {
                    // Enable angry icon, disable others
                    setComponentEnabled(MAIN_ACTIVITY, false)
                    setComponentEnabled(WARNING_ACTIVITY, false)
                    setComponentEnabled(ANGRY_ACTIVITY, true)
                    Log.d(TAG, "Switched to angry app icon")
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
        // Ensure default icon is enabled on first run
        try {
            setComponentEnabled(MAIN_ACTIVITY, true)
            setComponentEnabled(WARNING_ACTIVITY, false)
            setComponentEnabled(ANGRY_ACTIVITY, false)
            currentIconState = IconState.DEFAULT
            Log.d(TAG, "OverdueHabitIconManager initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize icon manager", e)
        }
    }
    
    /**
     * Handle habit completion - clear cache and force refresh
     */
    fun onHabitCompleted() {
        Log.d(TAG, "Habit completed, clearing cache and updating icon...")
        OverdueHabitChecker.clearCache()
        checkAndUpdateIcon(forceRefresh = true)
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