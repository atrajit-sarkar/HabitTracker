# 🚀 Quick Setup Guide - In-App Update System

## ✅ What's Been Implemented

Your app now has a **professional in-app update system** that:
- ✅ Automatically checks for updates from GitHub releases
- ✅ Shows a beautiful Material Design 3 changelog dialog
- ✅ Downloads and installs updates directly in-app
- ✅ Tracks download progress (0-100%)
- ✅ Allows users to Skip or postpone updates
- ✅ Falls back to browser if download fails

## 📋 How to Release Updates

### Step 1: Update Version Numbers
Edit `app/build.gradle.kts`:
```kotlin
defaultConfig {
    versionCode = 2        // Increment by 1 for each release
    versionName = "1.1.0"  // Follow semantic versioning
}
```

### Step 2: Build Release APK
```bash
.\gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

### Step 3: Create GitHub Release

1. **Create a Git tag**:
```bash
git tag -a v1.1.0 -m "Version 1.1.0 - Feature Update"
git push origin v1.1.0
```

2. **Go to GitHub Releases**:
   - Visit: https://github.com/atrajit-sarkar/HabitTracker/releases/new
   - Select tag: `v1.1.0`
   - Release title: `Version 1.1.0 - Feature Update`

3. **Write Changelog** (example):
```markdown
## 🎉 What's New in v1.1.0

### ✨ New Features
- 📲 In-app update system with automatic version checking
- 📊 Statistics and analytics dashboard
- 🎨 Enhanced notification setup guide

### 🐛 Bug Fixes
- Fixed notification delivery issues on idle devices
- Resolved dark mode inconsistencies
- Fixed profile picture sync issues

### 🔧 Improvements
- Optimized database performance
- Enhanced UI animations
- Better offline support

### 📱 Technical
- Updated to Android SDK 36
- Added WorkManager for reliability
- Improved battery optimization handling
```

4. **Upload APK**:
   - Drag and drop `app-release.apk` to the release assets
   - GitHub will host it automatically

5. **Publish Release** ✅

### Step 4: Test the Update

1. Install old version on device
2. Wait 2 seconds (or reopen app)
3. Update dialog should appear automatically
4. Click "Update Now" to test download/install flow

## 🎨 What Users See

### When Update is Available:
```
┌─────────────────────────────────────┐
│   [Icon]  Update Available          │
│   Version 1.1.0                     │
├─────────────────────────────────────┤
│   Current: 1.0.0  →  New: 1.1.0    │
│   📦 15.2 MB   📅 Oct 2, 2025      │
├─────────────────────────────────────┤
│   What's New                        │
│   • Feature 1                       │
│   • Feature 2                       │
│   • Bug fixes                       │
├─────────────────────────────────────┤
│   [  Update Now  ]                  │
│   [ Skip ]  [ Later ]               │
└─────────────────────────────────────┘
```

### During Download:
```
┌─────────────────────────────────────┐
│   [Rotating Icon]                   │
│   Downloading...                    │
├─────────────────────────────────────┤
│   Downloading...            67%     │
│   ████████████░░░░░░░░░░░░         │
│   Please don't close the app        │
└─────────────────────────────────────┘
```

## ⚙️ Configuration

### Update Check Frequency
Default: Every 24 hours

To change, edit `UpdateManager.kt`:
```kotlin
private const val CHECK_INTERVAL = 24 * 60 * 60 * 1000L // milliseconds
```

### Mandatory Updates
For critical updates that users MUST install:

In `MainActivity.kt`, change:
```kotlin
isMandatory = true  // Users can't skip or dismiss
```

### Repository Settings
Already configured for your repo:
```kotlin
private const val GITHUB_REPO_OWNER = "atrajit-sarkar"
private const val GITHUB_REPO_NAME = "HabitTracker"
```

## 🔒 Permissions Added

Automatically added to `AndroidManifest.xml`:
- ✅ `INTERNET` - Download APKs
- ✅ `REQUEST_INSTALL_PACKAGES` - Install updates

## 📦 Dependencies Added

Added to `gradle/libs.versions.toml`:
- ✅ `okhttp:4.12.0` - HTTP client for GitHub API

## 🧪 Testing Checklist

Before releasing v1.1.0:
- [ ] Update `versionCode` and `versionName`
- [ ] Build release APK
- [ ] Create GitHub release with changelog
- [ ] Upload APK to release
- [ ] Install v1.0.0 on device
- [ ] Open app and verify update dialog appears
- [ ] Test "Update Now" downloads and installs
- [ ] Test "Skip" hides dialog permanently for this version
- [ ] Test "Later" shows dialog again after 24 hours

## 🐛 Troubleshooting

### "Update dialog doesn't appear"
- Check if 24 hours passed since last check
- Verify internet connection
- Check logs: `adb logcat | grep UpdateManager`

### "Download fails"
- Verify APK is uploaded to GitHub release
- Check file name ends with `.apk`
- System will automatically open browser as fallback

### "APK won't install"
- Use release build (not debug)
- Ensure APK is signed
- Check FileProvider configuration

## 📊 User Behavior

- **Update Now**: Downloads → Installs → App restarts
- **Skip**: Never shows this version again
- **Later**: Shows again after 24 hours
- **No Internet**: Silent fail, checks again later

## 🎯 Best Practices

1. **Semantic Versioning**: Use MAJOR.MINOR.PATCH
   - MAJOR: Breaking changes (2.0.0)
   - MINOR: New features (1.1.0)
   - PATCH: Bug fixes (1.0.1)

2. **Write Good Changelogs**:
   - Group by type (Features, Fixes, Improvements)
   - Use emojis for visual appeal
   - Be specific and user-friendly

3. **Test Before Release**:
   - Always test update flow on real device
   - Check both WiFi and mobile data
   - Test on different Android versions

4. **Version Control**:
   - Never skip version numbers
   - Tag every release in Git
   - Keep `versionCode` incrementing

## 📈 Next Release Example

For v1.2.0:

1. **Update `build.gradle.kts`**:
```kotlin
versionCode = 3
versionName = "1.2.0"
```

2. **Build**:
```bash
.\gradlew assembleRelease
```

3. **Tag**:
```bash
git tag -a v1.2.0 -m "Version 1.2.0"
git push origin v1.2.0
```

4. **Create GitHub Release** with changelog

5. **Upload APK** from `app/build/outputs/apk/release/`

6. **Publish** ✅

Users on v1.0.0 and v1.1.0 will automatically see the update!

## 🎉 What's Next?

Your app now has **production-ready** update system! Features:
- ✅ Professional UI with Material Design 3
- ✅ Automatic version checking
- ✅ Beautiful changelog display
- ✅ Progress tracking
- ✅ User choice (Update/Skip/Later)
- ✅ Fallback mechanisms
- ✅ Error handling

Just create releases on GitHub and users will be notified automatically! 🚀

---

**Implementation Date**: October 2, 2025
**Status**: ✅ Production Ready
**Build Status**: ✅ BUILD SUCCESSFUL
