# Fix Duplicate App Icons Issue

## The Problem
Multiple launcher activities are enabled in the manifest, causing duplicate app icons to appear.

## Quick Fix - Method 1: Uninstall and Reinstall

This is the **easiest and fastest** solution:

```powershell
# 1. Uninstall BOTH app versions
# On your device: Settings → Apps → Habit Tracker (all versions) → Uninstall

# 2. Clean build
.\gradlew.bat clean

# 3. Reinstall fresh
.\gradlew.bat installPlaystoreRelease
.\gradlew.bat installGithubRelease
```

## Quick Fix - Method 2: Reset Icon State Programmatically

Add this code to your app and run it once:

```kotlin
// Add this to MainActivity onCreate() temporarily
private fun resetIconState() {
    val packageManager = packageManager
    val context = this
    
    // Disable ALL activity aliases
    val aliases = listOf(
        ".MainActivity.Default",
        ".MainActivity.WarningDefault",
        ".MainActivity.AngryDefault",
        ".MainActivity.Warning",
        ".MainActivity.Angry",
        ".MainActivity.Anime",
        ".MainActivity.Bird",
        ".MainActivity.Dog",
        ".MainActivity.Cat",
        ".MainActivity.Lion",
        ".MainActivity.Tiger",
        ".MainActivity.Panda",
        ".MainActivity.Custom1",
        ".MainActivity.Custom2",
        ".MainActivity.Custom3",
        ".MainActivity.AnimeWarning",
        ".MainActivity.AnimeAngry"
    )
    
    aliases.forEach { alias ->
        try {
            val component = ComponentName(context, alias)
            packageManager.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d("IconFix", "Disabled: $alias")
        } catch (e: Exception) {
            Log.e("IconFix", "Failed to disable $alias", e)
        }
    }
    
    Toast.makeText(this, "Icon state reset! Restart app.", Toast.LENGTH_LONG).show()
}
```

Then:
1. Build and install the app
2. Open the app (it will disable all aliases)
3. Restart the device
4. Remove the resetIconState() code
5. Rebuild and reinstall

## Permanent Prevention

The issue might be caused by:

1. **Icon changing code running multiple times**
2. **Race condition in enabling/disabling aliases**
3. **App being killed during icon change**

To prevent this, ensure:
- Only ONE alias is enabled at a time
- MainActivity itself does NOT have LAUNCHER intent-filter when aliases are used
- Icon changes happen atomically (enable new → disable others)

## Recommended Solution

**Just uninstall and reinstall the app.** This is the cleanest fix!
