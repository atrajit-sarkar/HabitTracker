# Leaderboard Current Streak Fix

## Issue
The current streak displayed in the leaderboard was not matching the actual streak shown in the dashboard. For example:
- Dashboard shows: **3 days streak** ✅
- Leaderboard shows: **1 day streak** ❌

Other stats (success rate, total habits, completions) were updating correctly, but the current streak was stuck at low values.

## Root Cause

The problem was in the `ProfileStatsUpdater.kt` file, specifically in the `calculateHabitStreak()` function.

### Old (Broken) Implementation
```kotlin
private fun calculateHabitStreak(habit: Habit, today: LocalDate): Int {
    val lastCompleted = habit.lastCompletedDate ?: return 0
    
    // Simple streak calculation
    val daysSinceLastCompleted = java.time.temporal.ChronoUnit.DAYS.between(lastCompleted, today).toInt()
    
    return when {
        daysSinceLastCompleted == 0 -> 1  // Completed today
        daysSinceLastCompleted == 1 -> 2  // Completed yesterday (simplified)
        else -> 0  // Streak broken
    }
}
```

**Problems:**
1. Only used `lastCompletedDate` instead of full completion history
2. Could only return 0, 1, or 2 - never calculated actual long streaks
3. Didn't account for consecutive completions over multiple days
4. No penalty system for missed days

### New (Fixed) Implementation
```kotlin
private suspend fun calculateHabitStreak(habit: Habit, today: LocalDate): Int {
    // Fetch the completion history for this habit
    val completions = try {
        habitRepository.getHabitCompletions(habit.id)
    } catch (e: Exception) {
        Log.e(TAG, "calculateHabitStreak: Error fetching completions for habit ${habit.id}", e)
        return 0
    }
    
    if (completions.isEmpty()) return 0
    
    val completedDates = completions.map { it.completedDate }.toSet()
    val yesterday = today.minusDays(1)
    
    // Check if there's a recent completion (today or yesterday)
    val hasRecentCompletion = today in completedDates || yesterday in completedDates
    
    if (!hasRecentCompletion) {
        // No recent completion - apply penalty system
        val mostRecent = completedDates.max()
        val daysSinceLastCompletion = java.time.temporal.ChronoUnit.DAYS.between(mostRecent, today).toInt()
        
        // Calculate what the streak would have been
        var consecutiveStreak = 1
        var cursor = mostRecent.minusDays(1)
        while (cursor in completedDates) {
            consecutiveStreak++
            cursor = cursor.minusDays(1)
        }
        
        // Apply penalty: -1 for each missed day, but don't go below 0
        val penalty = daysSinceLastCompletion - 1
        return maxOf(0, consecutiveStreak - penalty)
    }
    
    // Has recent completion - calculate normal streak from most recent date
    val startDate = if (today in completedDates) today else yesterday
    var streak = 1
    var cursor = startDate.minusDays(1)
    while (cursor in completedDates) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}
```

**Improvements:**
1. ✅ Fetches full completion history from `habitRepository.getHabitCompletions()`
2. ✅ Properly calculates consecutive day streaks
3. ✅ Matches the same algorithm used in the dashboard (`HabitViewModel`)
4. ✅ Includes penalty system for missed days
5. ✅ Can calculate streaks of any length (3, 7, 30+ days)

## Changes Made

### File Modified
- `app/src/main/java/com/example/habittracker/ui/social/ProfileStatsUpdater.kt`

### Key Changes
1. Changed `calculateHabitStreak()` from synchronous to `suspend` function
2. Added fetching of completion history via `habitRepository.getHabitCompletions()`
3. Implemented full streak calculation algorithm
4. Changed `calculateCurrentStreak()` to also be a `suspend` function to call the updated `calculateHabitStreak()`

## Testing Verification

After this fix, verify:

1. **Dashboard Streak**: Open dashboard and check current streak
2. **Leaderboard Streak**: Open leaderboard and verify the streak matches
3. **Complete a Habit**: Complete a habit and check both screens update consistently
4. **Multi-day Streaks**: If you have a 3+ day streak, it should show the same number on both screens

### Example Scenarios

| Scenario | Dashboard | Leaderboard (Before) | Leaderboard (After) |
|----------|-----------|---------------------|-------------------|
| Completed 3 days in a row | 3 days ✅ | 1 day ❌ | 3 days ✅ |
| Completed 7 days in a row | 7 days ✅ | 2 days ❌ | 7 days ✅ |
| Completed 30 days in a row | 30 days ✅ | 1 day ❌ | 30 days ✅ |

## Technical Notes

- The streak calculation now properly queries the `HabitCompletion` table
- This ensures the leaderboard reflects the same data as the dashboard
- The algorithm accounts for:
  - ✅ Consecutive daily completions
  - ✅ Recent completions (today or yesterday)
  - ✅ Penalty for missed days
  - ✅ Multiple habits (shows longest streak)

## Related Files

- `ProfileStatsUpdater.kt` - Contains the fix
- `HabitViewModel.kt` - Contains the original correct algorithm (lines 572-608)
- `SocialViewModel.kt` - Uses ProfileStatsUpdater to update leaderboard stats
- `LeaderboardScreen.kt` - Displays the corrected streak values

---

**Status**: ✅ Fixed
**Date**: October 3, 2025
**Impact**: Current streak now accurately reflects user's actual habit completion history in the leaderboard
