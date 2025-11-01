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
                
                // Get habits whose time has passed (should have been done by now)
                val habitsDueByNow = scheduledToday.filter { habit ->
                    val habitTime = LocalTime.of(habit.reminderHour, habit.reminderMinute)
                    currentTime.isAfter(habitTime)
                }
                
                // Debug logging
                android.util.Log.d("HabitWidget", "=== Widget State Debug ===")
                android.util.Log.d("HabitWidget", "Current time: $currentTime")
                android.util.Log.d("HabitWidget", "Scheduled today: ${scheduledToday.size} habits")
                android.util.Log.d("HabitWidget", "Habits due by now: ${habitsDueByNow.size} (${habitsDueByNow.map { "${it.title} @ ${it.reminderHour}:${it.reminderMinute}" }})")
                android.util.Log.d("HabitWidget", "Completed today: ${completedToday.size} (${completedToday.map { it.title }})")
                android.util.Log.d("HabitWidget", "Overdue habits: ${overdueHabits.size}")
                
                // Determine widget state
                when {
                    // PRIORITY 1: Multiple habits overdue - Show angry urgent state
                    overdueHabits.size > 1 -> {
                        android.util.Log.d("HabitWidget", "STATE: MULTIPLE_OVERDUE (${overdueHabits.size} habits)")
                        setupMultipleOverdueState(context, views, overdueHabits)
                    }
                    
                    // PRIORITY 2: Single habit overdue - Show gentle reminder
                    overdueHabits.size == 1 -> {
                        android.util.Log.d("HabitWidget", "STATE: ONE_OVERDUE (${overdueHabits.first().title})")
                        setupOneOverdueState(context, views, overdueHabits.first())
                    }
                    
                    // PRIORITY 3: All scheduled habits completed - Show champion!
                    completedToday.size == scheduledToday.size && scheduledToday.isNotEmpty() -> {
                        android.util.Log.d("HabitWidget", "STATE: CHAMPION (ALL scheduled habits completed)")
                        val bestHabit = habits
                            .filter { !it.isDeleted && it.reminderEnabled }
                            .maxByOrNull { it.streak }
                        
                        android.util.Log.d("HabitWidget", "Best habit: ${bestHabit?.title}, streak: ${bestHabit?.streak}")
                        
                        if (bestHabit != null && bestHabit.streak > 0) {
                            setupBestHabitState(context, views, bestHabit, completedToday.size)
                        } else {
                            setupAllDoneState(context, views, completedToday.size)
                        }
                    }
                    
                    // PRIORITY 4: Some habits done, no overdue, future habits pending - Show all complete
                    overdueHabits.isEmpty() && completedToday.isNotEmpty() && completedToday.size < scheduledToday.size -> {
                        android.util.Log.d("HabitWidget", "STATE: ALL_COMPLETE (${completedToday.size} done, ${scheduledToday.size - completedToday.size} pending later)")
                        setupAllDoneState(context, views, completedToday.size)
                    }
                    
                    // PRIORITY 5: Morning state - No habits done yet, or no habits scheduled
                    else -> {
                        android.util.Log.d("HabitWidget", "STATE: READY_TO_START (morning/waiting)")
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
        views.setViewVisibility(R.id.widget_image, android.view.View.VISIBLE)
        views.setViewVisibility(R.id.widget_habit_avatar, android.view.View.GONE)
        
        views.setTextViewText(R.id.widget_title, "✨ READY TO START")
        
        val message = generateGeminiMessage(context, 
            "Motivating morning message (max 12 words). Be positive and energetic."
        ) ?: "Ready to conquer today's goals! 💪"
        
        views.setTextViewText(R.id.widget_message, message)
        views.setTextViewText(R.id.widget_habit_info, "No habits due yet")
        
        // Click opens main app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        
        // Set colors for sunrise theme - Bold and modern
        views.setTextColor(R.id.widget_title, android.graphics.Color.parseColor("#92400E"))
        views.setTextColor(R.id.widget_message, android.graphics.Color.parseColor("#451A03"))
        views.setTextColor(R.id.widget_habit_info, android.graphics.Color.parseColor("#78350F"))
    }

    private suspend fun setupAllDoneState(context: Context, views: RemoteViews, completedCount: Int) {
        // Set chill vibe gradient background
        views.setInt(R.id.widget_container, "setBackgroundResource", R.drawable.widget_bg_all_done)
        
        views.setImageViewResource(R.id.widget_image, R.drawable.widget_all_done)
        views.setViewVisibility(R.id.widget_image, android.view.View.VISIBLE)
        views.setViewVisibility(R.id.widget_habit_avatar, android.view.View.GONE)
        
        views.setTextViewText(R.id.widget_title, "🎉 ALL COMPLETE")
        
        val message = generateGeminiMessage(context,
            "Congratulatory message for $completedCount completed habits (max 12 words). Be proud."
        ) ?: "All $completedCount habits crushed! 🌟"
        
        views.setTextViewText(R.id.widget_message, message)
        views.setTextViewText(R.id.widget_habit_info, "$completedCount habits completed 🔥")
        
        // Click opens main app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        
        // Set colors for chill vibe theme - Bold modern green
        views.setTextColor(R.id.widget_title, android.graphics.Color.parseColor("#065F46"))
        views.setTextColor(R.id.widget_message, android.graphics.Color.parseColor("#064E3B"))
        views.setTextColor(R.id.widget_habit_info, android.graphics.Color.parseColor("#047857"))
    }

    /**
     * Setup widget state for best habit with longest streak
     * Shows when all habits are completed - celebrates the champion habit!
     */
    private suspend fun setupBestHabitState(
        context: Context, 
        views: RemoteViews, 
        bestHabit: it.atraj.habittracker.data.local.Habit,
        completedCount: Int
    ) {
        android.util.Log.d("HabitWidget", "Setting up CHAMPION state for: ${bestHabit.title}, streak: ${bestHabit.streak}")
        
        // Set golden victory gradient background
        views.setInt(R.id.widget_container, "setBackgroundResource", R.drawable.widget_bg_all_done)
        
        // Show champion trophy image (background removed)
        views.setImageViewResource(R.id.widget_image, R.drawable.widget_champion)
        views.setViewVisibility(R.id.widget_image, android.view.View.VISIBLE)
        views.setViewVisibility(R.id.widget_habit_avatar, android.view.View.GONE)
        
        views.setTextViewText(R.id.widget_title, "🏆 CHAMPION HABIT")
        
        // Generate personalized message based on habit description  
        val habitDescription = if (bestHabit.description.isNotBlank()) {
            bestHabit.description
        } else {
            bestHabit.title
        }
        
        val message = generateGeminiMessage(context,
            "Motivational message for '${habitDescription}' with ${bestHabit.streak} day streak (max 12 words)."
        ) ?: "${bestHabit.streak} day streak! 🌟"
        
        views.setTextViewText(R.id.widget_message, message)
        views.setTextViewText(R.id.widget_habit_info, "${bestHabit.title} • ${bestHabit.streak} days 🔥")
        
        // Click opens habit details
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val intent = launchIntent?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("habitId", bestHabit.id)
            putExtra("openHabitDetails", true)
        } ?: Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("habitId", bestHabit.id)
            putExtra("openHabitDetails", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            bestHabit.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        
        // Set colors for golden victory theme (same as all done - green success)
        views.setTextColor(R.id.widget_title, android.graphics.Color.parseColor("#065F46"))
        views.setTextColor(R.id.widget_message, android.graphics.Color.parseColor("#064E3B"))
        views.setTextColor(R.id.widget_habit_info, android.graphics.Color.parseColor("#047857"))
    }
    
    /**
     * Load and display habit avatar in widget
     * Returns true if avatar loaded successfully, false if fallback needed
     */
    private fun loadHabitAvatar(context: Context, views: RemoteViews, habit: it.atraj.habittracker.data.local.Habit): Boolean {
        return try {
            when (habit.avatar.type) {
                it.atraj.habittracker.data.local.HabitAvatarType.EMOJI -> {
                    // Create bitmap with emoji text
                    val bitmap = createEmojiBitmap(context, habit.avatar.value, habit.avatar.backgroundColor)
                    if (bitmap != null) {
                        views.setViewVisibility(R.id.widget_image, android.view.View.GONE)
                        views.setViewVisibility(R.id.widget_habit_avatar, android.view.View.VISIBLE)
                        views.setImageViewBitmap(R.id.widget_habit_avatar, bitmap)
                        android.util.Log.d("HabitWidget", "Loaded emoji avatar: ${habit.avatar.value}")
                        true
                    } else {
                        false
                    }
                }
                it.atraj.habittracker.data.local.HabitAvatarType.DEFAULT_ICON -> {
                    // Load icon from drawable resources
                    val iconResource = getIconResourceId(context, habit.avatar.value)
                    if (iconResource != 0) {
                        views.setViewVisibility(R.id.widget_image, android.view.View.GONE)
                        views.setViewVisibility(R.id.widget_habit_avatar, android.view.View.VISIBLE)
                        views.setImageViewResource(R.id.widget_habit_avatar, iconResource)
                        android.util.Log.d("HabitWidget", "Loaded icon avatar: ${habit.avatar.value}")
                        true
                    } else {
                        android.util.Log.e("HabitWidget", "Icon resource not found: ${habit.avatar.value}")
                        false
                    }
                }
                it.atraj.habittracker.data.local.HabitAvatarType.CUSTOM_IMAGE -> {
                    // Load custom image from URI
                    val uri = android.net.Uri.parse(habit.avatar.value)
                    views.setViewVisibility(R.id.widget_image, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_habit_avatar, android.view.View.VISIBLE)
                    views.setImageViewUri(R.id.widget_habit_avatar, uri)
                    android.util.Log.d("HabitWidget", "Loaded custom image avatar: ${habit.avatar.value}")
                    true
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HabitWidget", "Error loading habit avatar", e)
            false
        }
    }
    
    /**
     * Create a bitmap with emoji text on colored background
     */
    private fun createEmojiBitmap(context: Context, emoji: String, backgroundColor: String): android.graphics.Bitmap? {
        return try {
            val size = 200 // Size in pixels (will be scaled down to 80dp by widget)
            val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            
            // Draw background
            val bgPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor(backgroundColor)
                isAntiAlias = true
            }
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)
            
            // Draw emoji text
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = size * 0.6f // 60% of bitmap size
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            
            // Center the emoji
            val xPos = size / 2f
            val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
            
            canvas.drawText(emoji, xPos, yPos, textPaint)
            
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("HabitWidget", "Error creating emoji bitmap", e)
            null
        }
    }
    
    /**
     * Get drawable resource ID from icon name
     */
    private fun getIconResourceId(context: Context, iconName: String): Int {
        return try {
            context.resources.getIdentifier(iconName, "drawable", context.packageName)
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun setupOneOverdueState(
        context: Context, 
        views: RemoteViews, 
        habit: it.atraj.habittracker.data.local.Habit
    ) {
        // Set gray sad gradient background
        views.setInt(R.id.widget_container, "setBackgroundResource", R.drawable.widget_bg_one_overdue)
        
        views.setImageViewResource(R.id.widget_image, R.drawable.widget_1_overdue)
        views.setViewVisibility(R.id.widget_image, android.view.View.VISIBLE)
        views.setViewVisibility(R.id.widget_habit_avatar, android.view.View.GONE)
        
        views.setTextViewText(R.id.widget_title, "⏰ TIME'S UP")
        
        val message = generateGeminiMessage(context,
            "Reminder to complete '${habit.title}' (max 10 words). Be firm."
        ) ?: "Complete '${habit.title}' now! 💪"
        
        views.setTextViewText(R.id.widget_message, message)
        views.setTextViewText(R.id.widget_habit_info, "${habit.title} • ${habit.streak} days 🔥")
        
        // Click opens habit details - Use launcher intent to work with any activity alias
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val intent = launchIntent?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("habitId", habit.id)
            putExtra("openHabitDetails", true)
        } ?: Intent(context, MainActivity::class.java).apply {
            // Fallback if getLaunchIntentForPackage fails
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("habitId", habit.id)
            putExtra("openHabitDetails", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            habit.id.toInt(),
            intent,
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
        views.setViewVisibility(R.id.widget_image, android.view.View.VISIBLE)
        views.setViewVisibility(R.id.widget_habit_avatar, android.view.View.GONE)
        
        views.setTextViewText(R.id.widget_title, "🚨 ACTION REQUIRED")
        
        val count = overdueHabits.size
        val habitNames = overdueHabits.take(2).joinToString(", ") { it.title }
        val moreText = if (count > 2) " +${count - 2}" else ""
        
        val message = generateGeminiMessage(context,
            "$count overdue habits! Urgent reminder (max 10 words)."
        ) ?: "$count habits need attention now! 🔴"
        
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
        
        // Set colors for fiery anger theme - Bold red
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
