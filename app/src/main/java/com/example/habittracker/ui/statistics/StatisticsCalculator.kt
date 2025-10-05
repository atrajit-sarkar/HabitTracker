package it.atraj.habittracker.ui.statistics

import it.atraj.habittracker.data.local.HabitFrequency
import it.atraj.habittracker.ui.HabitCardUi
import it.atraj.habittracker.ui.HabitViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class HabitStatistics(
    val totalHabits: Int,
    val totalCompletions: Int,
    val completionRate: Int,
    val longestStreak: Int,
    val currentStreak: Int,
    val activeTodayCount: Int,
    val completedTodayCount: Int,
    val averagePerDay: Double,
    val bestDay: String,
    val performanceScore: Int,
    val dailyHabitsCount: Int,
    val weeklyHabitsCount: Int,
    val monthlyHabitsCount: Int,
    val yearlyHabitsCount: Int,
    val weeklyData: List<Pair<String, Int>>,
    val monthlyData: List<Pair<String, Int>>,
    val habitComparisons: List<HabitComparison>,
    val topHabits: List<HabitComparison>,
    val weekdayPerformance: List<Pair<String, Int>>
)

data class HabitComparison(
    val habitId: Long,
    val habitName: String,
    val habitEmoji: String,
    val completionCount: Int,
    val currentStreak: Int,
    val completionRate: Int
)

suspend fun calculateStatistics(
    habits: List<HabitCardUi>,
    viewModel: HabitViewModel
): HabitStatistics = coroutineScope {
    if (habits.isEmpty()) {
        return@coroutineScope HabitStatistics(
            totalHabits = 0,
            totalCompletions = 0,
            completionRate = 0,
            longestStreak = 0,
            currentStreak = 0,
            activeTodayCount = 0,
            completedTodayCount = 0,
            averagePerDay = 0.0,
            bestDay = "N/A",
            performanceScore = 0,
            dailyHabitsCount = 0,
            weeklyHabitsCount = 0,
            monthlyHabitsCount = 0,
            yearlyHabitsCount = 0,
            weeklyData = emptyList(),
            monthlyData = emptyList(),
            habitComparisons = emptyList(),
            topHabits = emptyList(),
            weekdayPerformance = emptyList()
        )
    }

    // Fetch all habit progress data in parallel
    val habitProgressList = habits.map { habit ->
        async {
            try {
                val progress = viewModel.getHabitProgress(habit.id)
                habit to progress
            } catch (e: Exception) {
                null
            }
        }
    }.awaitAll().filterNotNull()

    // Calculate total completions across all habits
    val totalCompletions = habitProgressList.sumOf { (_, progress) ->
        progress.completedDates.size
    }

    // Calculate completed today count
    val today = LocalDate.now()
    val completedTodayCount = habits.count { it.isCompletedToday }

    // Calculate completion rate (percentage of habits completed today)
    val activeTodayCount = habits.size
    val completionRate = if (activeTodayCount > 0) {
        (completedTodayCount * 100) / activeTodayCount
    } else {
        0
    }

    // Calculate streaks
    var longestStreak = 0
    var currentStreak = 0
    
    habitProgressList.forEach { (_, progress) ->
        if (progress.currentStreak > longestStreak) {
            longestStreak = progress.currentStreak
        }
        if (progress.currentStreak > currentStreak) {
            currentStreak = progress.currentStreak
        }
    }

    // Calculate weekly data (last 7 days)
    val weeklyData = (6 downTo 0).map { daysAgo ->
        val date = today.minusDays(daysAgo.toLong())
        val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val completionsOnDay = habitProgressList.count { (_, progress) ->
            progress.completedDates.contains(date)
        }
        dayName to completionsOnDay
    }

    // Calculate monthly data (last 30 days)
    val monthlyData = (29 downTo 0).map { daysAgo ->
        val date = today.minusDays(daysAgo.toLong())
        val label = if (daysAgo % 5 == 0) date.dayOfMonth.toString() else ""
        val completionsOnDay = habitProgressList.count { (_, progress) ->
            progress.completedDates.contains(date)
        }
        label to completionsOnDay
    }

    // Calculate weekday performance
    val weekdayPerformance = DayOfWeek.values().map { dayOfWeek ->
        val dayName = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val completionsOnWeekday = habitProgressList.sumOf { (_, progress) ->
            progress.completedDates.count { date ->
                date.dayOfWeek == dayOfWeek
            }
        }
        dayName to completionsOnWeekday
    }

    // Find best performing day
    val bestDay = weekdayPerformance.maxByOrNull { it.second }?.first ?: "N/A"

    // Calculate average completions per day
    val oldestHabitDate = habitProgressList.mapNotNull { (_, progress) ->
        progress.completedDates.minOrNull()
    }.minOrNull() ?: today
    
    val daysSinceStart = kotlin.math.max(
        java.time.temporal.ChronoUnit.DAYS.between(oldestHabitDate, today).toInt() + 1,
        1
    )
    val averagePerDay = totalCompletions.toDouble() / daysSinceStart

    // Calculate frequency distribution
    val dailyHabitsCount = habits.count { it.frequency == HabitFrequency.DAILY }
    val weeklyHabitsCount = habits.count { it.frequency == HabitFrequency.WEEKLY }
    val monthlyHabitsCount = habits.count { it.frequency == HabitFrequency.MONTHLY }
    val yearlyHabitsCount = habits.count { it.frequency == HabitFrequency.YEARLY }

    // Calculate habit comparisons
    val habitComparisons = habitProgressList.map { (habit, progress) ->
        HabitComparison(
            habitId = habit.id,
            habitName = habit.title,
            habitEmoji = habit.avatar.value,
            completionCount = progress.completedDates.size,
            currentStreak = progress.currentStreak,
            completionRate = if (daysSinceStart > 0) {
                (progress.completedDates.size * 100) / daysSinceStart
            } else {
                0
            }
        )
    }.sortedByDescending { it.completionCount }

    // Top 5 habits
    val topHabits = habitComparisons.take(5)

    // Calculate performance score (0-100)
    val performanceScore = calculatePerformanceScore(
        completionRate = completionRate,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        totalCompletions = totalCompletions,
        totalHabits = habits.size
    )

    HabitStatistics(
        totalHabits = habits.size,
        totalCompletions = totalCompletions,
        completionRate = completionRate,
        longestStreak = longestStreak,
        currentStreak = currentStreak,
        activeTodayCount = activeTodayCount,
        completedTodayCount = completedTodayCount,
        averagePerDay = averagePerDay,
        bestDay = bestDay,
        performanceScore = performanceScore,
        dailyHabitsCount = dailyHabitsCount,
        weeklyHabitsCount = weeklyHabitsCount,
        monthlyHabitsCount = monthlyHabitsCount,
        yearlyHabitsCount = yearlyHabitsCount,
        weeklyData = weeklyData,
        monthlyData = monthlyData,
        habitComparisons = habitComparisons,
        topHabits = topHabits,
        weekdayPerformance = weekdayPerformance
    )
}

private fun calculatePerformanceScore(
    completionRate: Int,
    currentStreak: Int,
    longestStreak: Int,
    totalCompletions: Int,
    totalHabits: Int
): Int {
    // Weighted scoring system
    val completionRateScore = (completionRate * 0.4).toInt() // 40% weight
    val streakScore = ((currentStreak.coerceAtMost(30) * 100 / 30) * 0.3).toInt() // 30% weight
    val consistencyScore = ((longestStreak.coerceAtMost(30) * 100 / 30) * 0.2).toInt() // 20% weight
    val volumeScore = if (totalHabits > 0) {
        ((totalCompletions.coerceAtMost(100) * 100 / 100) * 0.1).toInt() // 10% weight
    } else {
        0
    }
    
    return (completionRateScore + streakScore + consistencyScore + volumeScore).coerceIn(0, 100)
}
