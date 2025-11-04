# 6-State Icon System Implementation

## Overview
Implemented a comprehensive 6-state icon system for the **Default icon selection** while maintaining simple warning/angry logic for custom theme icons (anime, sitama, bird).

## Icon States (Default Icon Only)

### 1. DEFAULT
- **Trigger**: User completed a due task
- **Icon**: `default.png` → `ic_launcher_default.png`
- **Activity Alias**: `it.atraj.habittracker.MainActivity.Default`

### 2. FREEZE
- **Trigger**: New day, before reaching any due habit
- **Icon**: `cold-shiver-default.png` → `ic_launcher_freeze.png`
- **Activity Alias**: `it.atraj.habittracker.MainActivity.Freeze`

### 3. GLITCH
- **Trigger**: Reached a due habit (habit is due now, 0-2 hours overdue)
- **Icon**: `glitched-away-default.png` → `ic_launcher_glitch.png`
- **Activity Alias**: `it.atraj.habittracker.MainActivity.Glitch`

### 4. WARNING
- **Trigger**: Any habit is 2+ hours overdue
- **Icon**: `warning-default.png` → `ic_launcher_warning.png`
- **Activity Alias**: `it.atraj.habittracker.MainActivity.Warning`

### 5. ANGRY
- **Trigger**: Any habit is 4+ hours overdue
- **Icon**: `angry-default.png` → `ic_launcher_angry.png`
- **Activity Alias**: `it.atraj.habittracker.MainActivity.Angry`

### 6. ALL_DONE
- **Trigger**: All habits for that day are completed
- **Icon**: `alldone.png` → `ic_launcher_alldone.png`
- **Activity Alias**: `it.atraj.habittracker.MainActivity.AllDone`

## Custom Icons (Simple System)
For **anime**, **sitama**, and **bird** icons, only use:
- Base icon (when no overdue)
- WARNING icon (2+ hours overdue)
- ANGRY icon (4+ hours overdue)

## Files Modified

### 1. PowerShell Script
- **File**: `convert-default-state-icons.ps1`
- **Purpose**: Convert 6 state icons from `icons/default/` folder to Android resources
- **Generated**: 30 PNG files (6 icons × 5 densities)
  - mdpi (48px), hdpi (72px), xhdpi (96px), xxhdpi (144px), xxxhdpi (192px)

### 2. Enum Definition
- **File**: `app/src/main/java/com/example/habittracker/util/OverdueHabitChecker.kt`
- **Changes**:
  ```kotlin
  enum class IconState {
      DEFAULT,      // Task completed
      FREEZE,       // New day, no due habits reached
      GLITCH,       // Reached a due habit
      WARNING,      // 2+ hours overdue
      ANGRY,        // 4+ hours overdue (renamed from CRITICAL_WARNING)
      ALL_DONE      // All habits completed
  }
  ```

### 3. State Detection Logic
- **File**: `app/src/main/java/com/example/habittracker/util/OverdueHabitChecker.kt`
- **Method**: `determineIconState()`
- **Priority Order**:
  1. Check if all habits completed → ALL_DONE
  2. Check overdue hours:
     - 4+ hours → ANGRY
     - 2+ hours → WARNING
     - 0-2 hours → GLITCH
  3. Check if habits scheduled for today → FREEZE
  4. Default → DEFAULT

### 4. Manifest Configuration
- **File**: `app/src/main/AndroidManifest.xml`
- **Added Aliases**:
  - `MainActivity.Default` (enabled by default)
  - `MainActivity.Freeze`
  - `MainActivity.Glitch`
  - `MainActivity.Warning`
  - `MainActivity.Angry`
  - `MainActivity.AllDone`
- **Removed Aliases**:
  - `MainActivity.WarningDefault`
  - `MainActivity.AngryDefault`
  - `MainActivity.Atrajit`
  - `MainActivity.WarningAtrajit`
  - `MainActivity.AngryAtrajit`

### 5. Icon Manager
- **File**: `app/src/main/java/com/example/habittracker/service/AppIconManager.kt`
- **Changes**:
  - Removed "atrajit" from `ICON_ALIASES`
  - Added `DEFAULT_STATE_ALIASES` set (6 states)
  - Added `CUSTOM_THEME_OVERDUE_ALIASES` set (warning/angry for anime, sitama, bird)

### 6. Overdue Icon Manager
- **File**: `app/src/main/java/com/example/habittracker/service/OverdueHabitIconManager.kt`
- **Changes**:
  - Updated companion object constants (removed Atrajit, added Freeze, Glitch, AllDone)
  - Rewrote `updateAppIcon()` to handle 6-state system for default, simple system for custom
  - Rewrote `updateAppIconSafely()` to match new logic
  - Updated `checkAndUpdateIcon()` to pass additional parameters to state detection

### 7. UI Screen
- **File**: `app/src/main/java/com/example/habittracker/ui/profile/AppIconSelectionScreen.kt`
- **Changes**: Removed "atrajit" option from icon selection list

## Resources Deleted
To reduce APK size, deleted old icon files:
- `ic_launcher_warning_default.png` (5 densities = 5 files)
- `ic_launcher_angry_default.png` (5 densities = 5 files)
- `ic_launcher_atrajit.png` (5 densities = 5 files)
- `ic_launcher_warning_atrajit.png` (5 densities = 5 files)
- `ic_launcher_angry_atrajit.png` (5 densities = 5 files)
- **Total**: 25 files deleted

## Resources Created
New icon files in `app/src/main/res/`:
- `mipmap-mdpi/ic_launcher_default.png`
- `mipmap-mdpi/ic_launcher_freeze.png`
- `mipmap-mdpi/ic_launcher_glitch.png`
- `mipmap-mdpi/ic_launcher_warning.png`
- `mipmap-mdpi/ic_launcher_angry.png`
- `mipmap-mdpi/ic_launcher_alldone.png`
- (Same pattern for hdpi, xhdpi, xxhdpi, xxxhdpi)
- **Total**: 30 files created

## Net Resource Impact
- Files created: 30
- Files deleted: 25
- **Net increase**: 5 files (for enhanced functionality)

## State Transition Flow

```
App Launch
    ↓
[FREEZE] - New day, habits not yet due
    ↓
User reaches habit due time
    ↓
[GLITCH] - Habit is due now
    ↓
User completes habit
    ↓
[DEFAULT] - Task completed
    ↓
(If all habits completed)
    ↓
[ALL_DONE] - Perfect day!

OR (if habit not completed):

[GLITCH] - Habit due
    ↓
Wait 2 hours
    ↓
[WARNING] - 2+ hours overdue
    ↓
Wait 2 more hours
    ↓
[ANGRY] - 4+ hours overdue
```

## Testing Checklist

### Default Icon Selected
- [ ] New day starts → Icon changes to FREEZE
- [ ] Habit becomes due → Icon changes to GLITCH
- [ ] Complete a habit → Icon changes to DEFAULT
- [ ] Complete all habits → Icon changes to ALL_DONE
- [ ] Habit 2 hours overdue → Icon changes to WARNING
- [ ] Habit 4 hours overdue → Icon changes to ANGRY

### Custom Icons Selected (anime/sitama/bird)
- [ ] No overdue habits → Shows base icon
- [ ] Habit 2 hours overdue → Shows warning variant
- [ ] Habit 4 hours overdue → Shows angry variant
- [ ] FREEZE, GLITCH, ALL_DONE states ignored (shows base icon instead)

## Build Instructions
```powershell
# Clean and rebuild
./gradlew clean
./gradlew assembleDebug

# Or use build script
./build-optimized-release.ps1
```

## Known Limitations
1. Icon changes require launcher refresh (some launchers cache icons)
2. State detection runs every 5 seconds (configurable delay in code)
3. FREEZE state only works if habits are scheduled for today

## Future Enhancements
- [ ] Add FREEZE/GLITCH/ALL_DONE variants for custom icons
- [ ] Reduce state detection delay for faster responses
- [ ] Add notification when icon changes state
- [ ] Add settings to enable/disable specific states

## Credits
- Default icon: From `icons/default/default.png`
- Freeze icon: From `icons/default/cold-shiver-default.png`
- Glitch icon: From `icons/default/glitched-away-default.png`
- Warning icon: From `icons/default/warning-default.png`
- Angry icon: From `icons/default/angry-default.png`
- All Done icon: From `icons/default/alldone.png`
