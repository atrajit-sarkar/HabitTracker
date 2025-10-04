# Notification Sound Not Saving - Debug & Fix

## Issue Report
**Problem:** When adding a custom notification sound for a habit in the app, all habits show the default notification sound in the Android system notification channel settings instead of the selected custom sound.

**User Report:** "Even if I add custom for a habit notification from the app all habits are saved with default notification in the notification channel in app settings"

## Investigation

### Current Implementation

The app has a comprehensive notification sound system:
1. ✅ Per-habit notification channels (Android O+)
2. ✅ Dynamic sound loading from device (30-60 sounds via RingtoneManager)
3. ✅ Sound preview UI with MediaPlayer
4. ✅ Channel recreation when sound changes
5. ✅ Database storage of sound data (ID, name, URI)

### Code Flow Analysis

#### When Creating a New Habit:
```
1. User opens Add Habit Sheet
   ├─ AddHabitState initialized with NotificationSound.DEFAULT
   └─ Available sounds loaded from device

2. User selects custom sound
   ├─ NotificationSoundSelector dropdown shows all sounds
   ├─ User clicks on a sound
   └─ onNotificationSoundChange() called
       └─ Updates UI state: addHabitState.notificationSound = selected sound

3. User clicks Save
   ├─ saveHabit() called in HabitViewModel
   ├─ Habit object created with:
   │   ├─ notificationSoundId = addForm.notificationSound.id
   │   ├─ notificationSoundName = addForm.notificationSound.displayName
   │   └─ notificationSoundUri = addForm.notificationSound.uri
   ├─ Habit saved to database
   └─ HabitReminderService.updateHabitChannel() called
       └─ Deletes and recreates channel with new sound
```

#### Channel Creation (ensureHabitChannel):
```
1. Get channelId = "habit_reminder_${habitId}"

2. Check if channel exists
   ├─ If exists:
   │   ├─ Get current sound URI from existing channel
   │   ├─ Get new sound URI from habit data
   │   └─ If different: Delete channel
   └─ If not exists: Continue

3. Create channel (if null)
   ├─ Get sound URI: NotificationSound.getActualUri(context, habit.getNotificationSound())
   ├─ Create NotificationChannel with:
   │   ├─ Channel ID
   │   ├─ Name: "Reminder: {habit.title}"
   │   ├─ Importance: HIGH
   │   └─ Sound: soundUri + AudioAttributes
   └─ Call manager.createNotificationChannel()
```

### Potential Issues

#### 1. **Sound URI Resolution**
The `getActualUri()` function handles different sound types:
- `DEFAULT_ID` → Returns `RingtoneManager.getDefaultUri(TYPE_NOTIFICATION)`
- `SYSTEM_DEFAULT_ID` → Returns `RingtoneManager.getDefaultUri(TYPE_NOTIFICATION)`  
- Custom sounds → Returns `Uri.parse(sound.uri)`

**Potential Problem:** If `sound.uri` is empty or invalid, it returns null, and the channel is created without a sound (uses system default).

#### 2. **Race Condition in Channel Creation**
After deleting a channel in `ensureHabitChannel`, there might be a delay before Android OS confirms deletion. The immediate check for `manager.getNotificationChannel(channelId) == null` might return the deleted channel momentarily.

#### 3. **Notification Channel Caching**
Android caches notification channel settings. Even after deletion, the system might retain old settings for a short time.

#### 4. **Sound Not Stored Correctly**
If the sound selection dropdown doesn't properly capture the URI, the database might store an empty URI.

## Enhanced Logging Added

To diagnose the issue, I've added comprehensive logging:

### In HabitViewModel:
```kotlin
// When sound is changed in UI
onNotificationSoundChange(sound: NotificationSound) {
    Log.d("HabitViewModel", "Notification sound changed to: ${sound.displayName} (ID: ${sound.id}, URI: ${sound.uri})")
    // ... update state
}

// When saving habit
saveHabit() {
    Log.d("HabitViewModel", "Saving habit with notification sound: ...")
    Log.d("HabitViewModel", "Habit object created: soundId=..., soundName=..., soundUri=...")
    Log.d("HabitViewModel", "Habit saved to database with ID: ..., calling updateHabitChannel...")
}
```

### In HabitReminderService:
```kotlin
ensureHabitChannel() {
    Log.d("HabitReminderService", "ensureHabitChannel called for habit: ${habit.title}")
    Log.d("HabitReminderService", "  Habit sound: ${habit.notificationSoundName} (ID: ..., URI: ...)")
    
    // When channel exists
    Log.d("HabitReminderService", "  Channel exists. Current sound: ..., New sound: ...")
    Log.d("HabitReminderService", "  Sounds are different! Deleting channel...")
    // OR
    Log.d("HabitReminderService", "  Sounds are same, keeping existing channel")
    
    // When creating channel
    Log.d("HabitReminderService", "  Creating channel with sound URI: $soundUri")
    Log.d("HabitReminderService", "  Sound set on channel: $soundUri")
    // OR
    Log.w("HabitReminderService", "  Sound URI is null! Channel will use default sound")
    
    // Verification
    Log.d("HabitReminderService", "✓ Created channel ... with sound: ...")
    Log.d("HabitReminderService", "  Verification: Channel sound after creation: ...")
}
```

## Testing Instructions

### 1. Install the APK
The APK with enhanced logging has been built. Install it on your device.

### 2. Clear Existing Data (Recommended)
To start fresh:
```
Settings → Apps → Habit Tracker → Storage → Clear Data
```

OR uninstall and reinstall the app.

### 3. Test Scenario
1. **Open the app** and sign in
2. **Create a new habit**
3. **In the notification sound dropdown**, select a custom sound (not "Default Notification")
4. **Preview the sound** to confirm it plays
5. **Save the habit**
6. **Open Android Settings** → Apps → Habit Tracker → Notifications
7. **Find the habit's notification channel**
8. **Check the sound** - does it show your custom sound or "Default"?

### 4. Collect Logs
Connect your phone to PC and run:
```powershell
adb logcat -s HabitViewModel:D HabitReminderService:D > notification_debug.log
```

Then repeat the test scenario. The log will show:
- What sound you selected in the UI
- What sound was saved to the database
- What sound URI was used when creating the channel
- Whether the channel was created successfully

## Expected Log Output (Success Case)

```
D/HabitViewModel: Notification sound changed to: Bounce (ID: content://media/internal/audio/media/123, URI: content://media/internal/audio/media/123)
D/HabitViewModel: Saving habit with notification sound: Bounce (ID: content://..., URI: content://...)
D/HabitViewModel: Habit object created: soundId=content://..., soundName=Bounce, soundUri=content://...
D/HabitViewModel: Habit saved to database with ID: 1, calling updateHabitChannel...
D/HabitReminderService: ensureHabitChannel called for habit: Morning Exercise (ID: 1)
D/HabitReminderService:   Habit sound: Bounce (ID: content://..., URI: content://...)
D/HabitReminderService:   Channel does not exist, will create new one
D/HabitReminderService:   Creating channel with sound URI: content://media/internal/audio/media/123
D/HabitReminderService:   Sound set on channel: content://media/internal/audio/media/123
D/HabitReminderService: ✓ Created channel habit_reminder_1 with sound: content://media/internal/audio/media/123
D/HabitReminderService:   Verification: Channel sound after creation: content://media/internal/audio/media/123
```

## Expected Log Output (Failure Case)

If the issue occurs, you'll see one of these patterns:

### Pattern 1: Sound URI is empty
```
D/HabitViewModel: Notification sound changed to: Bounce (ID: some_id, URI: )  ← Empty URI!
D/HabitViewModel: Saving habit with notification sound: Bounce (ID: some_id, URI: )
...
D/HabitReminderService:   Creating channel with sound URI: null  ← NULL!
W/HabitReminderService:   Sound URI is null! Channel will use default sound
```

**Diagnosis:** Sound URI not captured from RingtoneManager

### Pattern 2: Wrong sound stored
```
D/HabitViewModel: Notification sound changed to: Bounce (ID: ..., URI: ...)  ← Correct
D/HabitViewModel: Saving habit with notification sound: Default Notification (ID: default, URI: )  ← WRONG!
```

**Diagnosis:** State not updating correctly, or being reset before save

### Pattern 3: Channel not created with sound
```
D/HabitReminderService:   Creating channel with sound URI: content://media/internal/audio/media/123
D/HabitReminderService:   Sound set on channel: content://media/internal/audio/media/123
D/HabitReminderService:   Verification: Channel sound after creation: content://settings/system/notification_sound  ← Different!
```

**Diagnosis:** Android OS overriding the sound or permission issue

## Potential Fixes

### Fix 1: Add Delay After Channel Deletion
```kotlin
// Delete existing channel if sound has changed
if (currentSoundUri != newSoundUri) {
    manager.deleteNotificationChannel(channelId)
    // Add small delay to ensure deletion is processed
    Thread.sleep(100)  // 100ms delay
}
```

### Fix 2: Verify Sound URI Before Saving
```kotlin
fun onNotificationSoundChange(sound: NotificationSound) {
    // Verify the sound has a valid URI
    if (sound.id != NotificationSound.DEFAULT_ID && sound.uri.isEmpty()) {
        Log.e("HabitViewModel", "Selected sound has empty URI: $sound")
        // Maybe show error to user
        return
    }
    
    _uiState.update { state ->
        state.copy(addHabitState = state.addHabitState.copy(notificationSound = sound))
    }
}
```

### Fix 3: Force Channel Recreation Every Time
```kotlin
fun updateHabitChannel(context: Context, habit: Habit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = "${CHANNEL_PREFIX}${habit.id}"
        val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?: return
        
        // ALWAYS delete and recreate (don't try to be smart about it)
        manager.deleteNotificationChannel(channelId)
        Thread.sleep(100) // Wait for deletion
        
        // Recreate the channel
        ensureHabitChannel(context, habit)
    }
}
```

### Fix 4: Check Android Permissions
Ensure app has permission to set custom notification sounds:
```xml
<!-- In AndroidManifest.xml -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />
```

## Next Steps

1. **Install the updated APK** on your device
2. **Run the test scenario** (create habit with custom sound)
3. **Collect logs** using adb logcat
4. **Check Android notification settings** to see if sound is correct
5. **Share the logs** so we can identify the exact failure point

## Files Modified

| File | Changes |
|------|---------|
| `HabitViewModel.kt` | Added logging in `onNotificationSoundChange()` and `saveHabit()` |
| `HabitReminderService.kt` | Added comprehensive logging in `ensureHabitChannel()` |

## Build Status

✅ **BUILD SUCCESSFUL**  
📦 APK Location: `app/build/outputs/apk/debug/app-debug.apk`

---

Once you test this and provide the logs, we'll know exactly where the issue is and can implement the appropriate fix!
