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
import java.time.DayOfWeek
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
        
        Log.d(TAG, "updateUserStats: Calculated stats - SR: ${stats.successRate}%, Habits: ${stats.totalHabits}, Completions: ${stats.totalCompletions}, Streak: ${stats.currentStreak}, Score: ${stats.leaderboardScore}, ThisWeek: ${stats.completedThisWeek}")
        
        // Use updateUserStats instead of updateUserPublicProfile to avoid overwriting displayName/photoUrl
        friendRepository.updateUserStats(
            userId = user.uid,
            successRate = stats.successRate,
            totalHabits = stats.totalHabits,
            totalCompletions = stats.totalCompletions,
            currentStreak = stats.currentStreak,
            leaderboardScore = stats.leaderboardScore,
            completedThisWeek = stats.completedThisWeek
        )
        
        Log.d(TAG, "updateUserStats: Profile stats updated in Firestore")
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
        
        Log.d(TAG, "updateUserStats (legacy): Calculated stats - SR: ${stats.successRate}%, Habits: ${stats.totalHabits}, Completions: ${stats.totalCompletions}, Streak: ${stats.currentStreak}, Score: ${stats.leaderboardScore}, ThisWeek: ${stats.completedThisWeek}")
        
        // Use updateUserStats instead of updateUserPublicProfile to avoid overwriting displayName/photoUrl
        friendRepository.updateUserStats(
            userId = user.uid,
            successRate = stats.successRate,
            totalHabits = stats.totalHabits,
            totalCompletions = stats.totalCompletions,
            currentStreak = stats.currentStreak,
            leaderboardScore = stats.leaderboardScore,
            completedThisWeek = stats.completedThisWeek
        )
        
        Log.d(TAG, "updateUserStats (legacy): Profile stats updated in Firestore")
    }

    private suspend fun calculateStats(habits: List<Habit>): UserStats {
        val activeHabits = habits.filter { !it.isDeleted }
        val totalHabits = activeHabits.size
        val today = LocalDate.now()
        
        // Calculate completions today
        val completedToday = activeHabits.count { it.lastCompletedDate == today }
        
        // Fetch ALL completion data ONCE and cache it to avoid multiple database queries
        val completionCache = mutableMapOf<Long, List<it.atraj.habittracker.data.local.HabitCompletion>>()
        var totalCompletions = 0
        
        activeHabits.forEach { habit ->
            try {
                val completions = habitRepository.getHabitCompletions(habit.id)
                completionCache[habit.id] = completions
                totalCompletions += completions.size
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching completions for habit ${habit.id}: ${e.message}")
                completionCache[habit.id] = emptyList()
            }
        }
        
        // Calculate success rate (percentage of habits completed today)
        val successRate = if (totalHabits > 0) {
            (completedToday * 100) / totalHabits
        } else {
            0
        }
        
        // Calculate current streak - average of all habit streaks (floor)
        // (Each habit's streak is calculated by StreakCalculator with grace/freeze)
        val currentStreak = if (activeHabits.isNotEmpty()) {
            kotlin.math.floor(activeHabits.map { it.streak }.average()).toInt()
        } else {
            0
        }
        
        // Calculate this week's completions (Monday to Sunday) using cached data
        val completedThisWeek = calculateWeekCompletions(activeHabits, today, completionCache)
        
        // Calculate leaderboard score (weighted cumulative score)
        // Formula: (Success Rate × 5) + (Total Habits × 3) + (Current Streak × 10) + (Total Completions × 2)
        // - Success Rate (0-100) × 5 = 0-500 points (consistency is very important)
        // - Total Habits × 3 = encourages maintaining multiple habits
        // - Current Streak × 10 = heavily rewards daily consistency (most important)
        // - Total Completions × 2 = rewards overall dedication
        val leaderboardScore = (successRate * 5) + (totalHabits * 3) + (currentStreak * 10) + (totalCompletions * 2)
        
        Log.d(TAG, "calculateStats: Habits=$totalHabits, TodayCompleted=$completedToday, TotalCompletions=$totalCompletions, SuccessRate=$successRate%, Streak=$currentStreak, ThisWeek=$completedThisWeek, Score=$leaderboardScore")
        
        return UserStats(
            totalHabits = totalHabits,
            totalCompletions = totalCompletions,
            successRate = successRate,
            currentStreak = currentStreak,
            leaderboardScore = leaderboardScore,
            completedThisWeek = completedThisWeek
        )
    }

    // OLD FUNCTIONS REMOVED - Now using habit.streak from StreakCalculator

    private suspend fun calculateWeekCompletions(
        habits: List<Habit>, 
        today: LocalDate,
        completionCache: Map<Long, List<it.atraj.habittracker.data.local.HabitCompletion>>
    ): Int {
        if (habits.isEmpty()) return 0
        
        val startOfWeek = today.with(DayOfWeek.MONDAY)
        val endOfWeek = startOfWeek.plusDays(6) // Sunday
        
        Log.d(TAG, "calculateWeekCompletions: Week range $startOfWeek to $endOfWeek")
        
        var weekCompletions = 0
        habits.forEach { habit ->
            try {
                val completions = completionCache[habit.id] ?: emptyList()
                val thisWeekCount = completions.count { completion ->
                    !completion.completedDate.isBefore(startOfWeek) && 
                    !completion.completedDate.isAfter(endOfWeek)
                }
                weekCompletions += thisWeekCount
                Log.d(TAG, "calculateWeekCompletions: Habit '${habit.title}' = $thisWeekCount completions this week")
            } catch (e: Exception) {
                Log.e(TAG, "calculateWeekCompletions: Error for habit ${habit.id}", e)
            }
        }
        
        Log.d(TAG, "calculateWeekCompletions: Total = $weekCompletions")
        return weekCompletions
    }
    
    private data class UserStats(
        val totalHabits: Int,
        val totalCompletions: Int,
        val successRate: Int,
        val currentStreak: Int,
        val leaderboardScore: Int,
        val completedThisWeek: Int
    )
}
