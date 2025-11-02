package it.atraj.habittracker.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppIconManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager = context.packageManager
    private val prefs = context.getSharedPreferences("app_icon_prefs", Context.MODE_PRIVATE)
    private val iconChangeScope = CoroutineScope(Dispatchers.Default)
    private var pendingIconChangeJob: Job? = null
    
    companion object {
        private const val TAG = "AppIconManager"
        private const val PREF_CURRENT_ICON_ID = "current_icon_id"
        private const val PREF_USER_SELECTED_ICON_ID = "user_selected_icon_id"
        private const val PREF_USER_SELECTED_ALIAS = "user_selected_alias"
        private const val PREF_IS_CHANGING_ICON = "is_changing_icon"
        private const val PREF_ICON_CHANGE_TIMESTAMP = "icon_change_timestamp"
        private const val PREF_FIRST_LAUNCH = "is_first_launch"
        private const val PREF_NEEDS_CLEANUP = "needs_icon_cleanup"
        
        // Available icon aliases (only user-selected custom icons)
        private val ICON_ALIASES = mapOf(
            "default" to "it.atraj.habittracker.MainActivity.Default",
            "anime" to "it.atraj.habittracker.MainActivity.Anime",
            "sitama" to "it.atraj.habittracker.MainActivity.Sitama",
            "bird" to "it.atraj.habittracker.MainActivity.Bird",
            // Personal themed icon (developer photo)
            "atrajit" to "it.atraj.habittracker.MainActivity.Atrajit"
        )
        
        // Warning/Angry icons managed by OverdueHabitIconManager - DO NOT MODIFY
        private val OVERDUE_ICON_ALIASES = setOf(
            "it.atraj.habittracker.MainActivity.Warning",
            "it.atraj.habittracker.MainActivity.Angry",
            "it.atraj.habittracker.MainActivity.WarningDefault",
            "it.atraj.habittracker.MainActivity.AngryDefault",
            "it.atraj.habittracker.MainActivity.WarningAnime",
            "it.atraj.habittracker.MainActivity.AngryAnime",
            "it.atraj.habittracker.MainActivity.WarningSitama",
            "it.atraj.habittracker.MainActivity.AngrySitama",
            "it.atraj.habittracker.MainActivity.WarningBird",
            "it.atraj.habittracker.MainActivity.AngryBird",
            "it.atraj.habittracker.MainActivity.WarningAtrajit",
            "it.atraj.habittracker.MainActivity.AngryAtrajit",
            "it.atraj.habittracker.MainActivity.Atrajit",
            "it.atraj.habittracker.MainActivity.Default"
        )
    }
    
    fun getCurrentIconId(): String {
        return prefs.getString(PREF_CURRENT_ICON_ID, "default") ?: "default"
    }
    
    fun getUserSelectedIconId(): String {
        return prefs.getString(PREF_USER_SELECTED_ICON_ID, "default") ?: "default"
    }
    
    fun getUserSelectedAlias(): String {
        return prefs.getString(PREF_USER_SELECTED_ALIAS, ICON_ALIASES["default"]!!) ?: ICON_ALIASES["default"]!!
    }
    
    fun isChangingIcon(): Boolean {
        // Check if icon was changed in the last 30 seconds
        // Longer window to account for app restart after icon change
        val changeTimestamp = prefs.getLong(PREF_ICON_CHANGE_TIMESTAMP, 0L)
        val currentTime = System.currentTimeMillis()
        return (currentTime - changeTimestamp) < 30000 // 30 seconds
    }
    
    fun clearChangingIconFlag() {
        // Clear by setting timestamp to 0
        prefs.edit().putLong(PREF_ICON_CHANGE_TIMESTAMP, 0L).apply()
    }
    
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(PREF_FIRST_LAUNCH, true)
    }
    
    fun markFirstLaunchComplete() {
        prefs.edit().putBoolean(PREF_FIRST_LAUNCH, false).apply()
    }
    
    suspend fun changeAppIcon(iconId: String, activityAlias: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Changing app icon to: $iconId (alias: $activityAlias)")
            
            // IMPORTANT: Save preferences BEFORE changing components
            // This ensures that when the app restarts, it knows the user's choice
            prefs.edit()
                .putString(PREF_CURRENT_ICON_ID, iconId)
                .putString(PREF_USER_SELECTED_ICON_ID, iconId)
                .putString(PREF_USER_SELECTED_ALIAS, activityAlias)
                .putLong(PREF_ICON_CHANGE_TIMESTAMP, System.currentTimeMillis())  // Set timestamp to prevent overdue checks
                .apply()
            
            Log.d(TAG, "Saved icon preference: $iconId")
            
            // CRITICAL: Enable the new icon FIRST before disabling others
            // This ensures there's always at least one enabled launcher activity
            setComponentEnabled(activityAlias, true)
            Log.d(TAG, "Enabled new icon: $activityAlias")
            
            // Immediately disable ALL other aliases (no delay needed with DONT_KILL_APP)
            val allAliases = ICON_ALIASES.values + OVERDUE_ICON_ALIASES
            allAliases.forEach { alias ->
                if (alias != activityAlias) {
                    setComponentEnabled(alias, false)
                    Log.d(TAG, "Disabled alias: $alias")
                }
            }
            
            Log.d(TAG, "Successfully changed app icon to: $iconId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to change app icon", e)
            false
        }
    }
    
    /**
     * Schedule icon change with smooth transition.
     * Shows the change immediately in the UI but applies component changes
     * after a delay to make the transition less jarring.
     */
    fun scheduleIconChange(iconId: String, activityAlias: String) {
        // Cancel any pending icon change
        pendingIconChangeJob?.cancel()
        
        // Save preferences immediately so UI updates
        prefs.edit()
            .putString(PREF_CURRENT_ICON_ID, iconId)
            .putString(PREF_USER_SELECTED_ICON_ID, iconId)
            .putString(PREF_USER_SELECTED_ALIAS, activityAlias)
            .putLong(PREF_ICON_CHANGE_TIMESTAMP, System.currentTimeMillis())
            .putBoolean(PREF_NEEDS_CLEANUP, true) // Mark that we need to cleanup old icons
            .apply()
        
        Log.d(TAG, "Scheduled icon change to: $iconId (will cleanup when app goes to background)")
        
        // Enable the new icon immediately (this doesn't kill the app)
        try {
            setComponentEnabled(activityAlias, true)
            Log.d(TAG, "Enabled new icon: $activityAlias")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable new icon", e)
        }
        
        // DON'T disable old icons here - that would kill the app!
        // They will be disabled when app goes to background via cleanupOldIconsInBackground()
    }
    
    /**
     * Apply icon change immediately (fallback method for backward compatibility).
     */
    suspend fun changeAppIconImmediate(iconId: String, activityAlias: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Immediate icon change to: $iconId (alias: $activityAlias)")
            
            prefs.edit()
                .putString(PREF_CURRENT_ICON_ID, iconId)
                .putString(PREF_USER_SELECTED_ICON_ID, iconId)
                .putString(PREF_USER_SELECTED_ALIAS, activityAlias)
                .putLong(PREF_ICON_CHANGE_TIMESTAMP, System.currentTimeMillis())
                .apply()
            
            setComponentEnabled(activityAlias, true)
            
            val allAliases = ICON_ALIASES.values + OVERDUE_ICON_ALIASES
            allAliases.forEach { alias ->
                if (alias != activityAlias) {
                    setComponentEnabled(alias, false)
                }
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed immediate icon change", e)
            false
        }
    }
    
    internal fun setComponentEnabled(componentName: String, enabled: Boolean) {
        val component = ComponentName(context, componentName)
        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        
        // Use DONT_KILL_APP to update icons without killing the app
        // This ensures smooth icon switching
        packageManager.setComponentEnabledSetting(
            component,
            newState,
            PackageManager.DONT_KILL_APP
        )
        
        Log.d(TAG, "Set component $componentName enabled=$enabled")
    }
    
    /**
     * Set current icon temporarily (used by overdue manager)
     */
    internal fun setCurrentIconIdTemporarily(iconId: String) {
        prefs.edit().putString(PREF_CURRENT_ICON_ID, iconId).apply()
    }
    
    /**
     * Initialize default icon state
     */
    fun initialize() {
        try {
            val currentIconId = getCurrentIconId()
            Log.d(TAG, "Initializing AppIconManager with icon: $currentIconId")
            
            // DON'T clean up on app start - this would kill the app!
            // Cleanup will happen when app goes to background
            
            Log.d(TAG, "Current icon ID: $currentIconId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AppIconManager", e)
        }
    }
    
    /**
     * Cleanup old icons when app goes to background.
     * This is called by Application lifecycle callbacks when all activities are stopped.
     * Since the app is in background, disabling components won't disrupt the user.
     */
    fun cleanupOldIconsInBackground() {
        // Check if cleanup is needed
        val needsCleanup = prefs.getBoolean(PREF_NEEDS_CLEANUP, false)
        if (!needsCleanup) {
            Log.d(TAG, "No icon cleanup needed")
            return
        }
        
        iconChangeScope.launch(Dispatchers.IO) {
            try {
                val userSelectedAlias = getUserSelectedAlias()
                Log.d(TAG, "App went to background - instantly cleaning up old icons, keeping: $userSelectedAlias")
                
                // No delay - cleanup immediately so user doesn't see duplicate icons
                val allAliases = ICON_ALIASES.values + OVERDUE_ICON_ALIASES
                allAliases.forEach { alias ->
                    if (alias != userSelectedAlias) {
                        setComponentEnabled(alias, false)
                        Log.d(TAG, "Disabled old icon: $alias")
                    }
                }
                
                // Mark cleanup as complete
                prefs.edit().putBoolean(PREF_NEEDS_CLEANUP, false).apply()
                
                Log.d(TAG, "Old icons cleanup complete instantly (in background)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cleanup old icons in background", e)
            }
        }
    }
    
    /**
     * Cleanup old icons - disable all icons except the current one.
     * This is kept for backward compatibility but should only be used on app startup
     * if absolutely necessary.
     */
    private fun cleanupOldIcons() {
        try {
            val userSelectedAlias = getUserSelectedAlias()
            Log.d(TAG, "Cleaning up old icons, keeping: $userSelectedAlias")
            
            val allAliases = ICON_ALIASES.values + OVERDUE_ICON_ALIASES
            allAliases.forEach { alias ->
                if (alias != userSelectedAlias) {
                    setComponentEnabled(alias, false)
                    Log.d(TAG, "Disabled old icon: $alias")
                }
            }
            
            Log.d(TAG, "Old icons cleanup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old icons", e)
        }
    }
}