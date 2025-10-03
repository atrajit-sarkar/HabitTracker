# Debug Language Issue - Step by Step 🔍

If language is **STILL not changing** after the fix, follow these debugging steps:

## Step 1: Verify Arabic Strings Exist

Check if the Arabic strings file is properly included in the APK:

```powershell
# Check if values-ar folder exists
Test-Path "app\src\main\res\values-ar\strings.xml"
```

Should return: `True`

## Step 2: Verify Language Preference is Saved

The app saves the language choice to SharedPreferences. Let's verify:

### Add Logging to LanguageManager

Temporarily add logs to see what's happening:

1. Open: `app\src\main\java\com\example\habittracker\util\LanguageManager.kt`

2. Add these imports at the top:
```kotlin
import android.util.Log
```

3. Update `setLanguage()` function to add logs:
```kotlin
fun setLanguage(context: Context, language: Language) {
    Log.d("LanguageManager", "=== SETTING LANGUAGE ===")
    Log.d("LanguageManager", "Selected: ${language.code} (${language.nativeName})")
    
    // Save preference
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    
    Log.d("LanguageManager", "Saved to SharedPreferences: ${language.code}")
    
    // Apply locale change
    setLocale(context, language.code)
    
    Log.d("LanguageManager", "Locale applied")
    
    // Force activity recreation for immediate effect
    if (context is Activity) {
        Log.d("LanguageManager", "Recreating activity...")
        context.recreate()
    } else {
        Log.e("LanguageManager", "Context is not an Activity!")
    }
}
```

4. Add log in `setLocale()`:
```kotlin
private fun setLocale(context: Context, languageCode: String) {
    Log.d("LanguageManager", "setLocale called with: $languageCode")
    
    val locale = Locale(languageCode)
    Locale.setDefault(locale)
    
    Log.d("LanguageManager", "Default locale set to: ${Locale.getDefault().language}")
    
    // ... rest of the code
}
```

5. Rebuild and install:
```powershell
.\gradlew installDebug
```

## Step 3: Check Logs

After changing language, check the logs. You should see:

```
LanguageManager: === SETTING LANGUAGE ===
LanguageManager: Selected: ar (العربية)
LanguageManager: Saved to SharedPreferences: ar
LanguageManager: setLocale called with: ar
LanguageManager: Default locale set to: ar
LanguageManager: Locale applied
LanguageManager: Recreating activity...
```

### If you see "Context is not an Activity":
- The Activity context wasn't passed correctly
- Check LanguageSelectorScreen is passing `activity` not `context`

### If logs don't appear at all:
- Language selection click handler isn't being called
- Check if the card's onClick is properly wired

## Step 4: Verify attachBaseContext is Called

Add log to MainActivity's `attachBaseContext()`:

```kotlin
override fun attachBaseContext(newBase: Context) {
    Log.d("MainActivity", "=== ATTACH BASE CONTEXT ===")
    
    // Apply saved language before attaching context
    val prefs = newBase.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
    val languageCode = prefs.getString("selected_language", "en") ?: "en"
    
    Log.d("MainActivity", "Loaded language from prefs: $languageCode")
    
    val locale = Locale(languageCode)
    Locale.setDefault(locale)
    
    Log.d("MainActivity", "Default locale: ${Locale.getDefault().language}")
    
    // ... rest of code
    
    Log.d("MainActivity", "Context attached with locale: $languageCode")
    super.attachBaseContext(context)
}
```

Rebuild, then look for these logs when app starts:

```
MainActivity: === ATTACH BASE CONTEXT ===
MainActivity: Loaded language from prefs: ar
MainActivity: Default locale: ar
MainActivity: Context attached with locale: ar
```

## Step 5: Verify String Resources Load Correctly

Add a test in ProfileScreen to log the actual string value:

In ProfileScreen.kt, add this inside the composable:

```kotlin
LaunchedEffect(Unit) {
    val languageSettingsString = context.getString(R.string.language_settings)
    Log.d("ProfileScreen", "language_settings resource: '$languageSettingsString'")
    Log.d("ProfileScreen", "Current locale: ${context.resources.configuration.locales[0]}")
}
```

After changing to Arabic, you should see:

```
ProfileScreen: language_settings resource: 'إعدادات اللغة'
ProfileScreen: Current locale: ar
```

If you see:
```
ProfileScreen: language_settings resource: 'Language Settings'
ProfileScreen: Current locale: ar
```

This means locale is set but strings aren't loading correctly - there's an issue with the resource loading.

## Step 6: Nuclear Option - Force Clean Build

Sometimes Android Studio/Gradle caches cause issues:

```powershell
# Stop Gradle daemon
.\gradlew --stop

# Clean build
.\gradlew clean

# Delete build folders
Remove-Item -Recurse -Force app\build
Remove-Item -Recurse -Force build

# Clear Gradle cache (optional, takes time)
Remove-Item -Recurse -Force $env:USERPROFILE\.gradle\caches

# Rebuild
.\gradlew installDebug
```

## Step 7: Check APK Contents

Verify the APK actually contains Arabic resources:

```powershell
# Build release APK (easier to inspect)
.\gradlew assembleDebug

# APK is at: app\build\outputs\apk\debug\app-debug.apk
```

Extract the APK (rename to .zip and extract) and check for:
- `res/values-ar/strings.xml` should exist
- Open it and verify Arabic strings are there

## Step 8: Device Locale Override

Some devices have a "Force RTL layout" developer option that might interfere:

1. Go to device Settings
2. Developer Options
3. Check if "Force RTL layout direction" is enabled
4. Try toggling it off/on

## Step 9: Check Android Version Compatibility

If using Android 15 (API 35), try this alternative in attachBaseContext:

```kotlin
override fun attachBaseContext(newBase: Context) {
    val prefs = newBase.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
    val languageCode = prefs.getString("selected_language", "en") ?: "en"
    
    val locale = Locale.forLanguageTag(languageCode)  // Use forLanguageTag instead
    Locale.setDefault(locale)
    
    val configuration = Configuration(newBase.resources.configuration)
    configuration.setLocale(locale)
    
    val context = newBase.createConfigurationContext(configuration)
    
    super.attachBaseContext(context)
}
```

## Step 10: Verify LanguageSelectorScreen is Using Activity

Check this line in LanguageSelectorScreen.kt:

```kotlin
val activity = context as? Activity
```

And when setting language:

```kotlin
activity?.let {
    LanguageManager.setLanguage(it, language)
}
```

If `activity` is null, it won't work. Add a log:

```kotlin
activity?.let {
    Log.d("LanguageSelector", "Activity found, setting language")
    LanguageManager.setLanguage(it, language)
} ?: Log.e("LanguageSelector", "Activity is NULL! Cannot set language")
```

## Diagnostic Checklist

Run through this checklist and report which ones pass:

- [ ] `values-ar/strings.xml` file exists
- [ ] Arabic strings are visible in the file (not corrupted)
- [ ] App builds without errors
- [ ] APK installs successfully
- [ ] Can open Language Selector screen
- [ ] Can tap on Arabic option
- [ ] Screen flashes/recreates when tapped
- [ ] Logs show "SETTING LANGUAGE" message
- [ ] Logs show "Recreating activity" message
- [ ] Logs show "ATTACH BASE CONTEXT" message
- [ ] Logs show language code "ar" loaded
- [ ] String resource logs show Arabic text
- [ ] Locale logs show "ar"

## If NOTHING Works

Try this minimal test:

1. Create a simple test in MainActivity:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // FORCE Arabic for testing
    val locale = Locale("ar")
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    resources.updateConfiguration(config, resources.displayMetrics)
    
    // Log what we get
    val testString = getString(R.string.home_title)
    Log.d("TEST", "home_title: $testString")
    Log.d("TEST", "Should be: عاداتك")
    
    // Continue normal onCreate...
}
```

2. Rebuild and check logs:

**If you see:**
```
TEST: home_title: عاداتك
TEST: Should be: عاداتك
```
→ Strings work! Issue is with the language switching logic.

**If you see:**
```
TEST: home_title: Your Habits
TEST: Should be: عاداتك
```
→ Arabic strings aren't being loaded at all. Resource loading issue.

## Common Issues and Fixes

### Issue: "Activity context is null"
**Fix:** Ensure LanguageSelectorScreen uses:
```kotlin
val activity = context as? Activity
```

### Issue: "Locale shows 'ar' but text is English"
**Fix:** Resources aren't reloading. Try:
```kotlin
context.resources.updateConfiguration(config, context.resources.displayMetrics)
```

### Issue: "Some text changes, some doesn't"
**Fix:** Those are hardcoded strings. Focus on testing screens we know are fully refactored (Language Selector, Profile).

### Issue: "RTL works but text is English"
**Fix:** Locale direction is applied but language isn't. Check if `values-ar` folder is in the APK.

---

## Report Back

After running these diagnostics, report:

1. Which logs appear?
2. What do the string resource logs show?
3. Does the minimal test work?
4. Any error messages in logcat?

This will help identify exactly where the issue is!
