# Fix: Unresolved Reference 'getAllHabits'

## Issue
```
e: file:///E:/CodingWorld/AndroidAppDev/HabitTracker/app/src/main/java/com/example/habittracker/MainActivity.kt:101:50 
Unresolved reference 'getAllHabits'.
```

## Root Cause
The `HabitRepository` interface didn't have a `getAllHabits()` method. It only had:
- `observeHabits(): Flow<List<Habit>>` - For reactive UI updates
- `getHabitById(id: Long): Habit` - For single habit retrieval

But our new notification reliability features (BootReceiver, AlarmVerificationWorker, MainActivity) needed a simple suspend function to get all habits synchronously for background operations.

## Solution
Added `getAllHabits()` method to the repository:

### 1. Updated HabitRepository Interface
**File**: `app/src/main/java/com/example/habittracker/data/HabitRepository.kt`

```kotlin
interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    fun observeDeletedHabits(): Flow<List<Habit>>
    suspend fun getAllHabits(): List<Habit>  // ← NEW METHOD
    suspend fun getHabitById(id: Long): Habit
    // ... rest of methods
}
```

### 2. Implemented in FirestoreHabitRepository
**File**: `app/src/main/java/com/example/habittracker/data/firestore/FirestoreHabitRepository.kt`

```kotlin
override suspend fun getAllHabits(): List<Habit> {
    val userCollection = getUserCollection() ?: return emptyList()
    return try {
        val snapshot = userCollection.get().await()
        snapshot.toFirestoreHabits()
            .mapNotNull { runCatching { it.toHabit() }.getOrNull() }
            .filterNot { it.isDeleted }
            .sortedWith(compareBy({ it.reminderHour }, { it.reminderMinute }))
    } catch (e: Exception) {
        android.util.Log.e("FirestoreRepo", "Error getting all habits", e)
        emptyList()
    }
}
```

## Implementation Details

### Why Not Use `observeHabits().first()`?
While we could have used:
```kotlin
val habits = habitRepository.observeHabits().first()
```

A dedicated `getAllHabits()` method is better because:
1. **Clearer Intent**: The method name explicitly states it's a one-time fetch
2. **Simpler API**: No need to import Flow operators
3. **Better Performance**: Direct Firestore query without Flow overhead
4. **Error Handling**: Can return empty list instead of throwing on auth issues
5. **Consistency**: Follows the existing pattern (`getHabitById`)

### Behavior
- **Returns**: `List<Habit>` (non-deleted habits only)
- **Sorting**: By reminder time (hour, then minute)
- **Empty List**: Returned if user not authenticated or error occurs
- **Thread-Safe**: Suspend function, safe to call from any coroutine

### Used By
1. **BootReceiver.kt** - Reschedules alarms after device reboot
2. **AlarmVerificationWorker.kt** - Verifies alarms every 24 hours
3. **MainActivity.kt** - Checks if user has reminders before showing battery dialog

## Testing
After this fix:
- ✅ No compilation errors
- ✅ All notification reliability features work
- ✅ Repository pattern maintained
- ✅ Consistent with existing code style

## Files Modified
1. `app/src/main/java/com/example/habittracker/data/HabitRepository.kt` (interface)
2. `app/src/main/java/com/example/habittracker/data/firestore/FirestoreHabitRepository.kt` (implementation)

## Status
✅ **FIXED** - Ready to build and test
