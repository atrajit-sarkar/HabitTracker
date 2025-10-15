# Streak Calculation Fix - Pure Penalty System

## Date: October 12, 2025

## Problem
The streak calculation had an **inconsistent hybrid system** that:
1. Used consecutive day counting (gaps break streaks)
2. Then applied penalties on already-reset streaks
3. Result: **Double penalty** - users lost streaks unfairly

### Example Issue:
```
Completions: Oct 5, 6, 7, 8, 10 (Oct 9 missed)
Today: Oct 12

OLD BEHAVIOR:
- Oct 9 gap → Streak resets to 1 (only Oct 10 counts)
- Missing Oct 11-12 → Penalty of 1
- Result: 1 - 1 = 0 days ❌

EXPECTED BEHAVIOR:
- Build streak: 5 days (Oct 5, 6, 7, 8, 10)
- Oct 9 gap → Penalty -1 → 4 days
- Oct 11-12 gap → Penalty -1 → 3 days
- Result: 3 days ✓
```

---

## Solution
Implemented a **pure penalty-based system** where:
- Missed days cost -1 per day
- No hard resets at gaps
- Streaks can recover from gaps without starting over

---

## Files Modified

### 1. `HabitViewModel.kt`
**Location:** `app/src/main/java/com/example/habittracker/ui/HabitViewModel.kt`
**Function:** `calculateCurrentStreak(completedDates: Set<LocalDate>): Int`

**Changes:**
- Removed consecutive-only counting that reset at gaps
- Implemented cumulative streak building with gap penalties
- Each gap costs -1 per missed day instead of full reset
- If penalties make streak negative, starts fresh from that point

**Algorithm:**
```kotlin
1. Start from most recent completion
2. Work backwards through all completions
3. For each completion: +1 to streak
4. For each gap: -1 per day missed
5. If streak goes negative: reset to 1 and continue
6. Apply final penalty for days since last completion to today
7. Return max(0, finalStreak)
```

---

### 2. `ProfileStatsUpdater.kt`
**Location:** `app/src/main/java/com/example/habittracker/ui/social/ProfileStatsUpdater.kt`
**Function:** `calculateHabitStreak(habit: Habit, today: LocalDate, completions: List<HabitCompletion>): Int`

**Changes:**
- Updated to match HabitViewModel's new penalty system
- Ensures social features (leaderboard, friend profiles) show consistent streaks

---

### 3. `StatisticsCalculator.kt`
**Status:** ✅ No changes needed
**Reason:** Already uses `viewModel.getHabitProgress()` which calls the updated `calculateCurrentStreak()`

---

## Impact Across App

### ✅ Consistent Everywhere:
1. **Home Screen** - Habit cards show current streak
2. **Habit Details Screen** - Individual habit statistics
3. **Statistics Screen** - Overall analytics and comparisons
4. **Profile Screen** - User's own stats
5. **Friends List** - Friend profiles and stats
6. **Leaderboard** - Rankings based on streaks
7. **Friend Profile Screen** - Viewing friend's stats
8. **Search Users** - User preview cards

All screens now use the **same penalty-based calculation** ensuring consistency!

---

## Example Calculations

### Case 1: Single Gap
```
Completions: Oct 5, 6, 7, 8, 10
Today: Oct 10 (same day as last completion)

Process:
- Oct 10: +1 → 1
- Oct 8: gap of 1 (Oct 9 missing) → -1, then +1 → 1
- Oct 7: +1 → 2
- Oct 6: +1 → 3
- Oct 5: +1 → 4

Current gap penalty: 0 (completed today)
Final: 4 days ✓
```

### Case 2: Single Gap + Current Gap
```
Completions: Oct 5, 6, 7, 8, 10
Today: Oct 12

Process:
- Oct 10: +1 → 1
- Oct 8: gap of 1 → -1 + 1 → 1
- Oct 7: +1 → 2
- Oct 6: +1 → 3
- Oct 5: +1 → 4

Current gap penalty: 2 - 1 = 1
Final: 4 - 1 = 3 days ✓
```

### Case 3: Multiple Gaps
```
Completions: Oct 5, 6, 10, 11
Today: Oct 12

Process:
- Oct 11: +1 → 1
- Oct 10: +1 → 2
- Oct 6: gap of 3 (Oct 7, 8, 9 missing) → -3 + 1 → 0
  - Goes negative, reset to 1 and continue
- Oct 5: +1 → 2

Current gap penalty: 1 (Oct 12 - Oct 11) - 1 = 0
Final: 2 days ✓
```

---

## Grace Period
- If completed **today OR yesterday**, no penalty applied
- Only penalized if missed both today and yesterday
- Penalty formula: `daysSinceLastCompletion - 1`

---

## Benefits
1. ✅ Fair penalty system - only -1 per missed day
2. ✅ No harsh resets - streaks survive small gaps
3. ✅ Consistent across entire app
4. ✅ Motivating - users can recover from lapses
5. ✅ Transparent - easy to understand calculation

---

## Testing
To test with your current data:
- Completions: Oct 5, 6, 7, 8, 10
- Today: Oct 12
- **Expected current streak: 3 days**
- **Expected longest streak: 4 days**

Build and install the app to see the updated values!
