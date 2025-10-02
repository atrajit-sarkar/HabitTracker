# Testing Notification Reliability Fixes

## Quick Test Commands

### 1. Monitor Logs
```bash
# Watch all notification-related logs
adb logcat -s BootReceiver AlarmVerificationWorker NotificationReliability HabitReminderScheduler HabitReminderReceiver MainActivity

# Watch only errors
adb logcat *:E

# Clear logs before test
adb logcat -c
```

### 2. Test Boot Receiver
```bash
# Reboot device
adb reboot

# After device boots, check if alarms were rescheduled
adb logcat -s BootReceiver | grep "Rescheduled"

# Expected output:
# BootReceiver: Device booted or app updated, rescheduling reminders...
# BootReceiver: Rescheduled reminder for: Morning Meditation
# BootReceiver: Successfully rescheduled X reminders out of Y total habits
```

### 3. Test Doze Mode (Quick Test)
```bash
# Unplug device
adb shell dumpsys battery unplug

# Force device into Doze mode immediately
adb shell dumpsys deviceidle force-idle

# Check current Doze state (should show "IDLE")
adb shell dumpsys deviceidle get deep

# Wait for notification to trigger...

# Exit Doze mode
adb shell dumpsys deviceidle unforce

# Re-enable battery
adb shell dumpsys battery reset
```

### 4. Check Scheduled Alarms
```bash
# List all pending alarms for your app
adb shell dumpsys alarm | grep -A 20 "com.example.habittracker"

# Look for "RTC_WAKEUP" entries with your app's package name
```

### 5. Check WorkManager Status
```bash
# List all WorkManager work
adb shell dumpsys jobscheduler | grep -A 30 "com.example.habittracker"

# Look for "alarm_verification_work"
```

### 6. Test Battery Optimization Status
```bash
# Check if app is exempt from battery optimization
adb shell dumpsys power | grep -A 5 "com.example.habittracker"

# Or check directly
adb shell "dumpsys deviceidle whitelist | grep habittracker"
```

### 7. Simulate App Update
```bash
# This will trigger MY_PACKAGE_REPLACED which should reschedule alarms
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Check logs for BootReceiver
adb logcat -s BootReceiver
```

## Manual Testing Steps

### Test 1: Basic Notification While Idle
1. Set a reminder for 5 minutes from now
2. Lock device screen
3. Wait 5 minutes
4. **Expected**: Notification appears and makes sound
5. **If fails**: Check battery optimization exemption

### Test 2: Notification After Reboot
1. Set a reminder for 10 minutes from now
2. Wait 2 minutes
3. Restart device: `adb reboot`
4. Wait for device to boot and app to initialize
5. Check logs: Should see "BootReceiver: Successfully rescheduled..."
6. Wait remaining time
7. **Expected**: Notification still appears

### Test 3: Multiple Reminders
1. Create 3 habits with reminders at different times
2. Note all scheduled times
3. Restart device
4. **Expected**: All 3 reminders trigger at correct times
5. Check logs: Should see all 3 habits rescheduled

### Test 4: Doze Mode (Overnight Test)
1. Set reminder for 8 hours from now (e.g., next morning)
2. Unplug device
3. Lock screen
4. Leave device untouched overnight
5. **Expected**: Notification appears in morning
6. This tests real Doze mode behavior

### Test 5: Battery Optimization Dialog
1. Fresh install or clear app data
2. Create a habit with reminder enabled
3. Close and reopen app
4. Wait 1.5 seconds
5. **Expected**: Battery optimization dialog appears
6. Test both "Open Settings" and "Not Now" paths
7. Verify dialog doesn't appear again after "Not Now"

### Test 6: Manufacturer-Specific Instructions
1. Run app on Xiaomi/Oppo/Samsung device (or change Build.MANUFACTURER)
2. Trigger battery optimization dialog
3. **Expected**: After main dialog, manufacturer-specific instructions appear
4. Verify instructions match device manufacturer

### Test 7: WorkManager Backup
1. Install app and create reminders
2. Force stop app: `adb shell am force-stop com.example.habittracker`
3. Wait 24 hours (or reduce interval for testing)
4. Check logs for AlarmVerificationWorker
5. **Expected**: Worker runs and reschedules alarms

## Debugging Common Issues

### Issue: Notifications Not Appearing
**Check:**
```bash
# Is battery optimization exempted?
adb shell dumpsys deviceidle whitelist | grep habittracker

# Are alarms scheduled?
adb shell dumpsys alarm | grep habittracker

# Are notifications enabled?
adb shell dumpsys notification | grep habittracker

# Check exact alarm permission (Android 12+)
adb shell cmd notification list_listeners
```

**Solutions:**
1. Manually grant battery optimization exemption
2. Grant exact alarm permission: Settings → Apps → Special app access → Alarms & reminders
3. Enable notifications: Settings → Apps → HabitTracker → Notifications

### Issue: Alarms Not Rescheduling After Boot
**Check:**
```bash
# Is BootReceiver registered?
adb shell dumpsys package com.example.habittracker | grep -A 5 "BootReceiver"

# Check boot logs
adb logcat -s BootReceiver

# Manually trigger boot event
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED
```

**Solutions:**
1. Verify RECEIVE_BOOT_COMPLETED permission in manifest
2. Check Hilt setup is correct
3. Verify app doesn't crash on boot

### Issue: WorkManager Not Running
**Check:**
```bash
# Check WorkManager status
adb shell dumpsys jobscheduler | grep -A 30 habittracker

# Check if work is scheduled
adb shell dumpsys activity service WorkManagerService
```

**Solutions:**
1. Check Hilt Work initialization
2. Verify WorkManager dependency is included
3. Check Application class implements Configuration.Provider
4. Run work manually for testing: See next section

### Force Run WorkManager (For Testing)
Create a test function in MainActivity or a debug screen:
```kotlin
fun testAlarmVerification() {
    val workRequest = OneTimeWorkRequestBuilder<AlarmVerificationWorker>()
        .build()
    WorkManager.getInstance(this).enqueue(workRequest)
}
```

## Performance Testing

### Battery Impact Test
1. Charge device to 100%
2. Install app with fixes
3. Set 5 reminders throughout the day
4. Use device normally
5. At end of day, check battery usage
6. **Expected**: App uses <1% battery

### Memory Leak Test
1. Open/close app 50 times
2. Navigate through all screens
3. Create/delete habits
4. Check for memory growth
5. Use Android Profiler in Android Studio

## Automated Testing Script
Create `test_notifications.sh`:
```bash
#!/bin/bash

echo "=== Notification Reliability Test Suite ==="

# Clear logs
adb logcat -c

# Test 1: Check battery optimization
echo "Test 1: Checking battery optimization..."
adb shell dumpsys power | grep habittracker

# Test 2: Check alarms
echo "Test 2: Checking scheduled alarms..."
adb shell dumpsys alarm | grep habittracker | head -20

# Test 3: Trigger boot event
echo "Test 3: Simulating boot..."
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED
sleep 3
adb logcat -d -s BootReceiver | tail -10

# Test 4: Check WorkManager
echo "Test 4: Checking WorkManager..."
adb shell dumpsys jobscheduler | grep habittracker | head -20

echo "=== Test Complete ==="
echo "Check logs above for any errors"
```

Run with: `bash test_notifications.sh`

## Stress Testing

### High Load Test
1. Create 50 habits with reminders
2. Set different times for each
3. Restart device
4. Monitor memory and CPU usage
5. Verify all alarms reschedule correctly

### Rapid Toggle Test
1. Enable reminder on a habit
2. Immediately disable it
3. Enable again
4. Repeat 20 times rapidly
5. **Expected**: No crashes, correct final state

### Network Failure Test
1. Turn off WiFi and mobile data
2. Set reminders
3. Restart device with no network
4. **Expected**: Alarms still reschedule (uses local database)

## Production Monitoring

### Metrics to Track
- Notification delivery rate
- Battery optimization exemption rate
- WorkManager success rate
- BootReceiver execution count
- Alarm scheduling failures

### User Feedback to Monitor
- "Not receiving notifications"
- "Notifications delayed"
- "Battery drain"
- "Alarms don't survive reboot"

## Quick Reference

| Test | Command | Expected Result |
|------|---------|----------------|
| Check battery opt | `adb shell dumpsys power \| grep habittracker` | App in whitelist |
| Check alarms | `adb shell dumpsys alarm \| grep habittracker` | Shows RTC_WAKEUP alarms |
| Trigger boot | `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED` | BootReceiver logs |
| Force Doze | `adb shell dumpsys deviceidle force-idle` | Device enters IDLE state |
| Exit Doze | `adb shell dumpsys deviceidle unforce` | Device exits IDLE state |
| Reboot | `adb reboot` | Device restarts cleanly |

## Success Criteria

✅ **Pass Criteria:**
- [ ] Notifications appear within 30 seconds of scheduled time
- [ ] Notifications work after device reboot
- [ ] Notifications work in Doze mode
- [ ] Battery usage < 1% per day
- [ ] No crashes in logs
- [ ] WorkManager runs every 24 hours
- [ ] Battery optimization dialog appears once
- [ ] Manufacturer instructions appear on applicable devices

❌ **Fail Criteria:**
- Notifications delayed > 5 minutes
- Notifications missing after reboot
- App crashes on boot
- Battery drain > 5%
- WorkManager not running

## Getting Help

If tests fail:
1. Check logs with commands above
2. Review NOTIFICATION_RELIABILITY_FIX.md
3. Verify all permissions in AndroidManifest.xml
4. Check Hilt setup in Application class
5. Test on a different device/Android version
