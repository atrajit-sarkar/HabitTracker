package it.atraj.habittracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast

/**
 * Helper object for managing widget operations
 */
object WidgetHelper {
    
    /**
     * Request to add widget to home screen
     * @param context Application context
     * @param onSuccess Callback when widget pinning dialog opens successfully
     * @return true if pinning is supported and request was made, false otherwise
     */
    fun requestAddWidget(context: Context, onSuccess: (() -> Unit)? = null): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        
        // Check if the launcher supports widget pinning (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                // Create the widget component
                val widgetProvider = ComponentName(context, HabitWidgetProvider::class.java)
                
                // Create success callback
                val successCallback = PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(context, HabitWidgetProvider::class.java).apply {
                        action = "WIDGET_ADDED_SUCCESS"
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                // Request to pin the widget
                val pinned = appWidgetManager.requestPinAppWidget(
                    widgetProvider,
                    null,
                    successCallback
                )
                
                if (pinned) {
                    Toast.makeText(
                        context,
                        "Widget pinning dialog opened! Place it on your home screen",
                        Toast.LENGTH_LONG
                    ).show()
                    onSuccess?.invoke()
                    return true
                } else {
                    showManualInstructions(context)
                    return false
                }
            } else {
                // Launcher doesn't support pinning
                showManualInstructions(context)
                return false
            }
        } else {
            // Android 7.1 and below - manual addition only
            showManualInstructions(context)
            return false
        }
    }
    
    /**
     * Show manual widget addition instructions
     */
    private fun showManualInstructions(context: Context) {
        Toast.makeText(
            context,
            "Long press on home screen → Widgets → Habit Tracker",
            Toast.LENGTH_LONG
        ).show()
    }
    
    /**
     * Check if widget is already added to home screen
     */
    fun isWidgetAdded(context: Context): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, HabitWidgetProvider::class.java)
        )
        return widgetIds.isNotEmpty()
    }
    
    /**
     * Get count of widgets added
     */
    fun getWidgetCount(context: Context): Int {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        return appWidgetManager.getAppWidgetIds(
            ComponentName(context, HabitWidgetProvider::class.java)
        ).size
    }
}
