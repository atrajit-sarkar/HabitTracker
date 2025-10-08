# Edit Habit - Firebase Sync Testing Guide

## Enhanced Logging Installed ✅

The app now has comprehensive logging for both ViewModel and Firestore operations.

## How to Test and Monitor

### Step 1: Start Logcat Monitoring

Open PowerShell and run:
```powershell
adb logcat -s HabitViewModel:D FirestoreRepo:D | Select-String "update|Update|UPDATE|Edit|EDIT"
```

Or for full logging:
```powershell
adb logcat -s HabitViewModel:D FirestoreRepo:D
```

### Step 2: Edit a Habit

1. **Open the app**
2. **Long-press** any habit card
3. **Tap Edit** button (pencil icon)
4. **Make changes** (change time, emoji, sound, etc.)
5. **Tap "Update Habit"**

### Step 3: Watch the Logs

You should see this sequence:

#### Loading Phase:
```
HabitViewModel: Loading habit for edit: ID=...
HabitViewModel: Habit data: title=..., desc=...
HabitViewModel: Time: HH:MM, enabled=true/false
HabitViewModel: Frequency: DAILY/WEEKLY/etc
HabitViewModel: Avatar: HabitAvatar(...)
HabitViewModel: Sound: id=..., name=..., uri=...
HabitViewModel: [Sound matching logs - trying different strategies]
HabitViewModel: Selected notification sound: ... (ID: ...)
HabitViewModel: Edit state updated successfully
```

#### Saving Phase:
```
HabitViewModel: Saving habit with notification sound: ...
HabitViewModel: EDIT MODE: Updating habit ID=...
HabitViewModel: Existing habit loaded: ...
HabitViewModel: New values - Hour: X, Minute: Y, Avatar: ..., Sound: ...
HabitViewModel: Habit object created: soundId=..., soundName=..., soundUri=...
HabitViewModel: Habit details - ID: ..., Title: ..., Hour: ..., Minute: ...
HabitViewModel: Habit avatar: ..., Frequency: ...
HabitViewModel: Calling habitRepository.updateHabit() for ID=...
```

#### Firestore Update Phase:
```
FirestoreRepo: updateHabit called for habit ID: ..., title: ...
FirestoreRepo: Found Firestore document: ...
FirestoreRepo: Converted to FirestoreHabit: ..., hour: ..., avatar: ...
FirestoreRepo: ✅ Firestore document updated successfully!
```

#### Completion Phase:
```
HabitViewModel: ✅ habitRepository.updateHabit() completed successfully!
HabitViewModel: Habit saved to database with ID: ...
```

## What to Check

### ✅ Success Indicators:
1. **"Loading habit for edit"** - Shows edit started
2. **"Selected notification sound"** - Shows correct sound loaded
3. **"EDIT MODE: Updating habit"** - Confirms edit mode active
4. **"Calling habitRepository.updateHabit()"** - Update initiated
5. **"updateHabit called for habit ID"** - Firestore received request
6. **"✅ Firestore document updated successfully!"** - Update completed
7. **"✅ habitRepository.updateHabit() completed"** - All done!

### ❌ Error Indicators:
- **"No match found, using default sound"** - Sound not found
- **"❌ Error updating habit"** - Update failed
- **Exception stack trace** - Something crashed
- **Missing Firestore logs** - Not reaching Firestore

## Verify Changes Persisted

### In the App:
1. Go back to home screen
2. Check if changes are visible
3. Long-press and edit again
4. Verify all fields still show updated values

### In Firebase Console:
1. Open [Firebase Console](https://console.firebase.google.com/)
2. Go to your project
3. Navigate to **Firestore Database**
4. Find: `users/{userId}/habits`
5. Look for your edited habit document
6. Verify fields are updated:
   - `reminderHour`
   - `reminderMinute`
   - `avatar` (emoji, color)
   - `notificationSoundId`
   - `notificationSoundName`
   - `notificationSoundUri`

## Common Issues and Solutions

### Issue 1: Sound Not Loading Correctly
**Symptom:** 
```
Sound: id=notification_225, name=notification_013, uri=...
Selected notification sound: Default Notification  ← WRONG
```

**Solution Applied:** 
The new code tries multiple matching strategies:
1. Exact ID match
2. URI match (most reliable)
3. Display name match
4. Recreate from stored data

**What to Check:**
- Look for "Found sound by URI" or "Found sound by name"
- If you see "Recreating sound from stored data" - that's OK too

### Issue 2: Firestore Update Not Called
**Symptom:**
```
HabitViewModel: Calling habitRepository.updateHabit()
[No FirestoreRepo logs after this]
```

**Possible Causes:**
- Not authenticated
- Network issue
- Exception thrown

**What to Check:**
- Full logcat: `adb logcat | Select-String "update"`
- Look for exceptions or errors

### Issue 3: Changes Not Visible After Update
**Symptom:**
- Logs show success
- But changes don't appear

**Possible Causes:**
- UI not refreshing
- Caching issue
- Firestore sync delay

**Solution:**
- Close and reopen the app
- Check Firebase Console directly
- Wait a few seconds for sync

## Test Different Scenarios

### Test 1: Change Time
1. Edit habit
2. Change time from 9:00 to 14:30
3. Save
4. Verify logs show: `New values - Hour: 14, Minute: 30`
5. Check Firestore: `reminderHour: 14, reminderMinute: 30`

### Test 2: Change Avatar/Emoji
1. Edit habit
2. Change emoji
3. Save
4. Verify logs show new avatar
5. Check Firestore: `avatar.value` field updated

### Test 3: Change Notification Sound
1. Edit habit
2. Select different sound
3. Save
4. Verify logs show: `Sound: [new sound name]`
5. Check Firestore: `notificationSoundId`, `notificationSoundName`, `notificationSoundUri` updated

### Test 4: Change Frequency
1. Edit habit
2. Change from Daily to Weekly
3. Select a day
4. Save
5. Verify logs show: `Frequency: WEEKLY, dayOfWeek: X`
6. Check Firestore: `frequency: "WEEKLY"`, `dayOfWeek: X`

### Test 5: Toggle Reminder
1. Edit habit
2. Turn reminder off/on
3. Save
4. Verify logs show: `enabled=true/false`
5. Check Firestore: `reminderEnabled: true/false`

## Debug Commands Reference

### Clear Logs:
```bash
adb logcat -c
```

### Start Monitoring:
```powershell
# Focused view
adb logcat -s HabitViewModel:D FirestoreRepo:D

# Search for specific terms
adb logcat | Select-String "update|firestore|edit"

# Save to file
adb logcat -s HabitViewModel:D FirestoreRepo:D > edit_debug.txt
```

### Check Firestore Status:
```powershell
adb logcat | Select-String "Firestore|Firebase"
```

## Expected Complete Flow

```
📱 User Actions:
1. Long-press habit
2. Tap Edit
3. Change values
4. Tap Update

⚙️ App Flow:
1. loadHabitForEdit() → Load from Firestore
2. Populate form fields
3. User edits
4. saveHabit() → Validate
5. Create updated Habit object
6. habitRepository.updateHabit() → Save to Firestore
7. Update notification channel
8. Reschedule notifications
9. Exit selection mode
10. Show "Habit updated" message

☁️ Firestore Flow:
1. findHabitDocument() → Find by ID
2. toFirestoreHabit() → Convert to Firestore format
3. doc.reference.set() → Upload to cloud
4. await() → Wait for confirmation
5. ✅ Success!

🔄 Sync Flow:
1. Firestore snapshot listener fires
2. observeHabits() receives update
3. UI automatically refreshes
4. Changes visible immediately
```

## Success Criteria ✅

All of these should be true:
- [ ] Edit screen loads with correct data
- [ ] All fields pre-filled (title, desc, time, emoji, sound)
- [ ] Changes can be made to any field
- [ ] "Update Habit" button saves changes
- [ ] Logs show "✅ Firestore document updated successfully!"
- [ ] Changes visible on home screen
- [ ] Changes persist after app restart
- [ ] Changes visible in Firebase Console
- [ ] Notification rescheduled with new time
- [ ] Selection mode exits after update

---

**Status**: Enhanced debug build installed
**Next Step**: Test editing a habit and monitor the logs!
