# 🚀 Dual-Flavor Build System - v7.0.0

## 📋 Overview

The app now supports **two build flavors** that allow you to maintain separate versions for GitHub releases and Play Store distribution:

| Flavor | Version | In-App Updates | Update Source | Use Case |
|--------|---------|----------------|---------------|----------|
| **GitHub** | 7.0.0-github | ✅ Enabled | GitHub Releases | Direct distribution to users |
| **PlayStore** | 7.0.0 | ❌ Disabled | Google Play Store | Official Play Store release |

---

## 🎯 Why Two Flavors?

### GitHub Flavor
- **In-app update system** that checks GitHub releases every 24 hours
- Users get instant notifications when new versions are available
- Perfect for beta testers and direct distribution
- Includes GitHub API integration for release checking

### Play Store Flavor
- **Clean build** without GitHub update dependencies
- Google Play Store handles all updates automatically
- Complies with Play Store policies (no external update systems)
- Optimized for official distribution

---

## 🔨 Building the App

### Quick Build Commands

#### Option 1: PowerShell Scripts (Recommended)

**Build GitHub Version:**
```powershell
.\build-github-release.ps1
```
Output: `app\build\outputs\apk\github\release\app-github-release.apk`

**Build Play Store Version:**
```powershell
.\build-playstore-release.ps1
```
Output: `app\build\outputs\apk\playstore\release\app-playstore-release.apk`

**Build Both Versions:**
```powershell
.\build-both-versions.ps1
```

#### Option 2: Gradle Commands

**Build GitHub Version:**
```bash
.\gradlew.bat assembleGithubRelease
```

**Build Play Store Version:**
```bash
.\gradlew.bat assemblePlaystoreRelease
```

**Build Both:**
```bash
.\gradlew.bat assembleRelease
```

---

## 📦 Version Information

| Property | Value |
|----------|-------|
| Version Name (GitHub) | 7.0.0-github |
| Version Name (Play Store) | 7.0.0 |
| Version Code | 20 |
| Min SDK | 29 (Android 10) |
| Target SDK | 36 |
| Application ID | it.atraj.habittracker |

---

## 🔧 Technical Implementation

### Build Configuration (`app/build.gradle.kts`)

```kotlin
flavorDimensions += "version"
productFlavors {
    create("github") {
        dimension = "version"
        buildConfigField("Boolean", "IS_GITHUB_VERSION", "true")
        buildConfigField("Boolean", "ENABLE_IN_APP_UPDATE", "true")
        versionNameSuffix = "-github"
    }
    
    create("playstore") {
        dimension = "version"
        buildConfigField("Boolean", "IS_GITHUB_VERSION", "false")
        buildConfigField("Boolean", "ENABLE_IN_APP_UPDATE", "false")
    }
}
```

### Runtime Detection

The app automatically detects which flavor is running:

```kotlin
if (BuildConfig.ENABLE_IN_APP_UPDATE) {
    // Show update check button
    // Enable automatic update checks
} else {
    // Hide update features
    // Rely on Play Store updates
}
```

### Update Manager Behavior

**GitHub Flavor:**
- Checks for updates every 24 hours
- Shows "Check for Updates" button in profile
- Downloads and installs APK from GitHub releases
- Displays changelog and update dialog

**Play Store Flavor:**
- All update checks return `null`
- Update button hidden in UI
- `shouldCheckForUpdate()` always returns `false`
- No GitHub API calls made

---

## 📱 Distribution Workflow

### GitHub Release Distribution

1. **Build the GitHub version:**
   ```powershell
   .\build-github-release.ps1
   ```

2. **Test the APK** on a physical device

3. **Create GitHub Release:**
   - Go to: https://github.com/atrajit-sarkar/HabitTracker/releases/new
   - Tag: `v7.0.0`
   - Title: `Habit Tracker v7.0.0`
   - Upload: `app-github-release.apk`
   - Add changelog and release notes
   - Mark as latest release

4. **User Experience:**
   - App checks for updates every 24 hours
   - Users see update notification dialog
   - One-tap download and install
   - Changelog displayed in-app

### Play Store Distribution

1. **Build the Play Store version:**
   ```powershell
   .\build-playstore-release.ps1
   ```

2. **Test the APK** on a physical device

3. **Upload to Play Console:**
   - Go to: https://play.google.com/console
   - Select your app
   - Create new release (Production/Beta/Alpha)
   - Upload `app-playstore-release.apk`
   - Fill in release notes
   - Submit for review

4. **User Experience:**
   - Google Play handles all updates
   - No in-app update dialogs
   - Standard Play Store update flow
   - Play Store handles rollouts and staging

---

## 🎨 UI Differences

### GitHub Flavor
- ✅ "Check for Updates" button visible in Profile screen
- ✅ Update dialogs and progress indicators
- ✅ Changelog viewer
- ✅ Manual update check option

### Play Store Flavor
- ❌ No "Check for Updates" button
- ❌ No update dialogs
- ❌ Cleaner profile screen
- ✅ Relies on Play Store updates

---

## 🔍 Identifying the Build Flavor

### From Code
```kotlin
val isGithubVersion = BuildConfig.IS_GITHUB_VERSION
val updatesEnabled = BuildConfig.ENABLE_IN_APP_UPDATE
```

### From APK Filename
- GitHub: `app-github-release.apk`
- Play Store: `app-playstore-release.apk`

### From Version String
- GitHub: Displays as "7.0.0-github"
- Play Store: Displays as "7.0.0"

### From App Behavior
- GitHub: Has update check button in profile
- Play Store: No update check button

---

## 📝 Release Checklist

### GitHub Release Checklist
- [ ] Run `.\build-github-release.ps1`
- [ ] Test APK on device
- [ ] Verify update check works
- [ ] Create GitHub release v7.0.0
- [ ] Upload APK to release
- [ ] Write changelog
- [ ] Mark as latest release
- [ ] Test in-app update on older version

### Play Store Release Checklist
- [ ] Run `.\build-playstore-release.ps1`
- [ ] Test APK on device
- [ ] Verify no update button shows
- [ ] Sign into Play Console
- [ ] Create new release
- [ ] Upload APK
- [ ] Fill release notes (use template)
- [ ] Set rollout percentage (optional)
- [ ] Submit for review
- [ ] Monitor for crashes/ANRs

---

## 🚦 Version Upgrade Path

When releasing new versions, update these files:

### 1. `app/build.gradle.kts`
```kotlin
versionCode = 21  // Increment by 1
versionName = "7.1.0"  // Follow semantic versioning
```

### 2. Build Scripts
Update version numbers in:
- `build-github-release.ps1`
- `build-playstore-release.ps1`
- `build-both-versions.ps1`

### 3. Release Notes
Create/update:
- `CHANGELOG_v7.1.0.md`
- GitHub release description
- Play Store "What's New" section

---

## 💡 Tips & Best Practices

### For GitHub Releases
1. **Always test** the update flow before releasing
2. **Include detailed changelog** in release notes
3. **Use semantic versioning** (MAJOR.MINOR.PATCH)
4. **Keep APK size** under 150MB for faster downloads
5. **Mark as pre-release** if it's a beta version

### For Play Store
1. **Follow Play Store guidelines** strictly
2. **Test on multiple devices** before submission
3. **Use staged rollouts** for major updates (10% → 50% → 100%)
4. **Monitor crash reports** in Play Console
5. **Respond to reviews** to maintain rating

### General
1. **Keep both versions in sync** feature-wise
2. **Test both flavors** on every major release
3. **Maintain separate** release notes for each distribution
4. **Use version codes** properly (always increment)
5. **Keep keystore.properties** secure (never commit)

---

## 🐛 Troubleshooting

### Build Fails
```powershell
# Clean and retry
.\gradlew.bat clean
.\gradlew.bat assembleGithubRelease
```

### Wrong Flavor Built
```powershell
# Check your command
.\gradlew.bat tasks --all | Select-String "assemble"
```

### Signing Issues
- Verify `keystore.properties` exists
- Check keystore path is correct
- Ensure passwords are correct

### Update Not Working (GitHub)
- Check internet connection
- Verify GitHub token in `keystore.properties`
- Ensure release is marked as "latest"
- Check 24-hour interval hasn't been triggered

---

## 📞 Support

### Issues
Report issues on GitHub: https://github.com/atrajit-sarkar/HabitTracker/issues

### Questions
- Check existing documentation
- Review Play Store policies
- Test on clean device install

---

## 🎉 Summary

You now have a **professional dual-flavor build system** that:
- ✅ Separates GitHub and Play Store distributions
- ✅ Handles updates appropriately for each platform
- ✅ Maintains clean code with build config flags
- ✅ Provides convenient build scripts
- ✅ Supports seamless version management

**Use GitHub flavor** for direct distribution and beta testing.
**Use Play Store flavor** for official Google Play releases.

Both versions are production-ready and fully optimized! 🚀
