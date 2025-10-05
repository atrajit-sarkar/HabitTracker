package it.atraj.habittracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import it.atraj.habittracker.data.local.Habit
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

interface HabitReminderScheduler {
    fun schedule(habit: Habit)
    fun cancel(habitId: Long)
}

class HabitReminderSchedulerImpl @Inject constructor(
    private val context: Context
) : HabitReminderScheduler {

    private val alarmManager: AlarmManager? =
        ContextCompat.getSystemService(context, AlarmManager::class.java)

    override fun schedule(habit: Habit) {
        if (habit.id == 0L) return
        val pendingIntent = habit.createPendingIntent(context)
        if (!habit.reminderEnabled) {
            alarmManager?.cancel(pendingIntent)
            return
        }
        val nextTriggerAt = nextTriggerAtMillis(habit)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager?.canScheduleExactAlarms() == false) {
                // Fallback to inexact alarm if exact permission not granted
                alarmManager?.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerAt,
                    pendingIntent
                )
                return
            }
        }
        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTriggerAt,
            pendingIntent
        )
    }

    override fun cancel(habitId: Long) {
        val pendingIntent = createPendingIntent(context, habitId)
        alarmManager?.cancel(pendingIntent)
    }

    private fun nextTriggerAtMillis(habit: Habit): Long {
        val now = LocalDateTime.now()
        val baseTime = now.withHour(habit.reminderHour).withMinute(habit.reminderMinute).withSecond(0).withNano(0)
        
        val nextTrigger = when (habit.frequency) {
            it.atraj.habittracker.data.local.HabitFrequency.DAILY -> {
                if (baseTime.isBefore(now) || baseTime.isEqual(now)) {
                    baseTime.plusDays(1)
                } else {
                    baseTime
                }
            }
            it.atraj.habittracker.data.local.HabitFrequency.WEEKLY -> {
                val targetDayOfWeek = habit.dayOfWeek ?: 1 // Default to Monday
                findNextWeeklyTrigger(now, baseTime, targetDayOfWeek)
            }
            it.atraj.habittracker.data.local.HabitFrequency.MONTHLY -> {
                val targetDayOfMonth = habit.dayOfMonth ?: 1
                findNextMonthlyTrigger(now, baseTime, targetDayOfMonth)
            }
            it.atraj.habittracker.data.local.HabitFrequency.YEARLY -> {
                val targetMonth = habit.monthOfYear ?: 1
                val targetDay = habit.dayOfMonth ?: 1
                findNextYearlyTrigger(now, baseTime, targetMonth, targetDay)
            }
        }
        
        return nextTrigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun findNextWeeklyTrigger(now: LocalDateTime, baseTime: LocalDateTime, targetDayOfWeek: Int): LocalDateTime {
        val currentDayOfWeek = now.dayOfWeek.value // 1 = Monday, 7 = Sunday
        val daysUntilTarget = if (targetDayOfWeek >= currentDayOfWeek) {
            if (targetDayOfWeek == currentDayOfWeek && (baseTime.isBefore(now) || baseTime.isEqual(now))) {
                7 // Next week
            } else {
                targetDayOfWeek - currentDayOfWeek
            }
        } else {
            7 - (currentDayOfWeek - targetDayOfWeek)
        }
        return baseTime.plusDays(daysUntilTarget.toLong())
    }

    private fun findNextMonthlyTrigger(now: LocalDateTime, baseTime: LocalDateTime, targetDayOfMonth: Int): LocalDateTime {
        var trigger = baseTime.withDayOfMonth(minOf(targetDayOfMonth, baseTime.toLocalDate().lengthOfMonth()))
        if (trigger.isBefore(now) || trigger.isEqual(now)) {
            trigger = trigger.plusMonths(1)
            trigger = trigger.withDayOfMonth(minOf(targetDayOfMonth, trigger.toLocalDate().lengthOfMonth()))
        }
        return trigger
    }

    private fun findNextYearlyTrigger(now: LocalDateTime, baseTime: LocalDateTime, targetMonth: Int, targetDay: Int): LocalDateTime {
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

    companion object {
        const val HABIT_ID_KEY = "habit_id"

        fun Habit.createPendingIntent(context: Context): PendingIntent {
            return createPendingIntent(context, id)
        }

        fun createPendingIntent(context: Context, habitId: Long): PendingIntent {
            val intent = Intent(context, HabitReminderReceiver::class.java).apply {
                putExtra(HABIT_ID_KEY, habitId)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.getBroadcast(context, habitId.toInt(), intent, flags)
        }
    }
}
