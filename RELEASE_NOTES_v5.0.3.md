# Release Notes - Version 5.0.3

**Release Date:** October 12, 2025  
**Version Code:** 12  
**Version Name:** 5.0.3

---

## 🎯 What's New

### Fixed: Streak Calculation System
**Major improvement to how streaks are calculated throughout the app!**

#### Problem Solved:
Previously, the app used an inconsistent "hybrid" streak system that would:
- Reset your entire streak when you missed a day
- Then apply additional penalties on the already-reset value
- Result: Unfair double-penalty causing streaks to drop to 0 unexpectedly

#### New Behavior:
- **Pure Penalty System**: Missing a day now costs only -1 per day instead of resetting everything
- **Fair & Motivating**: Your streak survives small gaps and recovers gradually
- **Consistent**: Same calculation used everywhere (home, details, statistics, profile, leaderboard)

#### Example:
```
Your completions: Oct 5, 6, 7, 8, 10 (missed Oct 9)
Today: Oct 12

OLD SYSTEM:
❌ Streak resets to 1 (only Oct 10 counts)
❌ Then penalized for missing Oct 11-12
❌ Result: 0 days

NEW SYSTEM:
✅ Count all completions: 5 days
✅ Oct 9 gap: -1 penalty → 4 days
✅ Oct 11-12 gap: -1 penalty → 3 days
✅ Result: 3 days
```

---

## 🔧 Technical Changes

### Modified Files:
1. **HabitViewModel.kt**
   - Rewrote `calculateCurrentStreak()` function
   - Implements cumulative penalty-based system
   - Used by: Home screen, Habit details, Statistics

2. **ProfileStatsUpdater.kt**
   - Updated `calculateHabitStreak()` function
   - Ensures social features show consistent streaks
   - Used by: Profile, Leaderboard, Friends list

### Consistency:
✅ Home Screen - Habit cards  
✅ Habit Details - Individual statistics  
✅ Statistics Screen - Analytics & charts  
✅ Profile Screen - Your stats  
✅ Leaderboard - Rankings  
✅ Friends List - Friend profiles  
✅ Friend Profile - View friend's stats  
✅ Search Users - User preview cards  

**All screens now use the same streak calculation!**

---

## 📊 Impact

### For Users:
- **More Motivating**: Streaks don't reset completely after one miss
- **More Fair**: Penalties are proportional to missed days
- **More Transparent**: Easy to understand how streaks work
- **More Accurate**: Reflects your actual consistency better

### Grace Period:
- If you complete a habit **today OR yesterday**, no penalty is applied
- Only penalized if you miss both today and yesterday
- Each missed day costs -1 from your streak

---

## 🐛 Bug Fixes

- Fixed: Streak showing 0 despite multiple completions
- Fixed: Inconsistent streak values across different screens
- Fixed: Harsh streak resets after single missed days
- Fixed: Double-penalty issue in calculation logic

---

## 📝 Algorithm Details

The new penalty-based algorithm:

1. Start from your most recent completion
2. Work backwards through all your completions
3. For each completion: **+1 to streak**
4. For each gap: **-1 per missed day**
5. If streak goes negative: Reset to 1 and continue
6. Apply final penalty for days since last completion to today
7. Return the maximum of 0 or the final streak value

---

## 🚀 Upgrade Notes

### What You'll Notice:
- Your current streak may **increase** after this update (if you had gaps)
- Streaks are now consistent across all app screens
- Longest streak remains unchanged (still based on consecutive days)

### No Action Required:
- The update will automatically recalculate your streaks
- All historical data is preserved
- No data migration needed

---

## 📦 Build Information

- **Version Code**: 12 (was 11)
- **Version Name**: 5.0.3 (was 5.0.2)
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 36
- **Compile SDK**: 36

---

## 🔍 Related Documentation

See `STREAK_PENALTY_FIX.md` for:
- Detailed technical explanation
- Example calculations
- Code changes and algorithms

---

## 💬 Feedback

If you have any questions or feedback about the new streak calculation:
- Check the in-app Statistics screen for detailed breakdowns
- Review the documentation file `STREAK_PENALTY_FIX.md`
- The calculation is now transparent and consistent everywhere

---

**Thank you for using Habit Tracker!** 🎉

This update makes the app more fair, motivating, and consistent for everyone.
