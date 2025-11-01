package it.atraj.habittracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import it.atraj.habittracker.HabitTrackerApp
import it.atraj.habittracker.MainActivity
import it.atraj.habittracker.R
import it.atraj.habittracker.data.HabitRepository
import it.atraj.habittracker.gemini.GeminiApiService
import it.atraj.habittracker.gemini.GeminiPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Professional Habit Widget Provider
 * 
 * Features:
 * - Shows due habit with Gemini-powered motivational text
 * - Shows best streak habit when no habits are due
 * - Opens habit details on click
 */
class HabitWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update all widget instances
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        scope.launch {
            try {
                // Get HabitRepository from Hilt app component
                val app = context.applicationContext as HabitTrackerApp
                val habitRepository = app.habitRepository
                
                val views = RemoteViews(context.packageName, R.layout.widget_habit_layout)
                
                // Get all habits
                val habits = habitRepository.getAllHabits().filter { !it.isDeleted }
                
                if (habits.isEmpty()) {
                    // No habits case
                    views.setTextViewText(R.id.widget_title, "No Habits Yet")
                    views.setTextViewText(R.id.widget_message, "Create your first habit to get started!")
                    views.setTextViewText(R.id.widget_streak_info, "")
                    
                    // Click opens main activity
                    val intent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                    
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return@launch
                }
                
                // Check for due habits
                val currentTime = LocalTime.now()
                val today = LocalDate.now()
                
                val dueHabits = habits.filter { habit ->
                    if (!habit.reminderEnabled) return@filter false
                    
                    // Check if habit is due today based on frequency
                    val isDueToday = when (habit.frequency) {
                        it.atraj.habittracker.data.local.HabitFrequency.DAILY -> true
                        it.atraj.habittracker.data.local.HabitFrequency.WEEKLY -> 
                            habit.dayOfWeek == today.dayOfWeek.value
                        it.atraj.habittracker.data.local.HabitFrequency.MONTHLY -> 
                            habit.dayOfMonth == today.dayOfMonth
                        it.atraj.habittracker.data.local.HabitFrequency.YEARLY -> 
                            habit.monthOfYear == today.monthValue && habit.dayOfMonth == today.dayOfMonth
                    }
                    
                    if (!isDueToday) return@filter false
                    
                    // Check if time has passed
                    val habitTime = LocalTime.of(habit.reminderHour, habit.reminderMinute)
                    currentTime.isAfter(habitTime) && habit.lastCompletedDate != today
                }
                
                if (dueHabits.isNotEmpty()) {
                    // Show first due habit with motivational message
                    val dueHabit = dueHabits.first()
                    
                    views.setTextViewText(R.id.widget_title, "⏰ Time for:")
                    views.setTextViewText(R.id.widget_habit_name, dueHabit.title)
                    
                    // Generate Gemini-powered motivational text
                    val geminiPrefs = GeminiPreferences(context)
                    val motivationalText = if (geminiPrefs.isGeminiEnabled()) {
                        generateMotivationalText(context, dueHabit.title)
                    } else {
                        getDefaultMotivationalText(dueHabit.title)
                    }
                    
                    views.setTextViewText(R.id.widget_message, motivationalText)
                    views.setTextViewText(
                        R.id.widget_streak_info, 
                        "Current Streak: ${dueHabit.streak} days 🔥"
                    )
                    
                    // Click opens habit details
                    val intent = Intent(context, MainActivity::class.java).apply {
                        action = "it.atraj.habittracker.OPEN_HABIT_DETAILS"
                        putExtra("habitId", dueHabit.id)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        dueHabit.id.toInt(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                    
                } else {
                    // No due habits - show best streak habit
                    val bestStreakHabit = habits.maxByOrNull { it.highestStreakAchieved }
                    
                    if (bestStreakHabit != null) {
                        views.setTextViewText(R.id.widget_title, "🏆 Your Best Performance")
                        views.setTextViewText(R.id.widget_habit_name, bestStreakHabit.title)
                        views.setTextViewText(
                            R.id.widget_message, 
                            "You're doing great! Keep up the momentum."
                        )
                        views.setTextViewText(
                            R.id.widget_streak_info, 
                            "Highest Streak: ${bestStreakHabit.highestStreakAchieved} days 🔥"
                        )
                        
                        // Click opens habit details
                        val intent = Intent(context, MainActivity::class.java).apply {
                            action = "it.atraj.habittracker.OPEN_HABIT_DETAILS"
                            putExtra("habitId", bestStreakHabit.id)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            bestStreakHabit.id.toInt(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                    }
                }
                
                appWidgetManager.updateAppWidget(appWidgetId, views)
                
            } catch (e: Exception) {
                e.printStackTrace()
                // Show error state
                val views = RemoteViews(context.packageName, R.layout.widget_habit_layout)
                views.setTextViewText(R.id.widget_title, "Error")
                views.setTextViewText(R.id.widget_message, "Unable to load habits")
                views.setTextViewText(R.id.widget_streak_info, "")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private suspend fun generateMotivationalText(context: Context, habitName: String): String {
        return try {
            val geminiPrefs = GeminiPreferences(context)
            val apiKey = geminiPrefs.getApiKey()
            
            if (apiKey.isNullOrBlank()) {
                return getDefaultMotivationalText(habitName)
            }
            
            val geminiService = GeminiApiService(apiKey)
            val prompt = """
                Generate a short, motivating message (max 15 words) to encourage someone to complete their habit: "$habitName".
                Be enthusiastic and action-oriented. Don't use emojis.
            """.trimIndent()
            
            val result = geminiService.generateCustomMessage(prompt)
            result.getOrNull() ?: getDefaultMotivationalText(habitName)
            
        } catch (e: Exception) {
            getDefaultMotivationalText(habitName)
        }
    }

    private fun getDefaultMotivationalText(habitName: String): String {
        val messages = listOf(
            "Let's do this! Time to build your streak.",
            "You've got this! Complete your habit now.",
            "Don't break the chain! Take action now.",
            "Your future self will thank you. Do it now!",
            "Small steps, big results. Let's go!",
            "Consistency is key. Complete this now!",
            "Make today count! Time for your habit."
        )
        return messages.random()
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality when first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality when last widget is disabled
    }

    companion object {
        /**
         * Request widget update from anywhere in the app
         */
        fun requestUpdate(context: Context) {
            val intent = Intent(context, HabitWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(android.content.ComponentName(context, HabitWidgetProvider::class.java))
            
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
