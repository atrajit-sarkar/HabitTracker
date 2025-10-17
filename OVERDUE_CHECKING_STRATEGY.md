# Production-Ready Overdue Habit Checking Strategy

## Overview
The overdue checking system uses a **multi-layered approach** to ensure timely icon updates while preserving battery life.

## Checking Mechanisms

### 1. **App Launch Check** (Immediate)
- **When**: Every time the app opens (MainActivity.onResume)
- **Delay**: 5 seconds after app start
- **Purpose**: Update icon based on current overdue status
- **Skip Conditions**:
  - First launch (prevents false positives)
  - Icon recently changed (30-second window)

### 2. **Periodic Background Check** (Every 1 Hour)
- **Frequency**: Every 1 hour
- **Technology**: WorkManager PeriodicWorkRequest
- **Purpose**: Fallback mechanism to catch any missed updates
- **Battery Impact**: Low (1 check per hour)
- **Runs**: Even when app is closed

### 3. **Smart Scheduled Checks** (Habit-Specific)
- **When**: 5 minutes after each habit's due time
- **Limit**: Next 10 upcoming habits within 24 hours
- **Purpose**: Detect overdue status precisely when habits become overdue
- **Example**: 
  - Habit due at 8:00 PM → Check scheduled at 8:05 PM
  - Habit due at 10:30 AM → Check scheduled at 10:35 AM

### 4. **Immediate Check on Habit Completion**
- **When**: User marks habit as complete
- **Mechanism**: Broadcast receiver triggers icon update
- **Purpose**: Immediately remove angry icon when habit is completed
- **Cache**: Cleared to force fresh calculation

## Production-Ready Benefits

### ✅ **Battery Efficient**
- Hourly checks instead of every 2 minutes
- Smart checks only scheduled when needed
- WorkManager handles battery optimization automatically

### ✅ **Accurate & Timely**
- Checks precisely when habits become overdue
- Immediate updates when habits are completed
- No false positives on first launch

### ✅ **Handles Edge Cases**
- App restart during icon change
- Background checks when app is closed
- Device reboot (WorkManager persists)
- Firestore sync delays (5-second startup delay)

### ✅ **Scalable**
- Limits smart checks to 10 upcoming habits
- Only schedules checks within next 24 hours
- Cache mechanism prevents redundant calculations

## Check Frequency Summary

| Scenario | Frequency | Method |
|----------|-----------|--------|
| **App Open** | Once per app launch | In-app check (5s delay) |
| **Background** | Every 1 hour | WorkManager periodic |
| **Habit Due** | 5 min after due time | WorkManager one-time |
| **Habit Completed** | Immediate | Broadcast receiver |

## Overdue Detection Logic

### Daily Habits
- ✅ Completed today → Never overdue
- ❌ Not completed today + time passed → Overdue

### Weekly/Monthly/Yearly Habits
- ✅ Completed on scheduled date → Not overdue
- ❌ Scheduled date is today + time passed → Overdue
- ⏰ Scheduled date is not today → Not checked (skip)

## Icon States

1. **DEFAULT**: No overdue habits
2. **WARNING**: 2-3 hours overdue (yellow icon)
3. **CRITICAL_WARNING**: 4+ hours overdue (angry red icon)

## Error Handling

- **Worker Retry**: Failed checks automatically retry
- **Cache Fallback**: Uses cached data if fresh data unavailable
- **Graceful Degradation**: If background checks fail, app launch check still works

## Testing Checklist

- [ ] Icon updates immediately when opening app
- [ ] Icon updates after completing overdue habit
- [ ] Icon updates 5 minutes after habit due time (background)
- [ ] No false overdue detection on first launch
- [ ] Icon doesn't change during icon selection process
- [ ] Background checks work when app is closed
- [ ] Battery usage is acceptable (<1% per day)

## Future Improvements

1. **Adaptive Scheduling**: Adjust check frequency based on habit patterns
2. **Notification Integration**: Show notification when habit becomes overdue
3. **Statistics**: Track overdue frequency for insights
4. **User Preferences**: Allow users to customize check intervals
