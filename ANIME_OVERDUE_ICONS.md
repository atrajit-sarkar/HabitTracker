# Anime-Themed Overdue Icons

## Overview
When a user selects the anime icon as their app icon, the overdue habit warning and critical warning icons will automatically use anime-themed versions instead of the default warning/angry icons.

## Implementation

### Icon Files
- **Source icons** (in `icons/` folder):
  - `anime.png` - Main anime app icon
  - `angry-anime.png` - Critical warning anime icon
  - `warning-anime.png` - Warning anime icon

- **Generated icons** (in `app/src/main/res/mipmap-*/`):
  - `ic_launcher_anime.png` - All densities (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)
  - `ic_launcher_angry_anime.png` - All densities
  - `ic_launcher_warning_anime.png` - All densities

### Android Manifest
Added activity aliases for anime-themed overdue icons:
- `.MainActivity.WarningAnime` - Uses `ic_launcher_warning_anime`
- `.MainActivity.AngryAnime` - Uses `ic_launcher_angry_anime`

### Code Changes

#### OverdueHabitIconManager.kt
- Added constants for anime-themed activities:
  ```kotlin
  private const val WARNING_ANIME_ACTIVITY = "it.atraj.habittracker.MainActivity.WarningAnime"
  private const val ANGRY_ANIME_ACTIVITY = "it.atraj.habittracker.MainActivity.AngryAnime"
  ```

- Updated `updateAppIcon()` function to check user's selected icon:
  - For WARNING state: Uses `WARNING_ANIME_ACTIVITY` if user has selected anime icon
  - For CRITICAL_WARNING state: Uses `ANGRY_ANIME_ACTIVITY` if user has selected anime icon
  - Otherwise: Uses default warning/angry icons

## Behavior

### When User Selects Anime Icon
1. App icon changes to anime theme
2. If habits become overdue:
   - **1-2 hours overdue**: App icon changes to warning-anime icon
   - **2+ hours overdue**: App icon changes to angry-anime icon

### When User Selects Other Icons
1. App icon changes to selected icon (default, custom1, custom2, ni, etc.)
2. If habits become overdue:
   - **1-2 hours overdue**: App icon changes to default warning icon
   - **2+ hours overdue**: App icon changes to default angry icon

## Icon Conversion Script

The `convert-anime-icons.ps1` PowerShell script converts source PNG files to Android launcher icons:
- Resizes images for all Android density folders (mdpi through xxxhdpi)
- Uses high-quality bicubic interpolation
- Automatically creates output directories if they don't exist

To regenerate icons after updating source files:
```powershell
powershell -ExecutionPolicy Bypass -File .\convert-anime-icons.ps1
```

## Files Modified
1. `app/src/main/java/com/example/habittracker/service/OverdueHabitIconManager.kt`
2. `app/src/main/AndroidManifest.xml` (already had the aliases)
3. All mipmap-* folders (icon assets regenerated)

## Testing
1. Select anime icon from app icon selector
2. Create a habit with a reminder time that has already passed
3. Wait for overdue check (runs every 15 minutes via WorkManager)
4. Observe:
   - Warning-anime icon appears for 1-2 hours overdue
   - Angry-anime icon appears for 2+ hours overdue
5. Complete the habit to verify icon returns to normal anime icon
