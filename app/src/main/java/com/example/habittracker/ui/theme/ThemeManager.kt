package it.atraj.habittracker.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager class to handle app theme selection and persistence
 */
class ThemeManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        THEME_PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    private val _currentThemeFlow = MutableStateFlow(getCurrentTheme())
    val currentThemeFlow: StateFlow<AppTheme> = _currentThemeFlow.asStateFlow()
    
    /**
     * Get the currently selected theme
     */
    fun getCurrentTheme(): AppTheme {
        val themeName = prefs.getString(THEME_KEY, AppTheme.DEFAULT.name) ?: AppTheme.DEFAULT.name
        return try {
            AppTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppTheme.DEFAULT
        }
    }
    
    /**
     * Save the selected theme
     */
    fun setTheme(theme: AppTheme) {
        prefs.edit()
            .putString(THEME_KEY, theme.name)
            .apply()
        _currentThemeFlow.value = theme
    }
    
    companion object {
        private const val THEME_PREFS_NAME = "app_theme_prefs"
        private const val THEME_KEY = "selected_theme"
        
        @Volatile
        private var instance: ThemeManager? = null
        
        fun getInstance(context: Context): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * Composable helper to remember ThemeManager instance
 */
@Composable
fun rememberThemeManager(): ThemeManager {
    val context = LocalContext.current
    return remember { ThemeManager.getInstance(context) }
}
