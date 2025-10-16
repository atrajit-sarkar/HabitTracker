package it.atraj.habittracker.data

import android.util.Log
import it.atraj.habittracker.data.local.Habit
import it.atraj.habittracker.data.local.HabitCompletion
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val TAG = "StreakCalculator"

/**
 * Result of streak calculation
 */
data class StreakCalculationResult(
    val newStreak: Int,
    val diamondsEarned: Int,
    val freezeDaysUsed: Int,
    val graceUsed: Boolean
)

/**
 * Calculates streak with grace period and freeze day logic
 */
object StreakCalculator {
    
    /**
     * Calculate streak for a habit based on completion history
     * 
     * Logic:
     * - First missed day gets automatic grace (no penalty)
     * - Additional consecutive missed days use freeze days if available
     * - If no freeze days, streak decreases by 1 per day
     * - Streak never goes below 0
     * - Rewards: +20 diamonds every 10 days, +N diamonds every 100 days (N = milestone number)
     */
    fun calculateStreak(
        habit: Habit,
        completions: List<HabitCompletion>,
        currentDate: LocalDate,
        availableFreezeDays: Int
    ): StreakCalculationResult {
        
        // If never completed, streak is 0
        if (completions.isEmpty()) {
            Log.d(TAG, "No completions for habit ${habit.title}, streak = 0")
            return StreakCalculationResult(
                newStreak = 0,
                diamondsEarned = 0,
                freezeDaysUsed = 0,
                graceUsed = false
            )
        }
        
        // Sort completions by date (most recent first)
        val sortedCompletions = completions.sortedByDescending { it.completedDate }
        val mostRecentCompletion = sortedCompletions.first().completedDate
        
        Log.d(TAG, "Calculating streak for ${habit.title}, most recent: $mostRecentCompletion, current: $currentDate")
        
        // Calculate days since last completion
        val daysSinceLastCompletion = ChronoUnit.DAYS.between(mostRecentCompletion, currentDate).toInt()
        
        Log.d(TAG, "Days since last completion: $daysSinceLastCompletion")
        
        // If completed today or yesterday, we're good
        if (daysSinceLastCompletion <= 1) {
            val currentStreak = calculateCurrentStreak(sortedCompletions)
            val diamondsEarned = calculateDiamonds(habit.highestStreakAchieved, currentStreak)
            
            Log.d(TAG, "Recent completion, current streak: $currentStreak, diamonds: $diamondsEarned")
            
            return StreakCalculationResult(
                newStreak = currentStreak,
                diamondsEarned = diamondsEarned,
                freezeDaysUsed = 0,
                graceUsed = false
            )
        }
        
        // We have missed days - apply grace and freeze logic
        val missedDays = daysSinceLastCompletion - 1 // Don't count yesterday
        
        Log.d(TAG, "Missed $missedDays days")
        
        // First missed day gets automatic grace
        var graceUsed = false
        var freezeDaysNeeded = missedDays
        
        if (missedDays >= 1) {
            graceUsed = true
            freezeDaysNeeded = missedDays - 1
            Log.d(TAG, "Applied grace for 1 day, need $freezeDaysNeeded freeze days")
        }
        
        // Calculate how many freeze days we can use
        val freezeDaysUsed = minOf(freezeDaysNeeded, availableFreezeDays)
        val unprotectedMissedDays = freezeDaysNeeded - freezeDaysUsed
        
        Log.d(TAG, "Used $freezeDaysUsed freeze days, $unprotectedMissedDays unprotected")
        
        // Calculate new streak
        val currentStreak = calculateCurrentStreak(sortedCompletions)
        var newStreak = currentStreak - unprotectedMissedDays
        
        // Ensure streak never goes below 0
        if (newStreak < 0) {
            newStreak = 0
        }
        
        Log.d(TAG, "New streak: $newStreak (was $currentStreak)")
        
        // No diamonds for broken streaks
        return StreakCalculationResult(
            newStreak = newStreak,
            diamondsEarned = 0,
            freezeDaysUsed = freezeDaysUsed,
            graceUsed = graceUsed
        )
    }
    
    /**
     * Calculate current streak from completion history
     * SPEC: Start from FIRST completion and go FORWARD, applying +1 for completions
     * and penalties for gaps (grace on first missed day of each gap)
     */
    private fun calculateCurrentStreak(sortedCompletions: List<HabitCompletion>): Int {
        if (sortedCompletions.isEmpty()) return 0
        
        // Sort chronologically (oldest first) - opposite of input
        val chronological = sortedCompletions.sortedBy { it.completedDate }
        
        var streak = 0
        var expectedDate = chronological.first().completedDate
        
        // Process each completion from FIRST onwards
        for (completion in chronological) {
            val completionDate = completion.completedDate
            
            // Calculate gap from expected date
            val gapDays = ChronoUnit.DAYS.between(expectedDate, completionDate).toInt()
            
            when {
                gapDays == 0 -> {
                    // Completion on expected date: +1 to streak
                    streak++
                    expectedDate = expectedDate.plusDays(1)
                }
                gapDays > 0 -> {
                    // Gap detected!
                    // First missed day gets grace (no penalty)
                    // Additional missed days: -1 per day
                    val penalty = if (gapDays > 1) gapDays - 1 else 0
                    streak = streak - penalty
                    
                    // If streak went negative, reset to 0
                    if (streak < 0) streak = 0
                    
                    // Add this completion
                    streak++
                    expectedDate = completionDate.plusDays(1)
                }
                // gapDays < 0: shouldn't happen with sorted data
            }
        }
        
        return maxOf(0, streak)
    }
    
    /**
     * Calculate diamond rewards for streak milestones
     * - Every 10 days: +20 diamonds
     * - Every 100 days: +N diamonds (where N = milestone number)
     */
    private fun calculateDiamonds(previousHighest: Int, currentStreak: Int): Int {
        // Only reward if current streak exceeds previous highest
        if (currentStreak <= previousHighest) {
            return 0
        }
        
        var diamonds = 0
        
        // Check each milestone between previousHighest and currentStreak
        for (streak in (previousHighest + 1)..currentStreak) {
            // Every 10 days: +20 diamonds
            if (streak % 10 == 0) {
                diamonds += 20
                Log.d(TAG, "Milestone $streak: +20 diamonds")
            }
            
            // Every 100 days: bonus equal to milestone number
            if (streak % 100 == 0) {
                val bonus = streak // e.g., 100 diamonds at 100 days, 200 at 200 days
                diamonds += bonus
                Log.d(TAG, "Major milestone $streak: +$bonus diamonds")
            }
        }
        
        return diamonds
    }
    
    /**
     * Determine if a missed day should show grace visual
     * Grace applies to the first missed day after last completion
     */
    fun isGraceDay(lastCompletedDate: LocalDate?, date: LocalDate, completions: List<HabitCompletion>): Boolean {
        if (lastCompletedDate == null) return false
        
        // Check if this date is exactly 1 day after the last completion
        val daysSince = ChronoUnit.DAYS.between(lastCompletedDate, date).toInt()
        
        // Grace applies to first missed day
        if (daysSince == 1) {
            // Make sure it's not actually completed
            val isCompleted = completions.any { it.completedDate == date }
            return !isCompleted
        }
        
        return false
    }
    
    /**
     * Determine if a missed day is protected by a freeze
     * Freeze applies to missed days after the grace day
     */
    fun isFreezeDay(
        lastCompletedDate: LocalDate?,
        date: LocalDate,
        completions: List<HabitCompletion>,
        freezeDaysAvailable: Int
    ): Boolean {
        if (lastCompletedDate == null || freezeDaysAvailable <= 0) return false
        
        val daysSince = ChronoUnit.DAYS.between(lastCompletedDate, date).toInt()
        
        // Freeze applies after grace day (day 2+)
        if (daysSince >= 2) {
            // Make sure it's not actually completed
            val isCompleted = completions.any { it.completedDate == date }
            if (isCompleted) return false
            
            // Count how many missed days before this one
            val missedDaysBefore = (1 until daysSince).count { daysAgo ->
                val checkDate = lastCompletedDate.plusDays(daysAgo.toLong())
                !completions.any { it.completedDate == checkDate }
            }
            
            // First missed day is grace (don't count it)
            val freezeDaysNeeded = missedDaysBefore - 1
            
            // This day is protected if we have enough freeze days
            return freezeDaysNeeded < freezeDaysAvailable
        }
        
        return false
    }
}
