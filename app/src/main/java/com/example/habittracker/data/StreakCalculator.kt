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
    val freezeDaysUsed: Int, // INCREMENTAL freeze days used (only new deductions)
    val graceUsed: Boolean,
    val gapStartDate: LocalDate?, // When current gap started
    val totalFreezeDaysUsedForGap: Int // Total freeze days used for this gap so far
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
                graceUsed = false,
                gapStartDate = null,
                totalFreezeDaysUsedForGap = 0
            )
        }
        
        // Sort completions by date (most recent first)
        val sortedCompletions = completions.sortedByDescending { it.completedDate }
        val mostRecentCompletion = sortedCompletions.first().completedDate
        
        Log.d(TAG, "Calculating streak for ${habit.title}, most recent: $mostRecentCompletion, current: $currentDate")
        
        // Calculate days since last completion
        val daysSinceLastCompletion = ChronoUnit.DAYS.between(mostRecentCompletion, currentDate).toInt()
        
        Log.d(TAG, "Days since last completion: $daysSinceLastCompletion")
        
        // If completed today, we're good
        if (daysSinceLastCompletion == 0) {
            val currentStreak = calculateCurrentStreak(sortedCompletions)
            val diamondsEarned = calculateDiamonds(habit.highestStreakAchieved, currentStreak)
            
            Log.d(TAG, "Completed today, current streak: $currentStreak, diamonds: $diamondsEarned")
            
            return StreakCalculationResult(
                newStreak = currentStreak,
                diamondsEarned = diamondsEarned,
                freezeDaysUsed = 0,
                graceUsed = false,
                gapStartDate = null, // No gap - completed today
                totalFreezeDaysUsedForGap = 0
            )
        }
        
        // If last completion was yesterday, streak is maintained but no new diamonds
        // (user still has today to complete and continue the streak)
        if (daysSinceLastCompletion == 1) {
            val currentStreak = calculateCurrentStreak(sortedCompletions)
            
            Log.d(TAG, "Last completion yesterday, maintaining streak: $currentStreak")
            
            return StreakCalculationResult(
                newStreak = currentStreak,
                diamondsEarned = 0,
                freezeDaysUsed = 0,
                graceUsed = false,
                gapStartDate = null, // Still in grace period
                totalFreezeDaysUsedForGap = 0
            )
        }
        
        // We have missed days - apply grace and freeze logic
        // Only count completed missed days (not including today)
        val missedDays = daysSinceLastCompletion - 1 // Don't count today
        
        Log.d(TAG, "Missed $missedDays days")
        
        // CRITICAL FIX: Determine gap start date and calculate INCREMENTAL freeze usage
        val gapStartDate = mostRecentCompletion.plusDays(1) // First day after last completion
        val isNewGap = habit.currentGapStartDate != gapStartDate
        
        if (isNewGap) {
            // This is a NEW gap - reset freeze tracking
            Log.d(TAG, "NEW gap detected starting ${gapStartDate}, resetting freeze tracking")
        } else {
            Log.d(TAG, "EXISTING gap from ${habit.currentGapStartDate}, already used ${habit.freezeDaysUsedForCurrentGap} freeze days")
        }
        
        // Calculate TOTAL freeze days needed for this entire gap
        var graceUsed = false
        var freezeDaysNeededTotal = missedDays
        
        if (missedDays >= 1) {
            graceUsed = true
            freezeDaysNeededTotal = missedDays - 1
            Log.d(TAG, "Applied grace for 1 day, total freeze needed for gap: $freezeDaysNeededTotal")
        }
        
        // Calculate INCREMENTAL freeze days needed (only what we haven't already used)
        val alreadyUsedForThisGap = if (isNewGap) 0 else habit.freezeDaysUsedForCurrentGap
        val freezeDaysNeededIncremental = (freezeDaysNeededTotal - alreadyUsedForThisGap).coerceAtLeast(0)

        Log.d(TAG, "Incremental freeze needed: $freezeDaysNeededIncremental (total needed: $freezeDaysNeededTotal, already used: $alreadyUsedForThisGap)")
        
        // Calculate how many freeze days we can use RIGHT NOW
        val freezeDaysUsed = minOf(freezeDaysNeededIncremental, availableFreezeDays)
        val totalFreezeDaysUsedForGap = alreadyUsedForThisGap + freezeDaysUsed
        
        // Calculate unprotected days based on TOTAL needs vs TOTAL usage
        val unprotectedMissedDays = (freezeDaysNeededTotal - totalFreezeDaysUsedForGap).coerceAtLeast(0)
        
        Log.d(TAG, "Using $freezeDaysUsed NEW freeze days (total for gap now: $totalFreezeDaysUsedForGap), $unprotectedMissedDays unprotected")
        
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
            freezeDaysUsed = freezeDaysUsed, // INCREMENTAL freeze days used
            graceUsed = graceUsed,
            gapStartDate = gapStartDate,
            totalFreezeDaysUsedForGap = totalFreezeDaysUsedForGap
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
     * Only shows for PAST dates (not today) since today is still ongoing
     */
    fun isGraceDay(lastCompletedDate: LocalDate?, date: LocalDate, completions: List<HabitCompletion>): Boolean {
        if (lastCompletedDate == null) return false
        
        // Don't show grace on today - user still has time to complete
        val today = LocalDate.now()
        if (date >= today) return false
        
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
     * 
     * IMPORTANT: This should only show freeze for dates that are ACTUALLY protected.
     * The issue is that freeze days are consumed chronologically across ALL gaps in the habit,
     * not per-gap. We need to simulate the entire streak calculation to know which specific
     * dates consumed freeze days.
     * 
     * Simplified approach: A date gets a freeze icon ONLY if:
     * 1. It's a missed day (not completed)
     * 2. It's after the first freeze purchase date
     * 3. It's part of the CURRENT ongoing gap (from most recent completion to today)
     * 4. It's within the freeze coverage for that current gap
     * 
     * For old/historical gaps, we DON'T show freeze icons because we can't accurately
     * determine which historical days actually consumed freeze vs penalty.
     */
    fun isFreezeDay(
        lastCompletedDate: LocalDate?,
        date: LocalDate,
        completions: List<HabitCompletion>,
        freezeDaysAvailable: Int,
        firstFreezePurchaseDate: LocalDate? = null
    ): Boolean {
        if (lastCompletedDate == null || freezeDaysAvailable <= 0) return false
        
        // Don't show freeze on today - user still has time to complete
        val today = LocalDate.now()
        if (date >= today) return false
        
        // Don't show freeze for dates before freeze was purchased
        if (firstFreezePurchaseDate != null && date < firstFreezePurchaseDate) {
            return false
        }
        
        // Make sure it's not actually completed
        val isCompleted = completions.any { it.completedDate == date }
        if (isCompleted) return false
        
        // CRITICAL FIX: Only show freeze icons for the MOST RECENT gap
        // (from last completion to today), not for historical gaps
        val sortedCompletions = completions.sortedByDescending { it.completedDate }
        if (sortedCompletions.isEmpty()) return false
        
        val mostRecentCompletion = sortedCompletions.first().completedDate
        
        // Only process if this date is in the current gap (after most recent completion)
        if (date <= mostRecentCompletion) {
            return false // This is a historical gap, don't show freeze
        }
        
        // Calculate days since most recent completion
        val daysSince = ChronoUnit.DAYS.between(mostRecentCompletion, date).toInt()
        
        // Freeze applies after grace day (day 2+)
        if (daysSince >= 2) {
            // Find all missed dates in current gap (from most recent completion to today)
            val allMissedDatesInCurrentGap = mutableListOf<LocalDate>()
            val daysSinceToToday = ChronoUnit.DAYS.between(mostRecentCompletion, today).toInt()
            
            for (i in 1 until daysSinceToToday) { // Exclude today
                val checkDate = mostRecentCompletion.plusDays(i.toLong())
                if (!completions.any { it.completedDate == checkDate }) {
                    allMissedDatesInCurrentGap.add(checkDate)
                }
            }
            allMissedDatesInCurrentGap.sort()
            
            // Find position of this date
            val positionOfThisDate = allMissedDatesInCurrentGap.indexOf(date) + 1 // 1-indexed
            
            if (positionOfThisDate < 1) return false // Date not in missed dates list
            
            // Calculate protection:
            // Position 1 = Grace (no freeze icon)
            // Positions 2 to (1 + freezeDaysAvailable) = Freeze protected
            // Beyond that = Penalty (no freeze icon)
            
            val graceDays = 1
            val freezeProtectedDays = freezeDaysAvailable
            val totalProtectedDays = graceDays + freezeProtectedDays
            
            return positionOfThisDate > graceDays && positionOfThisDate <= totalProtectedDays
        }
        
        return false
    }
}
