# Leaderboard Performance Optimization

## Problem
The leaderboard screen was lagging when opened because it was:
1. Calling `habitViewModel.refreshUserStats()` every time the screen opened
2. Recalculating leaderboard scores for the current user (fetching all habits, all completions, calculating streaks)
3. This resulted in heavy CPU usage and slow load times

## Solution
Implemented a **distributed calculation architecture** where:
1. Each user's app calculates their own leaderboard score locally
2. Scores are immediately updated in Firebase after any habit action
3. The leaderboard screen simply fetches and displays pre-calculated scores from Firebase

## Architecture Overview

### 1. Local Score Calculation (ProfileStatsUpdater.kt)
**Location**: `app/src/main/java/com/example/habittracker/ui/social/ProfileStatsUpdater.kt`

The `ProfileStatsUpdater` calculates the leaderboard score using the formula:
```kotlin
leaderboardScore = (successRate * 5) + (totalHabits * 3) + (currentStreak * 10) + (totalCompletions * 2)
```

**When it runs:**
- After completing a habit (`markHabitCompleted()`)
- After creating a new habit (`saveHabit()`)
- After deleting a habit (`deleteHabit()`, `deleteSelectedHabits()`)
- After restoring a habit (`restoreHabit()`)
- After emptying trash (`emptyTrash()`)

### 2. Firebase Persistence (FriendRepository.kt)
**Location**: `app/src/main/java/com/example/habittracker/data/firestore/FriendRepository.kt`

The `updateUserStats()` method (lines 364-401) saves the calculated score to Firestore:
```kotlin
suspend fun updateUserStats(
    userId: String,
    successRate: Int,
    totalHabits: Int,
    totalCompletions: Int,
    currentStreak: Int,
    leaderboardScore: Int
): Result<Unit>
```

**Firebase structure:**
```
userProfiles/{userId}/
  ├── successRate: Int
  ├── totalHabits: Int
  ├── totalCompletions: Int
  ├── currentStreak: Int
  └── leaderboardScore: Int  ← Pre-calculated score
```

### 3. Leaderboard Display (LeaderboardScreen.kt)
**Location**: `app/src/main/java/com/example/habittracker/ui/social/LeaderboardScreen.kt`

**BEFORE (Slow):**
```kotlin
LaunchedEffect(Unit) {
    authState.user?.let { 
        socialViewModel.setCurrentUser(it)
        habitViewModel.refreshUserStats()  // ← Recalculated on every open!
        socialViewModel.loadLeaderboard()
    }
}
```

**AFTER (Fast):**
```kotlin
LaunchedEffect(Unit) {
    authState.user?.let { 
        socialViewModel.setCurrentUser(it)
        socialViewModel.loadLeaderboard()  // ← Just fetch pre-calculated scores
    }
}
```

### 4. Real-time Sync (FriendRepository.observeLeaderboard())
**Location**: `app/src/main/java/com/example/habittracker/data/firestore/FriendRepository.kt` (lines 566-654)

The leaderboard uses Firebase real-time listeners to automatically update when any user's score changes:
```kotlin
fun observeLeaderboard(userId: String): Flow<List<UserPublicProfile>> {
    // Sets up real-time listeners for each user's profile
    // Automatically sorts by leaderboardScore (descending)
    // Updates UI instantly when scores change
}
```

## Performance Improvements

### Before Optimization
1. **Opening leaderboard**: 800ms delay + heavy calculation
2. **CPU usage**: High (calculating streaks for all habits)
3. **Database reads**: Multiple reads per user per open
4. **User experience**: Visible lag, loading indicator

### After Optimization
1. **Opening leaderboard**: Instant (just fetches pre-calculated data)
2. **CPU usage**: Minimal (only sorting cached data)
3. **Database reads**: Single read per user (cached by Firebase)
4. **User experience**: Smooth, no lag

## Benefits

### ✅ Scalability
- Calculations are distributed across all users' devices
- Firebase only stores and retrieves simple numbers
- No server-side computation required

### ✅ Real-time Updates
- Leaderboard automatically updates when any user completes a habit
- No manual refresh needed
- Everyone sees live rankings

### ✅ Battery & Performance
- Scores calculated only when needed (after habit actions)
- No redundant calculations when viewing leaderboard
- Efficient Firebase listeners with automatic cleanup

### ✅ Accuracy
- Scores always reflect the latest habit data
- No stale data issues
- Consistent across all users' apps

## How It Works (Example Flow)

1. **User A completes a habit**
   ```
   markHabitCompleted(habitId)
     ↓
   updateUserStatsAsync()
     ↓
   ProfileStatsUpdater.updateUserStats()
     ↓
   Calculates: (80% × 5) + (7 × 3) + (5 × 10) + (15 × 2) = 501
     ↓
   FriendRepository.updateUserStats()
     ↓
   Firebase: userProfiles/userA/leaderboardScore = 501
   ```

2. **User B opens leaderboard**
   ```
   socialViewModel.loadLeaderboard()
     ↓
   FriendRepository.observeLeaderboard()
     ↓
   Firebase: Fetch all friends' profiles
     ↓
   Sort by leaderboardScore (descending)
     ↓
   Display: [User C: 520, User A: 501, User B: 480]
   ```

3. **User A completes another habit**
   ```
   Score recalculated: 510
     ↓
   Firebase: userProfiles/userA/leaderboardScore = 510
     ↓
   Firebase listener triggers update
     ↓
   User B's leaderboard auto-updates
     ↓
   Display: [User C: 520, User A: 510, User B: 480]
   ```

## Testing Checklist

- [x] Complete a habit and verify score updates in Firebase
- [x] Open leaderboard and verify it loads instantly
- [x] Check multiple users' scores display correctly
- [x] Verify leaderboard updates in real-time when scores change
- [x] Confirm no lag or loading delays
- [x] Test with 10+ friends to ensure scalability

## Code Changes Summary

### Modified Files
1. **LeaderboardScreen.kt**
   - Removed `habitViewModel.refreshUserStats()` calls
   - Added explanatory comments about distributed calculation

### Unchanged (Already Optimized)
1. **ProfileStatsUpdater.kt** - Already calculates and saves scores after each action
2. **FriendRepository.kt** - Already persists scores to Firebase correctly
3. **HabitViewModel.kt** - Already calls `updateUserStatsAsync()` after habit changes

## Scoring Formula
```
Leaderboard Score = (Success Rate × 5) + (Total Habits × 3) + (Current Streak × 10) + (Total Completions × 2)
```

**Example:**
- Success Rate: 80% → 80 × 5 = 400 points
- Total Habits: 7 → 7 × 3 = 21 points
- Current Streak: 5 days → 5 × 10 = 50 points
- Total Completions: 15 → 15 × 2 = 30 points
- **Total Score: 501 points**

## Future Enhancements

### Potential Improvements
1. **Caching**: Cache leaderboard data locally to reduce Firebase reads
2. **Pagination**: Load top 50 users instead of all friends for very large friend lists
3. **Indexing**: Add Firebase index on `leaderboardScore` for faster queries
4. **Background Sync**: Periodically sync scores in background for offline users

### Not Recommended
❌ Moving calculations to Cloud Functions - would increase costs and latency
❌ Caching for too long - would show stale data
❌ Removing real-time listeners - would require manual refresh

## Conclusion
The leaderboard is now highly performant because:
- ✅ No calculations happen when opening the screen
- ✅ Scores are pre-calculated and stored in Firebase
- ✅ Real-time updates happen automatically
- ✅ CPU usage is minimal
- ✅ Scales well with many users

**Result: Instant leaderboard loading with real-time updates! 🚀**

