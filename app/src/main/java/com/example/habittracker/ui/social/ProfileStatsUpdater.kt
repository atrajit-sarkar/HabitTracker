package it.atraj.habittracker.ui.social

import android.util.Log
import it.atraj.habittracker.auth.User
import it.atraj.habittracker.data.HabitRepository
import it.atraj.habittracker.data.firestore.FriendRepository
import it.atraj.habittracker.data.local.Habit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to update user's public profile stats.
 * Call this when habits are completed or modified.
 */
@Singleton
class ProfileStatsUpdater @Inject constructor(
    private val friendRepository: FriendRepository,
    private val habitRepository: HabitRepository
) {
    companion object {
        private const val TAG = "ProfileStatsUpdater"
    }

    /**
     * Update user's public profile with current stats.
     * Fetches habits directly from Firestore to ensure fresh data.
     * Should be called:
     * - When a habit is completed
     * - When viewing the profile screen
     * - After loading the dashboard
     */
    suspend fun updateUserStats(user: User) {
        Log.d(TAG, "updateUserStats: Starting for user ${user.uid}")
        
        // Fetch habits directly from Firestore to ensure we have the latest data
        val habits = try {
            habitRepository.observeHabits().first()
        } catch (e: Exception) {
            Log.e(TAG, "updateUserStats: Error fetching habits from Firestore", e)
            emptyList()
        }
        
        Log.d(TAG, "updateUserStats: Fetched ${habits.size} habits from Firestore")
        
        val stats = calculateStats(habits)
        
        Log.d(TAG, "updateUserStats: Calculated stats - SR: ${stats.successRate}%, Habits: ${stats.totalHabits}, Completions: ${stats.totalCompletions}, Streak: ${stats.currentStreak}, Score: ${stats.leaderboardScore}")
        
        friendRepository.updateUserPublicProfile(
            userId = user.uid,
            email = user.email ?: "",
            displayName = user.effectiveDisplayName,
            photoUrl = user.photoUrl,
            customAvatar = user.customAvatar ?: "😊",
            successRate = stats.successRate,
            totalHabits = stats.totalHabits,
            totalCompletions = stats.totalCompletions,
            currentStreak = stats.currentStreak,
            leaderboardScore = stats.leaderboardScore
        )
        
        Log.d(TAG, "updateUserStats: Profile updated in Firestore")
    }
    
    /**
     * Legacy method for backward compatibility when habits are already available.
     */
    suspend fun updateUserStats(
        user: User,
        habits: List<Habit>
    ) {
        Log.d(TAG, "updateUserStats (legacy): Starting for user ${user.uid}, ${habits.size} habits provided")
        
        val stats = calculateStats(habits)
        
        Log.d(TAG, "updateUserStats (legacy): Calculated stats - SR: ${stats.successRate}%, Habits: ${stats.totalHabits}, Completions: ${stats.totalCompletions}, Streak: ${stats.currentStreak}, Score: ${stats.leaderboardScore}")
        
        friendRepository.updateUserPublicProfile(
            userId = user.uid,
            email = user.email ?: "",
            displayName = user.effectiveDisplayName,
            photoUrl = user.photoUrl,
            customAvatar = user.customAvatar ?: "😊",
            successRate = stats.successRate,
            totalHabits = stats.totalHabits,
            totalCompletions = stats.totalCompletions,
            currentStreak = stats.currentStreak,
            leaderboardScore = stats.leaderboardScore
        )
        
        Log.d(TAG, "updateUserStats (legacy): Profile updated in Firestore")
    }

    private suspend fun calculateStats(habits: List<Habit>): UserStats {
        val activeHabits = habits.filter { !it.isDeleted }
        val totalHabits = activeHabits.size
        val today = LocalDate.now()
        
        // Calculate completions today
        val completedToday = activeHabits.count { it.lastCompletedDate == today }
        
        // Calculate total completions (approximate - just count habits completed today)
        // For accurate count, you'd need to query all completions from the repository
        val totalCompletions = completedToday
        
        // Calculate success rate (percentage of habits completed today)
        val successRate = if (totalHabits > 0) {
            (completedToday * 100) / totalHabits
        } else {
            0
        }
        
        // Calculate current streak (longest consecutive days)
        val currentStreak = calculateCurrentStreak(activeHabits, today)
        
        // Calculate leaderboard score (weighted cumulative score)
        // Formula: (Success Rate × 5) + (Total Habits × 3) + (Current Streak × 10) + (Total Completions × 2)
        // - Success Rate (0-100) × 5 = 0-500 points (consistency is very important)
        // - Total Habits × 3 = encourages maintaining multiple habits
        // - Current Streak × 10 = heavily rewards daily consistency (most important)
        // - Total Completions × 2 = rewards overall dedication
        val leaderboardScore = (successRate * 5) + (totalHabits * 3) + (currentStreak * 10) + (totalCompletions * 2)
        
        return UserStats(
            totalHabits = totalHabits,
            totalCompletions = totalCompletions,
            successRate = successRate,
            currentStreak = currentStreak,
            leaderboardScore = leaderboardScore
        )
    }

    private suspend fun calculateCurrentStreak(habits: List<Habit>, today: LocalDate): Int {
        if (habits.isEmpty()) return 0
        
        // Find the longest current streak among all habits
        var maxStreak = 0
        
        habits.forEach { habit ->
            val streak = calculateHabitStreak(habit, today)
            if (streak > maxStreak) {
                maxStreak = streak
            }
        }
        
        return maxStreak
    }

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
            // Find the most recent completion
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
            val penalty = daysSinceLastCompletion - 1 // -1 because first day after is acceptable
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

    private data class UserStats(
        val totalHabits: Int,
        val totalCompletions: Int,
        val successRate: Int,
        val currentStreak: Int,
        val leaderboardScore: Int
    )
}
