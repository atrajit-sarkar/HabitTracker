# ProfileStatsUpdater Architecture Fix

## Issue
During compilation, Hilt threw an error:
```
Injection of an @HiltViewModel class is prohibited since it does not create a ViewModel instance correctly.
Injected ViewModel: com.example.habittracker.ui.social.ProfileStatsUpdater
```

## Root Cause
`ProfileStatsUpdater` was incorrectly designed as a `@HiltViewModel` (which extends `ViewModel`), but we were trying to inject it into another ViewModel (`HabitViewModel`). 

**Hilt's rule**: ViewModels cannot be injected into other ViewModels directly. They must be accessed via `ViewModelProvider` from the UI layer.

## Solution
Converted `ProfileStatsUpdater` from a `@HiltViewModel` to a `@Singleton` helper class.

## Changes Made

### Before ❌
```kotlin
@HiltViewModel
class ProfileStatsUpdater @Inject constructor(
    private val friendRepository: FriendRepository
) : ViewModel() {

    fun updateUserStats(user: User, habits: List<Habit>) {
        viewModelScope.launch {  // ❌ Using ViewModel's scope
            val stats = calculateStats(habits)
            friendRepository.updateUserPublicProfile(...)
        }
    }
}
```

### After ✅
```kotlin
@Singleton
class ProfileStatsUpdater @Inject constructor(
    private val friendRepository: FriendRepository
) {

    suspend fun updateUserStats(user: User, habits: List<Habit>) {
        // ✅ Now a regular suspend function
        val stats = calculateStats(habits)
        friendRepository.updateUserPublicProfile(...)
    }
}
```

## Key Architectural Changes

1. **Removed `@HiltViewModel`** → Changed to `@Singleton`
   - No longer extends `ViewModel`
   - Can be injected anywhere (ViewModels, Repositories, etc.)

2. **Removed `viewModelScope`** → Changed to `suspend` function
   - Caller now controls the coroutine scope
   - More flexible and testable

3. **Removed ViewModel imports**:
   ```kotlin
   // Removed:
   import androidx.lifecycle.ViewModel
   import androidx.lifecycle.viewModelScope
   import dagger.hilt.android.lifecycle.HiltViewModel
   
   // Added:
   import javax.inject.Singleton
   ```

4. **Removed extension function** (no longer needed):
   ```kotlin
   // Removed this helper function
   fun ProfileStatsUpdater.updateIfUserLoggedIn(...)
   ```

## Why This Design is Better

### 1. Proper Dependency Hierarchy
```
UI Layer (Composables)
    ↓
ViewModel Layer (HabitViewModel)
    ↓
Helper/Service Layer (ProfileStatsUpdater) ✅
    ↓
Repository Layer (FriendRepository)
    ↓
Data Layer (Firestore)
```

ViewModels should only be at the top of the hierarchy, not injected into each other.

### 2. Reusability
`ProfileStatsUpdater` can now be injected into:
- ✅ `HabitViewModel`
- ✅ `SocialViewModel`
- ✅ Any other ViewModel
- ✅ Repositories
- ✅ Background workers

### 3. Testability
```kotlin
// Easy to test - just pass mock dependencies
@Test
fun testStatsCalculation() {
    val mockRepo = mock<FriendRepository>()
    val updater = ProfileStatsUpdater(mockRepo)
    
    runTest {
        updater.updateUserStats(testUser, testHabits)
        verify(mockRepo).updateUserPublicProfile(...)
    }
}
```

### 4. Scope Control
Caller controls when and how coroutines run:
```kotlin
// In HabitViewModel
viewModelScope.launch(Dispatchers.IO) {
    profileStatsUpdater.updateUserStats(user, habits)
}
```

## Impact on HabitViewModel

No changes needed in `HabitViewModel`! The injection and usage remain exactly the same:

```kotlin
@HiltViewModel
class HabitViewModel @Inject constructor(
    ...
    private val profileStatsUpdater: ProfileStatsUpdater  // ✅ Still works
) : ViewModel() {

    private suspend fun updateUserStatsAsync() {
        val user = currentUser ?: return
        val habits = _uiState.value.habits.map { ui -> getHabitById(ui.id) }
        profileStatsUpdater.updateUserStats(user, habits)  // ✅ Still works
    }
}
```

## Build Status
✅ **Compilation successful** - Hilt error resolved!

## Design Pattern
This follows the **Helper/Service Pattern**:
- Not a ViewModel (no UI state)
- Not a Repository (doesn't own data)
- Helper class that coordinates between layers
- Singleton scope for efficiency

## Related Files Modified
- ✅ `ProfileStatsUpdater.kt` - Converted to @Singleton helper class

## Summary
Converted `ProfileStatsUpdater` from a misused `@HiltViewModel` to a proper `@Singleton` helper class, fixing the Hilt dependency injection error while maintaining all functionality.
