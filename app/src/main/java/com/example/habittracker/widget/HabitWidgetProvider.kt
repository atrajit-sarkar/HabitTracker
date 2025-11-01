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
import it.atraj.habittracker.gemini.GeminiApiService
import it.atraj.habittracker.gemini.GeminiPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Professional Habit Widget with 4 Dynamic States
 * 
 * States:
 * 1. NO_OVERDUE: Start of day, no scheduled habits yet
 * 2. ONE_OVERDUE: Single habit overdue
 * 3. MULTIPLE_OVERDUE: 2+ habits overdue
 * 4. ALL_DONE: All scheduled habits completed
 */
class HabitWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
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
                val app = context.applicationContext as HabitTrackerApp
                val habitRepository = app.habitRepository
                
                val views = RemoteViews(context.packageName, R.layout.widget_habit_professional)
                
                val habits = habitRepository.getAllHabits().filter { !it.isDeleted }
                val today = LocalDate.now()
                val currentTime = LocalTime.now()
                
                // Get today's scheduled habits (habits that should be done today)
                val scheduledToday = habits.filter { habit ->
                    if (!habit.reminderEnabled) return@filter false
                    
                    when (habit.frequency) {
                        it.atraj.habittracker.data.local.HabitFrequency.DAILY -> true
                        it.atraj.habittracker.data.local.HabitFrequency.WEEKLY -> 
                            habit.dayOfWeek == today.dayOfWeek.value
                        it.atraj.habittracker.data.local.HabitFrequency.MONTHLY -> 
                            habit.dayOfMonth == today.dayOfMonth
                        it.atraj.habittracker.data.local.HabitFrequency.YEARLY -> 
                            habit.monthOfYear == today.monthValue && habit.dayOfMonth == today.dayOfMonth
                    }
                }
                
                // Get overdue habits (time passed, not completed)
                val overdueHabits = scheduledToday.filter { habit ->
                    val habitTime = LocalTime.of(habit.reminderHour, habit.reminderMinute)
                    currentTime.isAfter(habitTime) && habit.lastCompletedDate != today
                }
                
                // Get completed today
                val completedToday = scheduledToday.filter { it.lastCompletedDate == today }
                
                // Determine widget state
                when {
                    scheduledToday.isEmpty() -> {
                        // NO_OVERDUE: No habits scheduled today
                        setupNoOverdueState(context, views)
                    }
                    
                    overdueHabits.isEmpty() && completedToday.isNotEmpty() -> {
                        // ALL_DONE: All scheduled habits completed
                        setupAllDoneState(context, views, completedToday.size)
                    }
                    
                    overdueHabits.size == 1 -> {
                        // ONE_OVERDUE: Single habit overdue
                        setupOneOverdueState(context, views, overdueHabits.first())
                    }
                    
                    overdueHabits.size > 1 -> {
                        // MULTIPLE_OVERDUE: Multiple habits overdue
                        setupMultipleOverdueState(context, views, overdueHabits)
                    }
                    
                    else -> {
                        // NO_OVERDUE: Waiting for scheduled time
                        setupNoOverdueState(context, views)
                    }
                }
                
                appWidgetManager.updateAppWidget(appWidgetId, views)
                
            } catch (e: Exception) {
                e.printStackTrace()
                val views = RemoteViews(context.packageName, R.layout.widget_habit_professional)
                views.setTextViewText(R.id.widget_title, "Error")
                views.setTextViewText(R.id.widget_message, "Unable to load habits")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private suspend fun setupNoOverdueState(context: Context, views: RemoteViews) {
        // Set morning sunrise gradient background
        views.setInt(R.id.widget_container, "setBackgroundResource", R.drawable.widget_bg_morning)
        
        views.setImageViewResource(R.id.widget_image, R.drawable.widget_no_overdue)
        views.setTextViewText(R.id.widget_title, "✨ Great Start!")
        
        val message = generateGeminiMessage(context, 
            "Generate a motivating 'good morning' or 'new day' message (max 20 words) for someone starting their day with no overdue habits. Be encouraging and positive. Don't use emojis."
        ) ?: "Good morning! Ready to crush your goals today? You've got this! 💪"
        
        views.setTextViewText(R.id.widget_message, message)
        views.setTextViewText(R.id.widget_habit_info, "No habits due yet")
        
        // Click opens main app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        
        // Set colors for sunrise theme
        views.setTextColor(R.id.widget_title, android.graphics.Color.parseColor("#92400E"))
        views.setTextColor(R.id.widget_message, android.graphics.Color.parseColor("#451A03"))
        views.setTextColor(R.id.widget_habit_info, android.graphics.Color.parseColor("#78350F"))
    }

    private suspend fun setupAllDoneState(context: Context, views: RemoteViews, completedCount: Int) {
        // Set chill vibe gradient background
        views.setInt(R.id.widget_container, "setBackgroundResource", R.drawable.widget_bg_all_done)
        
        views.setImageViewResource(R.id.widget_image, R.drawable.widget_all_done)
        views.setTextViewText(R.id.widget_title, "🎉 Amazing!")
        
        val message = generateGeminiMessage(context,
            "Generate a congratulatory message (max 20 words) for someone who completed all $completedCount habits today. Be proud and encouraging. Don't use emojis."
        ) ?: "Incredible work! You completed all $completedCount habits today! Keep this momentum going! 🌟"
        
        views.setTextViewText(R.id.widget_message, message)
        views.setTextViewText(R.id.widget_habit_info, "All $completedCount habits completed! 🔥")
        
        // Click opens main app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        
        // Set colors for chill vibe theme
        views.setTextColor(R.id.widget_title, android.graphics.Color.parseColor("#065F46"))
        views.setTextColor(R.id.widget_message, android.graphics.Color.parseColor("#064E3B"))
        views.setTextColor(R.id.widget_habit_info, android.graphics.Color.parseColor("#047857"))
    }

    private suspend fun setupOneOverdueState(
        context: Context, 
        views: RemoteViews, 
        habit: it.atraj.habittracker.data.local.Habit
    ) {
        // Set gray sad gradient background
        views.setInt(R.id.widget_container, "setBackgroundResource", R.drawable.widget_bg_one_overdue)
        
        views.setImageViewResource(R.id.widget_image, R.drawable.widget_1_overdue)
        views.setTextViewText(R.id.widget_title, "⏰ Time's Up!")
        
        val message = generateGeminiMessage(context,
            "Generate a motivating reminder (max 20 words) to complete habit '${habit.title}'. Be encouraging but firm. Don't use emojis."
        ) ?: "Time to complete '${habit.title}'! Don't break your streak! You can do this! 💪"
        
        views.setTextViewText(R.id.widget_message, message)
        views.setTextViewText(R.id.widget_habit_info, "${habit.title} • ${habit.streak} day streak 🔥")
        
        // Click opens habit details
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "it.atraj.habittracker.OPEN_HABIT_DETAILS"
            putExtra("habitId", habit.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, habit.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        
        // Set colors for sad gray theme
        views.setTextColor(R.id.widget_title, android.graphics.Color.parseColor("#374151"))
        views.setTextColor(R.id.widget_message, android.graphics.Color.parseColor("#1F2937"))
        views.setTextColor(R.id.widget_habit_info, android.graphics.Color.parseColor("#4B5563"))
    }

    private suspend fun setupMultipleOverdueState(
        context: Context,
        views: RemoteViews,
        overdueHabits: List<it.atraj.habittracker.data.local.Habit>
    ) {
        // Set fiery anger gradient background
        views.setInt(R.id.widget_container, "setBackgroundResource", R.drawable.widget_bg_multiple_overdue)
        
        views.setImageViewResource(R.id.widget_image, R.drawable.widget_more_overdue)
        views.setTextViewText(R.id.widget_title, "🚨 Urgent!")
        
        val count = overdueHabits.size
        val habitNames = overdueHabits.take(2).joinToString(", ") { it.title }
        val moreText = if (count > 2) " + ${count - 2} more" else ""
        
        val message = generateGeminiMessage(context,
            "Generate a firm, urgent message (max 20 words) for someone with $count overdue habits. Be direct and motivating. Create urgency. Don't use emojis."
        ) ?: "You have $count overdue habits! Time to take action NOW! Don't let them pile up! 🔴"
        
        views.setTextViewText(R.id.widget_message, message)
        views.setTextViewText(R.id.widget_habit_info, "$habitNames$moreText")
        
        // Click opens OverdueHabitsActivity to show all overdue habits with urgency animations
        val intent = Intent(context, it.atraj.habittracker.ui.OverdueHabitsActivity::class.java).apply {
            action = "it.atraj.habittracker.SHOW_OVERDUE_HABITS"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1000, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        
        // Set colors for fiery anger theme
        views.setTextColor(R.id.widget_title, android.graphics.Color.parseColor("#991B1B"))
        views.setTextColor(R.id.widget_message, android.graphics.Color.parseColor("#7F1D1D"))
        views.setTextColor(R.id.widget_habit_info, android.graphics.Color.parseColor("#B91C1C"))
    }

    private suspend fun generateGeminiMessage(context: Context, prompt: String): String? {
        return try {
            val geminiPrefs = GeminiPreferences(context)
            if (!geminiPrefs.isGeminiEnabled()) return null
            
            val apiKey = geminiPrefs.getApiKey()
            if (apiKey.isNullOrBlank()) return null
            
            val geminiService = GeminiApiService(apiKey)
            val result = geminiService.generateCustomMessage(prompt)
            result.getOrNull()
        } catch (e: Exception) {
            null
        }
    }

    override fun onEnabled(context: Context) {
        // First widget created
    }

    override fun onDisabled(context: Context) {
        // Last widget removed
    }

    companion object {
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
