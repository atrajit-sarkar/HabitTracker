# Habit Done Animation - Debugging Guide

## Testing Instructions

1. **Open the app** on your device
2. **Mark a habit as complete** by clicking the "Done" button
3. **Check logcat** for messages with tag "HabitDoneAnim"

## Expected Log Sequence

When you click "Done", you should see these logs in order:

```
D/HabitDoneAnim: Animation triggered for habit ID: [number]
D/HabitDoneAnim: State changed to: [number]
D/HabitDoneAnim: Showing overlay for habit ID: [number]
D/HabitDoneAnim: HabitDoneAnimationOverlay composable entered
D/HabitDoneAnim: Lottie composition loaded: true
D/HabitDoneAnim: Animation sequence started
D/HabitDoneAnim: Phase 0: Fade in
D/HabitDoneAnim: Phase 1: Hold
D/HabitDoneAnim: Phase 2: Fade out
D/HabitDoneAnim: Animation complete
D/HabitDoneAnim: Overlay complete, resetting state
D/HabitDoneAnim: State changed to: null
```

## Troubleshooting by Log Output

### Scenario 1: No logs at all
**Problem:** onClick handler not being called
**Possible causes:**
- App not using the updated version
- Button disabled or not clickable
- Habit already completed

### Scenario 2: Logs stop at "Animation triggered"
**Problem:** State not updating properly
**Check:** Ensure habit ID is valid

### Scenario 3: "Lottie composition loaded: false"
**Problem:** Asset file not found or corrupted
**Fix:** Verify `habit_done_anim.json` exists in `app/src/main/assets/`
**Command:** `Get-ChildItem "app\src\main\assets\habit_done_anim.json"`

### Scenario 4: Animation overlay not visible
**Problem:** Overlay is rendered but not visible
**Possible causes:**
- Alpha animation not working
- Overlay behind other UI elements
- Background color too transparent

### Scenario 5: Logs show everything but no visual
**Problem:** Rendering issue
**Try:**
- Increase animation size from 300.dp to 500.dp
- Change background alpha from 0.3f to 0.8f (darker)
- Check if other overlays work (Sharingan for Itachi theme)

## Quick Fixes to Try

### Fix 1: Make overlay more visible
In `HabitDoneAnimationOverlay`, change:
```kotlin
.background(Color.Black.copy(alpha = alpha * 0.3f))
```
to:
```kotlin
.background(Color.Black.copy(alpha = alpha * 0.7f))
```

### Fix 2: Increase animation size
Change:
```kotlin
.size(300.dp)
```
to:
```kotlin
.size(400.dp)
```

### Fix 3: Test with simpler animation
Replace the Lottie animation temporarily with a simple colored box:
```kotlin
Box(
    modifier = Modifier
        .size(300.dp)
        .background(Color.Red, CircleShape)
)
```

## Viewing Logcat

### Using Android Studio:
1. Open **Logcat** tab at bottom
2. Filter by "HabitDoneAnim"
3. Click "Done" button in app
4. Watch logs appear in real-time

### Using ADB Command:
```powershell
adb logcat -s HabitDoneAnim
```

## Current Implementation Summary

**File:** `habit_done_anim.json`
- Size: 245.98 KB
- Frames: 18
- FPS: 12
- Duration: 1494ms (loops)

**Animation Timing:**
- Fade in: 300ms
- Hold: 3400ms  
- Fade out: 300ms
- **Total: 4 seconds**

**Overlay Properties:**
- Size: 300dp × 300dp
- Position: Center screen
- Background: Black with 0.3 alpha
- Blocks interaction: Yes
