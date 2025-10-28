package it.atraj.habittracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import it.atraj.habittracker.data.local.Habit
import it.atraj.habittracker.data.local.HabitFrequency
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for overdue habit notifications
 * Schedules AlarmManager alarms at 2, 3, 4, 5, and 6 hours after habit due time
 */
@Singleton
class OverdueNotificationScheduler @Inject constructor(
    private val context: Context
) {
    
    private val alarmManager: AlarmManager? =
        ContextCompat.getSystemService(context, AlarmManager::class.java)
    
    companion object {
        private const val TAG = "OverdueNotificationScheduler"
        
        // Overdue check intervals in hours
        private val OVERDUE_CHECK_HOURS = listOf(2, 3, 4, 5, 6)
        
        // Recurring interval after 6 hours (every 2 hours)
        private const val RECURRING_INTERVAL_HOURS = 2
        
        // Request code offset to avoid conflicts with regular reminders
        private const val REQUEST_CODE_OFFSET = 10000
    }
    
    /**
     * Schedule overdue notifications for a habit
     * Will create alarms at 2, 3, 4, 5, and 6 hours after the habit's due time
     * This is used when creating/updating habits (schedules for NEXT due time)
     */
    fun scheduleOverdueChecks(habit: Habit) {
        if (habit.id == 0L || !habit.reminderEnabled || habit.isDeleted) {
            Log.d(TAG, "Skipping overdue scheduling for habit ${habit.id}: disabled or deleted")
            return
        }
        
        // Cancel any existing overdue alarms first
        cancelOverdueChecks(habit.id)
        
        // Calculate the next due time for the habit
        val nextDueTime = calculateNextDueTime(habit)
        
        // Schedule alarms for 2, 3, 4, 5, and 6 hours after due time
        OVERDUE_CHECK_HOURS.forEach { hours ->
            val overdueCheckTime = nextDueTime.plusHours(hours.toLong())
            scheduleOverdueAlarm(habit.id, hours, overdueCheckTime)
        }
        
        Log.d(TAG, "Scheduled ${OVERDUE_CHECK_HOURS.size} overdue checks for habit: ${habit.title}")
    }
    
    /**
     * Reschedule overdue notifications based on TODAY'S due time
     * Used to fix alarms that may have been scheduled incorrectly
     * Calculates from today's already-passed due time, not next occurrence
     */
    fun rescheduleOverdueChecksFromToday(habit: Habit) {
        if (habit.id == 0L || !habit.reminderEnabled || habit.isDeleted) {
            Log.d(TAG, "Skipping overdue rescheduling for habit ${habit.id}: disabled or deleted")
            return
        }
        
        // Cancel any existing overdue alarms first
        cancelOverdueChecks(habit.id)
        
        val now = LocalDateTime.now()
        
        // Calculate TODAY'S due time (not next occurrence)
        val todayDueTime = now.withHour(habit.reminderHour)
            .withMinute(habit.reminderMinute)
            .withSecond(0)
            .withNano(0)
        
        // Only schedule if today's due time has already passed (habit is potentially overdue)
        if (todayDueTime.isAfter(now)) {
            Log.d(TAG, "Habit ${habit.title} not yet due today, skipping overdue reschedule")
            return
        }
        
        // Calculate how many hours overdue from today's due time
        val hoursOverdue = java.time.Duration.between(todayDueTime, now).toHours().toInt()
        
        Log.d(TAG, "Rescheduling overdue checks for habit: ${habit.title}, currently ${hoursOverdue}h overdue")
        
        // Schedule remaining future notifications
        if (hoursOverdue < 6) {
            // Schedule initial checks (2-6 hours) that haven't passed yet
            OVERDUE_CHECK_HOURS.filter { it > hoursOverdue }.forEach { hours ->
                val overdueCheckTime = todayDueTime.plusHours(hours.toLong())
                scheduleOverdueAlarm(habit.id, hours, overdueCheckTime)
            }
        } else {
            // Already past 6 hours - schedule next recurring check
            val nextRecurringHours = ((hoursOverdue / RECURRING_INTERVAL_HOURS) + 1) * RECURRING_INTERVAL_HOURS
            val nextCheckTime = todayDueTime.plusHours(nextRecurringHours.toLong())
            scheduleOverdueAlarm(habit.id, nextRecurringHours, nextCheckTime)
        }
    }
    
    /**
     * Cancel all overdue check alarms for a habit
     */
    fun cancelOverdueChecks(habitId: Long) {
        // Cancel initial checks (2-6 hours)
        OVERDUE_CHECK_HOURS.forEach { hours ->
            val pendingIntent = createOverduePendingIntent(habitId, hours)
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        
        // Cancel potential recurring checks (8, 10, 12, 14, 16, 18, 20, 22, 24+ hours)
        // Cancel up to 48 hours worth of recurring notifications
        for (hours in 8..48 step RECURRING_INTERVAL_HOURS) {
            val pendingIntent = createOverduePendingIntent(habitId, hours)
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        
        Log.d(TAG, "Cancelled all overdue checks for habit: $habitId")
    }
    
    /**
     * Schedule a recurring overdue notification (for 6+ hours)
     * Called by OverdueNotificationReceiver after showing a 6+ hour notification
     */
    fun scheduleRecurringOverdueCheck(habit: Habit, totalOverdueHours: Int) {
        if (habit.id == 0L || !habit.reminderEnabled || habit.isDeleted) {
            Log.d(TAG, "Skipping recurring overdue scheduling: habit disabled or deleted")
            return
        }
        
        // Calculate next trigger time (current time + 2 hours)
        val nextTriggerTime = LocalDateTime.now().plusHours(RECURRING_INTERVAL_HOURS.toLong())
        
        // Schedule the alarm
        scheduleOverdueAlarm(habit.id, totalOverdueHours, nextTriggerTime)
        
        Log.d(TAG, "Scheduled recurring overdue check for habit: ${habit.title}, total overdue: ${totalOverdueHours}h")
    }
    
    /**
     * Schedule a single overdue alarm
     */
    private fun scheduleOverdueAlarm(habitId: Long, overdueHours: Int, triggerTime: LocalDateTime) {
        val triggerAtMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        
        // Only schedule if the trigger time is in the future
        if (triggerAtMillis <= now) {
            Log.d(TAG, "Skipping overdue alarm for $overdueHours hours (time already passed)")
            return
        }
        
        val pendingIntent = createOverduePendingIntent(habitId, overdueHours)
        
        // Use exact alarms for precise timing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager?.canScheduleExactAlarms() == false) {
                // Fallback to inexact alarm if exact permission not granted
                alarmManager?.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled inexact overdue alarm for $overdueHours hours at $triggerTime")
                return
            }
        }
        
        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
        
        Log.d(TAG, "Scheduled exact overdue alarm for $overdueHours hours at $triggerTime")
    }
    
    /**
     * Calculate the next due time for a habit based on its frequency
     */
    private fun calculateNextDueTime(habit: Habit): LocalDateTime {
        val now = LocalDateTime.now()
        val baseTime = now.withHour(habit.reminderHour)
            .withMinute(habit.reminderMinute)
            .withSecond(0)
            .withNano(0)
        
        return when (habit.frequency) {
            HabitFrequency.DAILY -> {
                if (baseTime.isBefore(now) || baseTime.isEqual(now)) {
                    baseTime.plusDays(1)
                } else {
                    baseTime
                }
            }
            HabitFrequency.WEEKLY -> {
                val targetDayOfWeek = habit.dayOfWeek ?: 1
                findNextWeeklyDueTime(now, baseTime, targetDayOfWeek)
            }
            HabitFrequency.MONTHLY -> {
                val targetDayOfMonth = habit.dayOfMonth ?: 1
                findNextMonthlyDueTime(now, baseTime, targetDayOfMonth)
            }
            HabitFrequency.YEARLY -> {
                val targetMonth = habit.monthOfYear ?: 1
                val targetDay = habit.dayOfMonth ?: 1
                findNextYearlyDueTime(now, baseTime, targetMonth, targetDay)
            }
        }
    }
    
    private fun findNextWeeklyDueTime(
        now: LocalDateTime,
        baseTime: LocalDateTime,
        targetDayOfWeek: Int
    ): LocalDateTime {
        val currentDayOfWeek = now.dayOfWeek.value
        val daysUntilTarget = if (targetDayOfWeek >= currentDayOfWeek) {
            if (targetDayOfWeek == currentDayOfWeek && (baseTime.isBefore(now) || baseTime.isEqual(now))) {
                7
            } else {
                targetDayOfWeek - currentDayOfWeek
            }
        } else {
            7 - (currentDayOfWeek - targetDayOfWeek)
        }
        return baseTime.plusDays(daysUntilTarget.toLong())
    }
    
    private fun findNextMonthlyDueTime(
        now: LocalDateTime,
        baseTime: LocalDateTime,
        targetDayOfMonth: Int
    ): LocalDateTime {
        var trigger = baseTime.withDayOfMonth(
            minOf(targetDayOfMonth, baseTime.toLocalDate().lengthOfMonth())
        )
        if (trigger.isBefore(now) || trigger.isEqual(now)) {
            trigger = trigger.plusMonths(1)
            trigger = trigger.withDayOfMonth(
                minOf(targetDayOfMonth, trigger.toLocalDate().lengthOfMonth())
            )
        }
        return trigger
    }
    
    private fun findNextYearlyDueTime(
        now: LocalDateTime,
        baseTime: LocalDateTime,
        targetMonth: Int,
        targetDay: Int
    ): LocalDateTime {
        var trigger = baseTime.withMonth(targetMonth).withDayOfMonth(1)
        val daysInMonth = trigger.toLocalDate().lengthOfMonth()
        trigger = trigger.withDayOfMonth(minOf(targetDay, daysInMonth))
        
        if (trigger.isBefore(now) || trigger.isEqual(now)) {
            trigger = trigger.plusYears(1)
            val newDaysInMonth = trigger.toLocalDate().lengthOfMonth()
            trigger = trigger.withDayOfMonth(minOf(targetDay, newDaysInMonth))
        }
        return trigger
    }
    
    /**
     * Create PendingIntent for overdue notification
     */
    private fun createOverduePendingIntent(habitId: Long, overdueHours: Int): PendingIntent {
        val intent = Intent(context, OverdueNotificationReceiver::class.java).apply {
            putExtra(OverdueNotificationReceiver.HABIT_ID_KEY, habitId)
            putExtra(OverdueNotificationReceiver.OVERDUE_HOURS_KEY, overdueHours)
        }
        
        // Use unique request code for each habit and overdue hour combination
        val requestCode = (habitId.toInt() * 100) + overdueHours + REQUEST_CODE_OFFSET
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
