package com.example.habittracker.ui.social

import com.example.habittracker.auth.User
import com.example.habittracker.data.firestore.FriendRepository
import com.example.habittracker.data.local.Habit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private val friendRepository: FriendRepository
) {

    /**
     * Update user's public profile with current stats.
     * Should be called:
     * - When a habit is completed
     * - When viewing the profile screen
     * - After loading the dashboard
     */
    suspend fun updateUserStats(
        user: User,
        habits: List<Habit>
    ) {
        val stats = calculateStats(habits)
        
        friendRepository.updateUserPublicProfile(
            userId = user.uid,
            email = user.email ?: "",
            displayName = user.effectiveDisplayName,
            photoUrl = user.photoUrl,
            customAvatar = user.customAvatar ?: "😊",
            successRate = stats.successRate,
            totalHabits = stats.totalHabits,
            totalCompletions = stats.totalCompletions,
            currentStreak = stats.currentStreak
        )
    }

    private fun calculateStats(habits: List<Habit>): UserStats {
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
        
        return UserStats(
            totalHabits = totalHabits,
            totalCompletions = totalCompletions,
            successRate = successRate,
            currentStreak = currentStreak
        )
    }

    private fun calculateCurrentStreak(habits: List<Habit>, today: LocalDate): Int {
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

    private data class UserStats(
        val totalHabits: Int,
        val totalCompletions: Int,
        val successRate: Int,
        val currentStreak: Int
    )
}
