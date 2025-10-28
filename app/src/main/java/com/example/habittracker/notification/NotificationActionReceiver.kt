package it.atraj.habittracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import it.atraj.habittracker.data.HabitRepository
import it.atraj.habittracker.receiver.HabitCompletionReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import it.atraj.habittracker.R
import it.atraj.habittracker.ui.theme.AppTheme
import it.atraj.habittracker.ui.theme.ThemeManager
import android.media.MediaPlayer

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var habitRepository: HabitRepository

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra("habitId", -1L)
        if (habitId == -1L) return

        when (intent.action) {
            "COMPLETE_HABIT" -> {
                // Play theme-specific sound immediately (on main thread)
                playThemeCompletionSound(context)
                
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        habitRepository.markCompletedToday(habitId)
                        HabitReminderService.dismissNotification(context, habitId)
                        
                        // Trigger icon check after habit completion
                        HabitCompletionReceiver.sendHabitCompletedBroadcast(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            "COMPLETE_OVERDUE_HABIT" -> {
                // Play theme-specific sound immediately (on main thread)
                playThemeCompletionSound(context)
                
                val overdueHours = intent.getIntExtra("overdueHours", -1)
                
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        habitRepository.markCompletedToday(habitId)
                        
                        // Dismiss the specific overdue notification
                        if (overdueHours != -1) {
                            OverdueNotificationService.dismissOverdueNotification(context, habitId, overdueHours)
                        }
                        
                        // Also dismiss regular reminder notification
                        HabitReminderService.dismissNotification(context, habitId)
                        
                        // Dismiss all other overdue notifications for this habit
                        OverdueNotificationService.dismissAllOverdueNotifications(context, habitId)
                        
                        // Trigger icon check after habit completion
                        HabitCompletionReceiver.sendHabitCompletedBroadcast(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            "DISMISS_HABIT" -> {
                HabitReminderService.dismissNotification(context, habitId)
            }
        }
    }
    
    /**
     * Play theme-specific completion sound for notification actions
     */
    private fun playThemeCompletionSound(context: Context) {
        try {
            // Get current theme
            val themeManager = ThemeManager(context)
            val currentTheme = themeManager.getCurrentTheme()
            
            // Get sound resource based on theme
            val soundRes = when (currentTheme) {
                AppTheme.ITACHI -> R.raw.sharingan_sound
                AppTheme.HALLOWEEN -> R.raw.creepy_sound
                AppTheme.EASTER -> R.raw.goodbye_sound
                AppTheme.COD_MW -> R.raw.ak47_sound
                else -> null // No sound for other themes
            }
            
            // Play sound if available for this theme
            soundRes?.let { resId ->
                val mediaPlayer = MediaPlayer()
                mediaPlayer.setDataSource(
                    context,
                    android.net.Uri.parse("android.resource://${context.packageName}/$resId")
                )
                mediaPlayer.prepare()
                mediaPlayer.setOnCompletionListener { mp ->
                    mp.release() // Release MediaPlayer after sound finishes
                }
                mediaPlayer.start()
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationSound", "Error playing theme sound: ${e.message}")
        }
    }
}
