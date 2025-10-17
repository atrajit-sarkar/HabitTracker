# Background Image Preload System

## Problem Solved

After clearing app cache, the first notification would show a fallback 📷 emoji instead of the custom habit image, because:
1. Cache cleared → No images in cache
2. Notification fires → Looks for cached image → Not found
3. Shows fallback (can't download in BroadcastReceiver)
4. User opens app → Images load → Next notification works

## Solution

**Proactive background image preloading** using WorkManager:

### When It Runs:
1. ✅ **App startup** - When habits are loaded
2. ✅ **After restore** - When habit is restored from trash
3. ✅ **Non-blocking** - Runs in background, doesn't freeze UI

### How It Works:

```kotlin
App Opens
    ↓
Habits Loaded
    ↓
scheduleImagePreload() called
    ↓
WorkManager enqueues HabitImagePreloadWorker
    ↓
Worker runs in background (IO thread)
    ↓
Fetches all habits with custom images
    ↓
Downloads each image with stable cache key
    ↓
Images cached: "habit_avatar_{habitId}"
    ↓
Notification fires
    ↓
Cache hit ✅ Shows custom image
```

## Implementation

### 1. HabitImagePreloadWorker
**File**: `image/HabitImagePreloadWorker.kt`

```kotlin
@HiltWorker
class HabitImagePreloadWorker : CoroutineWorker() {
    override suspend fun doWork(): Result {
        // Get habits with custom images
        val customImageHabits = habitRepository.getAllHabits()
            .filter { it.avatar.type == CUSTOM_IMAGE }
        
        // Preload each image with stable cache key
        customImageHabits.forEach { habit ->
            val cacheKey = "habit_avatar_${habit.id}"
            imageLoader.execute(request)
        }
        
        return Result.success()
    }
}
```

### 2. Trigger Points

**App Startup** (HabitViewModel.kt line 150):
```kotlin
if (habits.isNotEmpty()) {
    scheduleImagePreload() // Non-blocking background preload
}
```

**After Restore** (HabitViewModel.kt line 708):
```kotlin
fun restoreHabit(habitId: Long) {
    // ... restore logic ...
    scheduleImagePreload() // Refresh cache with new URL
}
```

### 3. Smart Scheduling

```kotlin
WorkManager.getInstance(context).enqueueUniqueWork(
    HabitImagePreloadWorker.WORK_NAME,
    ExistingWorkPolicy.KEEP, // Don't start if already running
    workRequest
)
```

## Performance Characteristics

### ✅ No UI Blocking
- Runs on WorkManager background thread
- Uses `Dispatchers.IO` for network operations
- UI remains responsive during preload

### ✅ Efficient
- One-time work per trigger
- `KEEP` policy prevents duplicate work
- Only preloads custom images (not emojis/icons)

### ✅ Best Effort
- Failures don't crash the app
- Returns `Result.success()` even if some images fail
- Logs success/failure counts for debugging

### ✅ Smart Caching
- Uses same cache keys as UI (`habit_avatar_{id}`)
- Small size (128px) for notifications
- Respects Coil's cache policies

## Benefits

| Scenario | Before | After |
|----------|--------|-------|
| Cache cleared + notification | 📷 fallback | ✅ Custom image |
| App startup | Images load on demand | Preloaded in background |
| Restore habit | Stale cache | Fresh cache |
| Network available | On-demand loading | Proactive preloading |

## Testing

### Verify Preload Works:

1. **Clear app cache**:
   ```
   Settings → Apps → HabitTracker → Storage → Clear Cache
   ```

2. **Open app** (triggers preload):
   ```
   Check logs: "Starting habit image preload..."
   Check logs: "Preloading X custom images..."
   Check logs: "Image preload complete: Y succeeded"
   ```

3. **Trigger notification** (without opening app UI):
   ```
   Set habit reminder for 1 minute from now
   Wait for notification
   Should show custom image, not 📷 fallback
   ```

### Monitor Logs:

```powershell
adb logcat | Select-String "HabitImagePreload|scheduleImagePreload"
```

Expected output:
```
HabitViewModel: Scheduled background image preload
HabitImagePreload: Starting habit image preload...
HabitImagePreload: Preloading 3 custom images...
HabitImagePreload: ✅ Cached image for habit: Morning Routine
HabitImagePreload: ✅ Cached image for habit: Study Session
HabitImagePreload: ✅ Cached image for habit: Exercise
HabitImagePreload: Image preload complete: 3 succeeded, 0 failed
```

## Architecture Benefits

1. **Separation of Concerns**: Image preloading is separate from UI
2. **Resilient**: Failures don't impact app functionality
3. **Observable**: Detailed logging for debugging
4. **Scalable**: Handles any number of habits efficiently
5. **Android-native**: Uses WorkManager (battery-friendly, survives app restarts)

## Related Systems

- **Stable Cache Keys**: Uses `habit_avatar_{id}` everywhere
- **Restore Flow**: Triggers preload to refresh cache
- **Notification System**: Reads from cache (no network)
- **UI Loading**: Uses same cache keys as preloader

## Future Enhancements

Potential improvements (not implemented yet):
- Periodic preload to refresh expired GitHub tokens
- Network-aware preloading (WiFi only)
- Image size optimization based on device
- Preload priority based on alarm schedule
