package it.atraj.habittracker.util

import it.atraj.habittracker.data.local.Habit
import it.atraj.habittracker.data.local.HabitCompletion
import it.atraj.habittracker.data.local.HabitFrequency
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class OverdueStatus(
    val isOverdue: Boolean,
    val overdueHours: Long,
    val isWarningLevel: Boolean // true for 2+ hours overdue
)

object OverdueHabitChecker {
    
    // Cache for performance optimization
    private var lastCheckTime: LocalDateTime? = null
    private var cachedOverdueHabits: List<Pair<Habit, OverdueStatus>>? = null
    private const val CACHE_DURATION_MINUTES = 1L // Cache for 1 minute
    
    /**
     * Check if a habit is overdue and by how many hours
     */
    fun checkHabitOverdueStatus(
        habit: Habit,
        completions: List<HabitCompletion>,
        currentTime: LocalDateTime = LocalDateTime.now()
    ): OverdueStatus {
        if (!habit.reminderEnabled || habit.isDeleted) {
            return OverdueStatus(false, 0, false)
        }
        
        val lastScheduledTime = getLastScheduledTime(habit, currentTime)
        val lastCompletionTime = getLastCompletionTime(habit, completions, lastScheduledTime.toLocalDate())
        
        // For daily habits: if completed today, never overdue (even if yesterday was missed)
        if (habit.frequency == HabitFrequency.DAILY) {
            val completedToday = completions.any { it.completedDate == currentTime.toLocalDate() }
            if (completedToday) {
                return OverdueStatus(false, 0, false)
            }
        }
        
        // If habit was completed after the last scheduled time, it's not overdue
        if (lastCompletionTime != null && lastCompletionTime.isAfter(lastScheduledTime)) {
            return OverdueStatus(false, 0, false)
        }
        
        // Calculate hours overdue
        val overdueHours = ChronoUnit.HOURS.between(lastScheduledTime, currentTime)
        
        return OverdueStatus(
            isOverdue = overdueHours > 0,
            overdueHours = overdueHours,
            isWarningLevel = overdueHours >= 2 // Warning starts at 2+ hours
        )
    }
    
    /**
     * Get all habits that are overdue with their status (optimized with caching)
     */
    fun getOverdueHabits(
        habits: List<Habit>,
        allCompletions: Map<Long, List<HabitCompletion>>,
        currentTime: LocalDateTime = LocalDateTime.now(),
        forceRefresh: Boolean = false
    ): List<Pair<Habit, OverdueStatus>> {
        // Use cache if available and not expired
        val lastCheck = lastCheckTime
        if (!forceRefresh && lastCheck != null && cachedOverdueHabits != null) {
            val cacheAge = ChronoUnit.MINUTES.between(lastCheck, currentTime)
            if (cacheAge < CACHE_DURATION_MINUTES) {
                return cachedOverdueHabits!!
            }
        }
        
        // Calculate fresh results
        val results = habits
            .asSequence() // Use sequence for better performance on large lists
            .filter { !it.isDeleted && it.reminderEnabled }
            .mapNotNull { habit ->
                val completions = allCompletions[habit.id] ?: emptyList()
                val status = checkHabitOverdueStatus(habit, completions, currentTime)
                if (status.isOverdue) {
                    Pair(habit, status)
                } else {
                    null
                }
            }
            .toList()
        
        // Update cache
        lastCheckTime = currentTime
        cachedOverdueHabits = results
        
        return results
    }
    
    /**
     * Clear cache to force fresh calculation (call when habits are completed)
     */
    fun clearCache() {
        lastCheckTime = null
        cachedOverdueHabits = null
    }
    
    /**
     * Determine the overall icon state based on overdue habits
     */
    fun determineIconState(overdueHabits: List<Pair<Habit, OverdueStatus>>): IconState {
        if (overdueHabits.isEmpty()) {
            return IconState.DEFAULT
        }
        
        val maxOverdueHours = overdueHabits.maxOfOrNull { it.second.overdueHours } ?: 0
        
        return when {
            maxOverdueHours >= 4 -> IconState.CRITICAL_WARNING // 4+ hours
            maxOverdueHours >= 2 -> IconState.WARNING // 2-3 hours
            else -> IconState.DEFAULT
        }
    }
    
    private fun getLastScheduledTime(habit: Habit, currentTime: LocalDateTime): LocalDateTime {
        val reminderTime = LocalTime.of(habit.reminderHour, habit.reminderMinute)
        val today = currentTime.toLocalDate()
        
        return when (habit.frequency) {
            HabitFrequency.DAILY -> {
                val todayScheduled = today.atTime(reminderTime)
                // Only return today's scheduled time if it has passed
                // Otherwise, return a future time (not overdue)
                if (todayScheduled.isBefore(currentTime) || todayScheduled.isEqual(currentTime)) {
                    todayScheduled
                } else {
                    // Future time - will not be marked as overdue
                    todayScheduled
                }
            }
            
            HabitFrequency.WEEKLY -> {
                val targetDayOfWeek = habit.dayOfWeek ?: 1
                val todayDayOfWeek = today.dayOfWeek.value
                // Only check if today is the scheduled day
                if (todayDayOfWeek == targetDayOfWeek) {
                    val todayScheduled = today.atTime(reminderTime)
                    if (todayScheduled.isBefore(currentTime) || todayScheduled.isEqual(currentTime)) {
                        todayScheduled
                    } else {
                        todayScheduled
                    }
                } else {
                    // Not scheduled for today - return future time
                    currentTime.plusDays(7)
                }
            }
            
            HabitFrequency.MONTHLY -> {
                val targetDayOfMonth = habit.dayOfMonth ?: 1
                val todayDayOfMonth = today.dayOfMonth
                // Only check if today is the scheduled day
                if (todayDayOfMonth == targetDayOfMonth) {
                    val todayScheduled = today.atTime(reminderTime)
                    if (todayScheduled.isBefore(currentTime) || todayScheduled.isEqual(currentTime)) {
                        todayScheduled
                    } else {
                        todayScheduled
                    }
                } else {
                    // Not scheduled for today - return future time
                    currentTime.plusMonths(1)
                }
            }
            
            HabitFrequency.YEARLY -> {
                val targetMonth = habit.monthOfYear ?: 1
                val targetDay = habit.dayOfMonth ?: 1
                val todayMonth = today.monthValue
                val todayDay = today.dayOfMonth
                // Only check if today is the scheduled day
                if (todayMonth == targetMonth && todayDay == targetDay) {
                    val todayScheduled = today.atTime(reminderTime)
                    if (todayScheduled.isBefore(currentTime) || todayScheduled.isEqual(currentTime)) {
                        todayScheduled
                    } else {
                        todayScheduled
                    }
                } else {
                    // Not scheduled for today - return future time
                    currentTime.plusYears(1)
                }
            }
        }
    }
    
    private fun getLastCompletionTime(
        habit: Habit,
        completions: List<HabitCompletion>,
        scheduledDate: LocalDate
    ): LocalDateTime? {
        return completions
            .filter { it.completedDate == scheduledDate }
            .maxByOrNull { it.completedAt }
            ?.completedAt
    }
    
    private fun findLastWeeklySchedule(today: LocalDate, targetDayOfWeek: Int): LocalDate {
        val currentDayOfWeek = today.dayOfWeek.value
        val daysBack = if (currentDayOfWeek >= targetDayOfWeek) {
            currentDayOfWeek - targetDayOfWeek
        } else {
            7 - (targetDayOfWeek - currentDayOfWeek)
        }
        return today.minusDays(daysBack.toLong())
    }
    
    private fun findLastMonthlySchedule(today: LocalDate, targetDayOfMonth: Int): LocalDate {
        val adjustedDay = minOf(targetDayOfMonth, today.lengthOfMonth())
        val thisMonth = today.withDayOfMonth(adjustedDay)
        
        return if (thisMonth.isBefore(today) || thisMonth.isEqual(today)) {
            thisMonth
        } else {
            val lastMonth = today.minusMonths(1)
            val adjustedDayLastMonth = minOf(targetDayOfMonth, lastMonth.lengthOfMonth())
            lastMonth.withDayOfMonth(adjustedDayLastMonth)
        }
    }
    
    private fun findLastYearlySchedule(today: LocalDate, targetMonth: Int, targetDay: Int): LocalDate {
        val thisYear = today.year
        val adjustedDay = try {
            LocalDate.of(thisYear, targetMonth, targetDay)
            targetDay
        } catch (e: Exception) {
            // Handle cases like Feb 29 in non-leap years
            LocalDate.of(thisYear, targetMonth, 1).lengthOfMonth()
        }
        
        val thisYearDate = LocalDate.of(thisYear, targetMonth, adjustedDay)
        
        return if (thisYearDate.isBefore(today) || thisYearDate.isEqual(today)) {
            thisYearDate
        } else {
            val lastYearDate = LocalDate.of(thisYear - 1, targetMonth, targetDay)
            try {
                LocalDate.of(thisYear - 1, targetMonth, targetDay)
            } catch (e: Exception) {
                val adjustedDayLastYear = LocalDate.of(thisYear - 1, targetMonth, 1).lengthOfMonth()
                LocalDate.of(thisYear - 1, targetMonth, adjustedDayLastYear)
            }
        }
    }
}

enum class IconState {
    DEFAULT,
    WARNING,     // 2-3 hours overdue
    CRITICAL_WARNING // 4+ hours overdue
}