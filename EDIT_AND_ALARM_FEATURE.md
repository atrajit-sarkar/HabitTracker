# Edit Habit & Alarm-Type Notifications Implementation

## Date: October 2, 2025

## Overview
This document describes the implementation of two major new features:
1. **Edit Habit Functionality** - Users can now edit existing habits
2. **Alarm-Type Notifications** - Continuous notification alerts that ring until marked as done

---

## ✅ COMPLETED CHANGES

### 1. Database Schema Updates

#### **Habit Model** (`data/local/Habit.kt`)
- ✅ Added `isAlarmType: Boolean = false` field
  - Determines if the habit uses continuous alarm-style notifications
  - Default is `false` for backward compatibility

#### **Firestore Model** (`data/firestore/FirestoreModels.kt`)
- ✅ Added `isAlarmType: Boolean = false` field to `FirestoreHabit`
- ✅ Updated `toFirestoreHabit()` conversion to include `isAlarmType`
- ✅ Updated Firestore document parsing to read `isAlarmType`

### 2. UI State Management

#### **AddHabitState** (`ui/HabitUiModels.kt`)
- ✅ Added `editingHabitId: Long? = null` field
  - `null` = Creating new habit
  - `non-null` = Editing existing habit
- ✅ Added `isAlarmType: Boolean = false` field
- ✅ Added `isEditMode` computed property

### 3. ViewModel Updates (`ui/HabitViewModel.kt`)

#### **New Functions:**
- ✅ `showEditHabitSheet(habitId: Long)` - Opens edit sheet with habit data pre-filled
- ✅ `onAlarmTypeToggle(isAlarmType: Boolean)` - Toggles alarm-type notification
- ✅ `habitUpdatedMessage()` - Shows success message after edit

#### **Modified Functions:**
- ✅ `saveHabit()` - Now handles both creating AND updating habits
  - Checks `isEditMode` to determine operation
  - Uses `updateHabit()` for edits, `insertHabit()` for new habits
  - Shows appropriate success message

---

## 🔧 REMAINING IMPLEMENTATION TASKS

### Phase 1: Complete Backend & Repository

#### 1. Repository Interface Updates
**File:** `data/HabitRepository.kt`

```kotlin
// Add this function if not already present:
suspend fun updateHabit(habit: Habit)
```

#### 2. Firestore Repository Implementation
**File:** `data/firestore/FirestoreHabitRepository.kt`

```kotlin
override suspend fun updateHabit(habit: Habit) {
    val userCollection = getUserCollection() ?: throw IllegalStateException("User not authenticated")
    val doc = findHabitDocument(habit.id) ?: throw NoSuchElementException("Habit not found")
    
    val firestoreHabit = habit.toFirestoreHabit(doc.id, habit.id)
    doc.reference.set(firestoreHabit).await()
    
    android.util.Log.d("FirestoreRepo", "Updated habit: ${habit.title} (ID: ${habit.id})")
}
```

### Phase 2: UI Integration - Home Screen

#### 1. Add Edit Button to HabitCard
**File:** `ui/HomeScreen.kt`

Find the `HabitCard` composable and add an edit button:

```kotlin
// Inside HabitCard, add this near the delete button:
IconButton(onClick = onEdit) {
    Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = "Edit Habit",
        tint = Color.White
    )
}
```

Update `HabitCard` signature:
```kotlin
@Composable
private fun HabitCard(
    habit: HabitCardUi,
    onToggleReminder: (Boolean) -> Unit,
    onMarkCompleted: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,  // NEW
    onSeeDetails: () -> Unit
) {
```

#### 2. Wire Up Edit Callback
**File:** `ui/HomeScreen.kt`

In `HabitHomeRoute`, add parameter:
```kotlin
@Composable
fun HabitHomeRoute(
    state: HabitScreenState,
    user: User?,
    onAddHabitClick: () -> Unit,
    onToggleReminder: (Long, Boolean) -> Unit,
    onMarkHabitCompleted: (Long) -> Unit,
    onEditHabit: (Long) -> Unit,  // NEW
    onDeleteHabit: (Long) -> Unit,
    // ... rest of parameters
) {
```

Update the `HabitCard` call:
```kotlin
items(state.habits, key = { it.id }) { habit ->
    HabitCard(
        habit = habit,
        onToggleReminder = { enabled -> onToggleReminder(habit.id, enabled) },
        onMarkCompleted = { onMarkHabitCompleted(habit.id) },
        onDelete = { onDeleteHabit(habit.id) },
        onEdit = { onEditHabit(habit.id) },  // NEW
        onSeeDetails = { onHabitDetailsClick(habit.id) }
    )
}
```

#### 3. Connect to MainActivity
**File:** `MainActivity.kt`

Find where `HabitHomeRoute` is called and add:
```kotlin
HabitHomeRoute(
    state = habitState,
    user = user,
    onAddHabitClick = viewModel::showAddHabitSheet,
    onToggleReminder = viewModel::toggleReminder,
    onMarkHabitCompleted = viewModel::markHabitCompleted,
    onEditHabit = viewModel::showEditHabitSheet,  // NEW
    onDeleteHabit = { habitId ->
        // existing code
    },
    // ... rest of callbacks
)
```

### Phase 3: Add Alarm Type Toggle to Add/Edit Sheet

#### Update AddHabitSheet
**File:** Find the add habit bottom sheet UI (likely in `HomeScreen.kt` or separate file)

Add after the notification sound selector:

```kotlin
// Alarm-type notification toggle
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = "Alarm-type Notification",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Ring continuously until marked as done",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Switch(
        checked = state.addHabitState.isAlarmType,
        onCheckedChange = viewModel::onAlarmTypeToggle
    )
}
```

Update the sheet title to show edit mode:
```kotlin
Text(
    text = if (state.addHabitState.isEditMode) "Edit Habit" else "Add New Habit",
    style = MaterialTheme.typography.headlineSmall,
    fontWeight = FontWeight.Bold
)
```

Update save button text:
```kotlin
Button(
    onClick = viewModel::saveHabit,
    enabled = !state.addHabitState.isSaving
) {
    Text(if (state.addHabitState.isEditMode) "Update Habit" else "Save Habit")
}
```

### Phase 4: Implement Alarm-Type Notifications

#### 1. Create Alarm Notification Service
**File:** `notification/AlarmNotificationService.kt` (NEW FILE)

```kotlin
package com.example.habittracker.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.habittracker.MainActivity
import com.example.habittracker.R

class AlarmNotificationService : Service() {
    
    private var ringtone: android.media.Ringtone? = null
    private var vibrator: Vibrator? = null
    
    companion object {
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_TITLE = "habit_title"
        const val EXTRA_SOUND_URI = "sound_uri"
        const val CHANNEL_ID = "habit_alarm_channel"
        const val NOTIFICATION_ID = 9999
        
        fun start(context: Context, habitId: Long, habitTitle: String, soundUri: String?) {
            val intent = Intent(context, AlarmNotificationService::class.java).apply {
                putExtra(EXTRA_HABIT_ID, habitId)
                putExtra(EXTRA_HABIT_TITLE, habitTitle)
                putExtra(EXTRA_SOUND_URI, soundUri)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmNotificationService::class.java))
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val habitId = intent?.getLongExtra(EXTRA_HABIT_ID, 0L) ?: 0L
        val habitTitle = intent?.getStringExtra(EXTRA_HABIT_TITLE) ?: "Habit Reminder"
        val soundUriString = intent?.getStringExtra(EXTRA_SOUND_URI)
        
        // Start ringing
        startRinging(soundUriString)
        
        // Show notification
        val notification = buildNotification(habitId, habitTitle)
        startForeground(NOTIFICATION_ID, notification)
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }
    
    private fun startRinging(soundUriString: String?) {
        // Start sound
        val soundUri = try {
            if (!soundUriString.isNullOrEmpty()) {
                Uri.parse(soundUriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
        } catch (e: Exception) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
        
        ringtone = RingtoneManager.getRingtone(this, soundUri)
        ringtone?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isLooping = true
            }
            play()
        }
        
        // Start vibration
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 1000, 500, 1000, 500)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            val pattern = longArrayOf(0, 1000, 500, 1000, 500)
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }
    
    private fun stopRinging() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }
    
    private fun buildNotification(habitId: Long, habitTitle: String): Notification {
        // Open app intent
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Mark as done intent
        val doneIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_DONE
            putExtra(NotificationActionReceiver.EXTRA_HABIT_ID, habitId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            this, habitId.toInt(), doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(habitTitle)
            .setContentText("Time to complete your habit!")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(openPendingIntent, true)
            .setContentIntent(openPendingIntent)
            .addAction(
                R.drawable.ic_check,
                "Mark as Done",
                donePendingIntent
            )
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Habit Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm-style notifications for habits"
                setSound(null, null) // We handle sound ourselves
                enableVibration(false) // We handle vibration ourselves
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
```

#### 2. Update Manifest
**File:** `AndroidManifest.xml`

Add the service inside `<application>` tag:
```xml
<service
    android:name=".notification.AlarmNotificationService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="mediaPlayback" />
```

#### 3. Update NotificationActionReceiver
**File:** `notification/NotificationActionReceiver.kt`

Add logic to stop alarm service when marked as done:
```kotlin
// In onReceive(), after marking habit as complete:
if (action == ACTION_MARK_DONE) {
    // Stop alarm service if running
    AlarmNotificationService.stop(context)
    
    // existing mark complete code...
}
```

#### 4. Update HabitReminderReceiver
**File:** `notification/HabitReminderReceiver.kt`

Check if habit uses alarm-type and start service instead of regular notification:

```kotlin
override fun onReceive(context: Context, intent: Intent) {
    val habitId = intent.getLongExtra(EXTRA_HABIT_ID, 0L)
    if (habitId == 0L) return

    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    scope.launch {
        try {
            val repository = (context.applicationContext as HabitTrackerApp)
                .habitRepository
            
            val habit = repository.getHabitById(habitId) ?: return@launch
            val shouldNotify = !habit.isCompletedToday()
            
            android.util.Log.d("HabitReminderReceiver", 
                "Habit: ${habit.title}, Completed today: ${habit.isCompletedToday()}, Should notify: $shouldNotify")
            
            if (shouldNotify) {
                // Check if alarm-type
                if (habit.isAlarmType) {
                    // Start alarm service
                    AlarmNotificationService.start(
                        context,
                        habit.id,
                        habit.title,
                        habit.notificationSoundUri
                    )
                    android.util.Log.d("HabitReminderReceiver", "Started alarm service for: ${habit.title}")
                } else {
                    // Regular notification
                    HabitReminderService.showNotification(context, habit)
                    android.util.Log.d("HabitReminderReceiver", "Showed regular notification for: ${habit.title}")
                }
            }
            
            // Reschedule next reminder
            val scheduler = (context.applicationContext as HabitTrackerApp)
                .habitReminderScheduler
            scheduler.schedule(habit)
            
        } catch (e: Exception) {
            android.util.Log.e("HabitReminderReceiver", "Error in reminder receiver", e)
        }
    }
}
```

#### 5. Stop Alarm When Habit Completed
**File:** `ui/HabitViewModel.kt`

Update `markHabitCompleted`:
```kotlin
fun markHabitCompleted(habitId: Long) {
    viewModelScope.launch(Dispatchers.IO) {
        habitRepository.markCompletedToday(habitId)
        
        // Stop alarm service if running
        AlarmNotificationService.stop(context)
        
        // Update stats after completion
        updateUserStatsAsync()
    }
}
```

---

## 🧪 TESTING PLAN

### Test 1: Edit Habit Functionality
1. ✅ Create a new habit
2. Click edit button on the habit card
3. Verify all fields are pre-filled correctly
4. Change title, time, frequency
5. Save and verify changes are persisted
6. Check Firestore to confirm update

### Test 2: Alarm-Type Notifications (Idle Device)
1. Create a habit with alarm-type enabled
2. Set reminder for 2 minutes from now
3. **Lock the device and wait**
4. When alarm triggers:
   - Should ring continuously
   - Should vibrate
   - Should show full-screen notification
5. Unlock device and mark as done
6. Verify alarm stops

### Test 3: Regular vs Alarm Notifications
1. Create two habits at same time:
   - Habit A: Regular notification
   - Habit B: Alarm-type
2. Wait for both to trigger
3. Verify:
   - Habit A: Single notification sound
   - Habit B: Continuous ringing until dismissed

### Test 4: Logcat Monitoring
Run these ADB commands to monitor notifications:

```bash
# Clear logcat
adb logcat -c

# Monitor all habit-related logs
adb logcat | grep -E "HabitReminder|AlarmNotification|HabitViewModel"

# Monitor specifically for idle/doze issues
adb logcat | grep -E "AlarmManager|PowerManager"
```

### Test 5: Doze Mode Testing
```bash
# Force device into doze mode
adb shell dumpsys deviceidle force-idle

# Check if alarm still fires
# Wait for notification...

# Exit doze mode
adb shell dumpsys deviceidle unforce

# Check logs
adb logcat | grep "HabitReminder"
```

---

## 📱 USER GUIDE

### How to Edit a Habit
1. Find the habit on your home screen
2. Click the edit icon (✏️) next to the delete button
3. Make your changes
4. Click "Update Habit"

### How to Enable Alarm-Type Notifications
**When Creating/Editing a Habit:**
1. Scroll down to "Alarm-type Notification" toggle
2. Enable it if you want continuous ringing
3. The alarm will:
   - Ring continuously until you mark it as done
   - Work even when device is idle/locked
   - Show a full-screen notification
   - Stop when you:
     - Mark the habit as done from notification
     - Mark the habit as done from the app
     - Swipe away the notification

---

## 🔍 DEBUGGING NOTIFICATIONS ON IDLE DEVICES

### Check if Exact Alarms are Allowed
```kotlin
// Add this log in HabitReminderScheduler
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val canSchedule = alarmManager?.canScheduleExactAlarms()
    android.util.Log.d("Scheduler", "Can schedule exact alarms: $canSchedule")
}
```

### Battery Optimization Check
Add option to request battery optimization exemption:
```kotlin
// In MainActivity or settings
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    val intent = Intent().apply {
        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        data = Uri.parse("package:${packageName}")
    }
    startActivity(intent)
}
```

### Monitor Alarm Manager
```bash
# Check scheduled alarms
adb shell dumpsys alarm | grep com.example.habittracker

# Check if alarms are being throttled
adb shell dumpsys alarm | grep -A 20 "Pending alarm batches"
```

---

## ✅ CHECKLIST FOR COMPLETION

- [x] Database schema updated with `isAlarmType` field
- [x] Firestore model updated
- [x] UI state management updated
- [x] ViewModel updated with edit & alarm toggle
- [ ] Repository `updateHabit()` implemented
- [ ] UI: Add edit button to HabitCard
- [ ] UI: Add alarm-type toggle to Add/Edit sheet
- [ ] UI: Update sheet title for edit mode
- [ ] AlarmNotificationService created
- [ ] Manifest updated with service
- [ ] Receiver updated to handle alarm-type
- [ ] Stop alarm when habit completed
- [ ] Test on locked device
- [ ] Test in doze mode
- [ ] Verify notifications work when app is closed

---

## 🚀 NEXT STEPS

1. Implement remaining UI changes (edit button, alarm toggle)
2. Create AlarmNotificationService
3. Test alarm notifications on locked device
4. Monitor logcat during idle testing
5. Optimize battery usage
6. Add user guide in app settings

---

## 📝 NOTES

- The `setExactAndAllowWhileIdle()` API is already being used, which should work during idle
- For best results on Android 12+, users should grant "Alarms & Reminders" permission
- Some manufacturers (Samsung, Xiaomi, etc.) may require additional battery optimization settings
- The alarm service uses `START_STICKY` to restart if killed by system
- Full-screen intent ensures alarm shows even on locked screen

