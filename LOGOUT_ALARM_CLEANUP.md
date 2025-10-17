# Logout Alarm Cleanup Implementation

## Overview
When a user logs out, all scheduled alarms are now automatically cancelled to prevent notifications for habits they can no longer access.

## Implementation Details

### Location
The alarm cleanup is implemented in `AuthViewModel.signOut()` method.

### Why ViewModel Level?
- **Avoided Circular Dependency**: `AuthRepository` depends on `FirestoreHabitRepository`, which already depends on `AuthRepository`. Injecting `HabitRepository` into `AuthRepository` would create a circular dependency.
- **Clean Separation**: ViewModels are the appropriate place for orchestrating multiple repository operations.

### Code Changes

#### File: `app/src/main/java/com/example/habittracker/auth/ui/AuthViewModel.kt`

```kotlin
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleSignInHelper: GoogleSignInHelper,
    private val habitRepository: HabitRepository,        // Added
    private val reminderScheduler: HabitReminderScheduler // Added
) : ViewModel()

fun signOut() {
    viewModelScope.launch {
        // Cancel all scheduled alarms and delete notification channels before signing out
        withContext(Dispatchers.IO) {
            try {
                val habits = habitRepository.getAllHabits()
                val habitIds = habits.map { it.id }
                var cancelledCount = 0
                
                // Cancel all alarms
                habits.forEach { habit ->
                    try {
                        reminderScheduler.cancel(habit.id)
                        cancelledCount++
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Failed to cancel alarm for habit ${habit.id}", e)
                    }
                }
                Log.d("AuthViewModel", "Cancelled $cancelledCount alarms before logout")
                
                // Delete all notification channels
                if (habitIds.isNotEmpty()) {
                    HabitReminderService.deleteMultipleHabitChannels(context, habitIds)
                    Log.d("AuthViewModel", "Deleted ${habitIds.size} notification channels before logout")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error cleaning up alarms/channels during logout", e)
            }
        }
        
        authRepository.signOut()
        googleSignInHelper.signOut()
    }
}
```

## Complete Alarm Lifecycle Coverage

Now all scenarios properly handle alarm rescheduling:

### System Events
1. ✅ **Device Reboot** - `BootReceiver` reschedules all active habits
2. ✅ **App Update/Reinstall** - `BootReceiver` handles `MY_PACKAGE_REPLACED`
3. ✅ **Time/Timezone Changes** - `TimeChangeReceiver` reschedules all active habits

### User Actions
4. ✅ **Create/Edit Habit** - Schedules or cancels based on reminder settings
5. ✅ **Toggle Reminder** - Schedules when enabled, cancels when disabled
6. ✅ **Delete Habit** - Cancels alarm immediately
7. ✅ **Restore Habit** - Reschedules with fresh data if reminder enabled
8. ✅ **Permanent Delete** - Cancels alarm before deletion
9. ✅ **Logout** - Cancels all alarms (NEW)

## Benefits

1. **Clean State**: No orphaned alarms or notification channels after logout
2. **Privacy**: User A won't receive notifications for User B's habits
3. **Resource Management**: Prevents unnecessary alarm wakeups
4. **Better UX**: Prevents confusion from stale notifications
5. **Clean System Settings**: All habit notification channels are removed from app settings
6. **Automatic Rescheduling**: When user logs back in, alarms and channels are automatically recreated

## Testing

To verify:
1. Login to Account A
2. Create habits with reminders enabled
3. Verify alarms are scheduled (check system alarm settings)
4. Check app notification settings - you should see individual channels for each habit
5. Logout
6. Verify in logs:
   - "Cancelled X alarms before logout"
   - "Deleted X notification channels before logout"
7. Check app notification settings - all habit channels should be gone
8. Login to Account A again
9. Open the app - `observeHabits()` flow triggers and habits are visible
10. Alarms and channels are automatically recreated on app startup

## Notes

- Both alarm cancellation and channel deletion happen **before** the actual Firebase signout
- Errors in individual alarm/channel cleanup don't block the logout process
- All operations are logged for debugging
- No circular dependency issues - properly architected
- Context is injected using `@ApplicationContext` annotation
- Channels are recreated automatically when user logs back in via `syncAllHabitChannels()`
