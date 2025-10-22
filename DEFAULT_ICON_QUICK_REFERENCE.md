# Quick Reference: Default Icon Changes

## What Changed?

### 1. Default App Icon
- **Old**: `ic_launcher.webp` (generic Android icon)
- **New**: `ic_launcher_default.png` (custom default.jpg from icons folder)
- **Location**: `app/src/main/res/mipmap-*/ic_launcher_default.png`

### 2. Overdue Icon Variants
- **Warning**: `ic_launcher_warning_default.png` (from default-warning.jpg)
- **Angry**: `ic_launcher_angry_default.png` (from default-angry.jpg)

### 3. Notification Icons
- **Old**: Hardcoded `R.drawable.ic_notification_habit`
- **New**: Dynamically uses current launcher icon
- **Benefit**: Notifications now match the app's current icon

### 4. Icon Selection
Users can now select "Default" icon from the app icon selection screen, and it will:
- Display the custom default icon
- Change to warning icon when habits are slightly overdue
- Change to angry icon when habits are critically overdue
- Show in notifications when default icon is active

## Icon Conversion Script

Run `.\convert-default-icons.ps1` to regenerate icons if needed.

## File Locations

### Source Icons (icons folder)
```
icons/
├── default.jpg           → Main default icon
├── default-warning.jpg   → Warning state
└── default-angry.jpg     → Angry state
```

### Generated Resources
```
app/src/main/res/
├── mipmap-mdpi/
│   ├── ic_launcher_default.png
│   ├── ic_launcher_warning_default.png
│   └── ic_launcher_angry_default.png
├── mipmap-hdpi/
│   ├── ic_launcher_default.png
│   ├── ic_launcher_warning_default.png
│   └── ic_launcher_angry_default.png
├── mipmap-xhdpi/
│   ├── ic_launcher_default.png
│   ├── ic_launcher_warning_default.png
│   └── ic_launcher_angry_default.png
├── mipmap-xxhdpi/
│   ├── ic_launcher_default.png
│   ├── ic_launcher_warning_default.png
│   └── ic_launcher_angry_default.png
└── mipmap-xxxhdpi/
    ├── ic_launcher_default.png
    ├── ic_launcher_warning_default.png
    └── ic_launcher_angry_default.png
```

## Code Changes Summary

### AndroidManifest.xml
- Main app icon: `@mipmap/ic_launcher_default`
- Added aliases: `MainActivity.Default`, `MainActivity.WarningDefault`, `MainActivity.AngryDefault`

### AppIconManager.kt
- Default alias: `"it.atraj.habittracker.MainActivity.Default"`
- Added to ICON_ALIASES and OVERDUE_ICON_ALIASES

### OverdueHabitIconManager.kt
- Added constants: `WARNING_DEFAULT_ACTIVITY`, `ANGRY_DEFAULT_ACTIVITY`
- Updated all icon switching logic to support default overdue states

### HabitReminderService.kt
- New function: `getCurrentLauncherIconResource()`
- Notifications now use current launcher icon dynamically

### AppIconSelectionScreen.kt
- Updated default option to use `ic_launcher_default`
- Updated alias to `MainActivity.Default`

## APK Size Impact
- **Removed**: 10 old WebP files (ic_launcher.webp and ic_launcher_round.webp)
- **Added**: 15 new PNG files (default icon variants)
- **Net Impact**: Approximately neutral or slightly reduced (depends on source image sizes)

## Testing Checklist
- [ ] Select default icon from app icon selection
- [ ] Verify icon displays correctly on launcher
- [ ] Create overdue habit and check warning icon appears
- [ ] Create critically overdue habit and check angry icon appears
- [ ] Check notification shows current launcher icon
- [ ] Restart app and verify icon persists
- [ ] Switch between icons and verify smooth transition

## Rollback Instructions
If issues occur:
1. Revert AndroidManifest.xml changes
2. Restore old `ic_launcher.webp` files from git history
3. Revert code changes in AppIconManager.kt, OverdueHabitIconManager.kt, HabitReminderService.kt
4. Clean and rebuild project

---

**Quick Command to Verify Icon Files:**
```powershell
Get-ChildItem -Recurse app/src/main/res/mipmap-* -Filter "*default.png"
```

**Expected Output:** 15 files (3 icons × 5 densities)
