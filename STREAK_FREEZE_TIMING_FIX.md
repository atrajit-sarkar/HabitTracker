# Streak Freeze & Grace Period - 24-Hour Timing Fix

## Problem Identified

Previously, when a new day started (midnight), the app would immediately:
1. Check if the previous day was missed
2. Apply grace period or streak freeze **instantly at midnight**
3. Deduct freeze days from the user's pool even though the user still had the entire current day to complete the habit

### Example of the Problem:
- User completes habit on Monday
- Tuesday starts at 12:00 AM
- App immediately applies grace/freeze for Monday being "missed"
- User's streak freeze is deducted
- User completes the habit at 10:00 AM on Tuesday
- **Problem**: Freeze was wasted even though user completed within 24 hours

## Solution Implemented

### 1. **Streak Calculation Logic** (`StreakCalculator.kt`)

Updated `calculateStreak()` to differentiate between:
- **Completed today** (daysSinceLastCompletion == 0): Full streak + diamonds
- **Last completion yesterday** (daysSinceLastCompletion == 1): Maintain streak, no penalties, no freeze used
- **Missed days** (daysSinceLastCompletion >= 2): Apply grace/freeze only for truly past days

```kotlin
// If last completion was yesterday, streak is maintained but no new diamonds
// (user still has today to complete and continue the streak)
if (daysSinceLastCompletion == 1) {
    val currentStreak = calculateCurrentStreak(sortedCompletions)
    
    return StreakCalculationResult(
        newStreak = currentStreak,
        diamondsEarned = 0,
        freezeDaysUsed = 0,
        graceUsed = false
    )
}
```

### 2. **Grace Day Visual Indicator** (`isGraceDay()`)

Updated to only show grace indicator on **PAST dates**, not on today:

```kotlin
fun isGraceDay(...): Boolean {
    // Don't show grace on today - user still has time to complete
    val today = LocalDate.now()
    if (date >= today) return false
    
    // ... rest of logic
}
```

### 3. **Freeze Day Visual Indicator** (`isFreezeDay()`)

Updated to only show freeze indicator on **PAST dates**, not on today:

```kotlin
fun isFreezeDay(...): Boolean {
    // Don't show freeze on today - user still has time to complete
    val today = LocalDate.now()
    if (date >= today) return false
    
    // ... rest of logic
}
```

### 4. **Calendar UI Display** (`HabitDetailsScreen.kt`)

Updated calendar day determination to exclude today from grace visual:

```kotlin
val isGraceDay = date?.let { d ->
    if (d !in completedDates && d >= habitCreationDate && d < today) {
        // Only show grace on past dates
        // ... check logic
    } else false
} ?: false
```

## How It Works Now

### Scenario 1: User Completes on Time
- **Monday**: Complete habit ✅
- **Tuesday 12:00 AM**: New day starts
  - Last completion = yesterday (1 day ago)
  - No grace shown on Tuesday (still ongoing)
  - No freeze deducted
  - Streak maintained
- **Tuesday 10:00 AM**: User completes habit ✅
  - Streak increases
  - No freeze/grace needed

### Scenario 2: User Misses Monday, Completes Tuesday
- **Monday**: Complete habit ✅
- **Tuesday**: User forgets to complete
- **Wednesday 12:00 AM**: New day starts
  - Last completion = 2 days ago (Monday)
  - **Tuesday now shows grace indicator** (icy blue border) - it's now in the past
  - Grace is applied to Tuesday (first missed day)
  - No freeze deducted yet
  - Wednesday is still ongoing - no indicators
- **Wednesday 10:00 AM**: User completes habit ✅
  - Streak maintained (grace protected Tuesday)
  - No freeze used

### Scenario 3: User Misses Multiple Days
- **Monday**: Complete habit ✅
- **Tuesday**: Missed
- **Wednesday**: Missed  
- **Thursday 12:00 AM**: New day starts
  - Last completion = 3 days ago (Monday)
  - **Tuesday shows grace** (icy blue border)
  - **Wednesday shows freeze** (if available)
  - 1 freeze day is deducted from pool
  - Thursday is still ongoing - no indicators
- **Thursday 2:00 PM**: User completes habit ✅
  - Streak maintained (grace + freeze protected)

## Key Benefits

✅ **No Premature Deduction**: Freeze days are never deducted on the current day
✅ **24-Hour Grace**: Users get the full day to complete before penalties apply
✅ **Clear Visual Feedback**: Grace/freeze indicators only appear on completed past days
✅ **Fair System**: Users can complete habits late in the day without losing streak protection
✅ **Predictable Behavior**: Grace/freeze only applies after the 24-hour period ends at midnight

## Technical Details

### When Streak Recalculation Happens
- On app open (first load)
- When date changes (detected automatically)
- Called by `recalculateAllStreaks()` in `HabitViewModel`

### Missed Days Calculation
```kotlin
val missedDays = daysSinceLastCompletion - 1 // Don't count today
```
This ensures today is never counted as a "missed day" until tomorrow arrives.

### Visual Indicators Timeline
- **During the day**: No grace/freeze indicators shown
- **After midnight**: Previous day's grace/freeze status becomes visible
- **Grace indicator**: Light icy blue gradient border (🧊 Sky Blue → Powder Blue)
  - Color: `#87CEEB` / `#B0E0E6`
  - Border width: 3dp
- **Freeze indicator**: Frosty cyan/turquoise gradient border (❄️ Dark Turquoise → Light Cyan)
  - Color: `#00CED1` / `#E0FFFF`
  - Border width: 3dp
  - Distinguishable from grace by darker, more vibrant cyan tone

## Files Modified

1. `app/src/main/java/com/example/habittracker/data/StreakCalculator.kt`
   - Updated `calculateStreak()` logic to handle yesterday separately
   - Modified `isGraceDay()` to exclude today
   - Modified `isFreezeDay()` to exclude today

2. `app/src/main/java/com/example/habittracker/ui/HabitDetailsScreen.kt`
   - **HabitDetailsScreen**: Pass `userRewards` and `completions` to CalendarSection
   - **CalendarSection**: Accept and forward userRewards and completions to MonthCalendar
   - **MonthCalendar**: 
     - Accept userRewards and completions parameters
     - Calculate both grace and freeze days using StreakCalculator
     - Pass `isFreezeDay` to CalendarDay
   - **CalendarDay**:
     - Added `isFreezeDay` parameter
     - Added distinct frost border styling for freeze days (cyan/turquoise gradient)
     - Grace days: Light icy blue border
     - Freeze days: Darker cyan/turquoise border (distinguishable from grace)
     - Regular missed days: Red border (broken streak)

## Testing Checklist

- [ ] Complete habit on Day 1, verify streak = 1
- [ ] Start Day 2, verify no grace shown on Day 2
- [ ] Complete habit late on Day 2, verify streak = 2, no freeze used
- [ ] Miss Day 2 completely, start Day 3, verify grace shown on Day 2
- [ ] Complete on Day 3, verify streak maintained, no freeze used
- [ ] Miss Day 2 and Day 3, start Day 4, verify grace on Day 2, freeze on Day 3, 1 freeze deducted
- [ ] Complete on Day 4, verify streak maintained
- [ ] Miss multiple days with no freezes available, verify streak decreases correctly

## Related Documentation

- `✨ Habit Tracker – Streak & Streak Freeze System.md` - Original streak system design
- `STREAK_FREEZE_IMPLEMENTATION.md` - Full streak freeze implementation details
