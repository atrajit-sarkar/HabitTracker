package it.atraj.habittracker.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppIconManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager = context.packageManager
    private val prefs = context.getSharedPreferences("app_icon_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "AppIconManager"
        private const val PREF_CURRENT_ICON_ID = "current_icon_id"
        private const val PREF_USER_SELECTED_ICON_ID = "user_selected_icon_id"
        private const val PREF_USER_SELECTED_ALIAS = "user_selected_alias"
        private const val PREF_IS_CHANGING_ICON = "is_changing_icon"
        private const val PREF_ICON_CHANGE_TIMESTAMP = "icon_change_timestamp"
        private const val PREF_FIRST_LAUNCH = "is_first_launch"
        
        // Available icon aliases (only user-selected custom icons)
        private val ICON_ALIASES = mapOf(
            "default" to "it.atraj.habittracker.MainActivity",
            "custom1" to "it.atraj.habittracker.MainActivity.Custom1",
            "custom2" to "it.atraj.habittracker.MainActivity.Custom2",
            "ni" to "it.atraj.habittracker.MainActivity.NI",
            "anime" to "it.atraj.habittracker.MainActivity.Anime",
            "sitama" to "it.atraj.habittracker.MainActivity.Sitama"
        )
        
        // Warning/Angry icons managed by OverdueHabitIconManager - DO NOT MODIFY
        private val OVERDUE_ICON_ALIASES = setOf(
            "it.atraj.habittracker.MainActivity.Warning",
            "it.atraj.habittracker.MainActivity.Angry",
            "it.atraj.habittracker.MainActivity.WarningAnime",
            "it.atraj.habittracker.MainActivity.AngryAnime",
            "it.atraj.habittracker.MainActivity.WarningSitama",
            "it.atraj.habittracker.MainActivity.AngrySitama"
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
            
            // Just log the current state - don't actually change components at startup
            // Component changes will happen when user explicitly selects an icon
            Log.d(TAG, "Current icon ID: $currentIconId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AppIconManager", e)
        }
    }
}